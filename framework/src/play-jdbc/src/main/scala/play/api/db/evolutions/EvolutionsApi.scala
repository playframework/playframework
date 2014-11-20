/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db.evolutions

import java.io.{ File, FileInputStream }
import java.sql.{ Connection, Date, PreparedStatement, ResultSet, SQLException }
import javax.inject.{ Inject, Singleton }

import scala.io.Codec
import scala.util.control.NonFatal

import play.api.db.DBApi
import play.api.libs.Collections
import play.api.{ Environment, Logger, Mode, PlayException }
import play.utils.PlayIO

/**
 * Evolutions API.
 */
trait EvolutionsApi {
  /**
   * Create evolution scripts.
   *
   * @param db the database name
   * @param evolutions the evolutions for the application
   * @return evolution scripts
   */
  def scripts(db: String, evolutions: Seq[Evolution]): Seq[Script]

  /**
   * Create evolution scripts.
   *
   * @param db the database name
   * @param reader evolution file reader
   * @return evolution scripts
   */
  def scripts(db: String, reader: EvolutionsReader): Seq[Script]

  /**
   * Create evolution scripts.
   *
   * @param db the database name
   * @param path application root path
   * @return evolution scripts
   */
  def scripts(db: String, path: File): Seq[Script]

  /**
   * Apply evolution scripts to the database.
   *
   * @param db the database name
   * @param scripts the evolution scripts to run
   * @param autocommit determines whether the connection uses autocommit
   */
  def evolve(db: String, scripts: Seq[Script], autocommit: Boolean): Unit

  /**
   * Resolve evolution conflicts.
   *
   * @param db the database name
   * @param revision the revision to mark as resolved
   */
  def resolve(db: String, revision: Int): Unit
}

/**
 * Default implementation of the evolutions API.
 */
@Singleton
class DefaultEvolutionsApi @Inject() (dbApi: DBApi) extends EvolutionsApi {

  import DefaultEvolutionsApi._

  private val logger = Logger(classOf[DefaultEvolutionsApi])

  /**
   * Create evolution scripts.
   *
   * @param db the database name
   * @param evolutions the evolutions for the application
   * @return evolution scripts
   */
  def scripts(db: String, evolutions: Seq[Evolution]): Seq[Script] = {
    if (evolutions.nonEmpty) {
      val application = evolutions.reverse
      val database = databaseEvolutions(db)

      val (nonConflictingDowns, dRest) = database.span(e => !application.headOption.exists(e.revision <= _.revision))
      val (nonConflictingUps, uRest) = application.span(e => !database.headOption.exists(_.revision >= e.revision))

      val (conflictingDowns, conflictingUps) = Evolutions.conflictings(dRest, uRest)

      val ups = (nonConflictingUps ++ conflictingUps).reverse.map(e => UpScript(e))
      val downs = (nonConflictingDowns ++ conflictingDowns).map(e => DownScript(e))

      downs ++ ups
    } else Nil
  }

  /**
   * Create evolution scripts.
   *
   * @param db the database name
   * @param reader evolution file reader
   * @return evolution scripts
   */
  def scripts(db: String, reader: EvolutionsReader): Seq[Script] = {
    scripts(db, reader.evolutions(db))
  }

  /**
   * Create evolution scripts.
   *
   * @param db the database name
   * @param path application root path
   * @return evolution scripts
   */
  def scripts(db: String, path: File): Seq[Script] = {
    scripts(db, new EvolutionsReader(Environment(path, this.getClass.getClassLoader, Mode.Dev /* not used */ )))
  }

  /**
   * Read evolutions from the database.
   *
   * @param db the database name
   */
  def databaseEvolutions(db: String): Seq[Evolution] = {
    implicit val connection = dbApi.database(db).getConnection(autocommit = true)

    checkEvolutionsState(db)

    try {

      Collections.unfoldLeft(executeQuery(
        """
            select id, hash, apply_script, revert_script from play_evolutions order by id
        """)) { rs =>
        rs.next match {
          case false => None
          case true => {
            Some((rs, Evolution(
              rs.getInt(1),
              Option(rs.getString(3)) getOrElse "",
              Option(rs.getString(4)) getOrElse "")))
          }
        }
      }

    } finally {
      connection.close()
    }
  }

  /**
   * Apply evolution scripts to the database.
   *
   * @param db the database name
   * @param scripts the evolution scripts to run
   * @param autocommit determines whether the connection uses autocommit
   */
  def evolve(db: String, scripts: Seq[Script], autocommit: Boolean): Unit = {
    def logBefore(script: Script)(implicit conn: Connection): Unit = {
      script match {
        case UpScript(e) => {
          val ps = prepare("insert into play_evolutions values(?, ?, ?, ?, ?, ?, ?)")
          ps.setInt(1, e.revision)
          ps.setString(2, e.hash)
          ps.setDate(3, new Date(System.currentTimeMillis()))
          ps.setString(4, e.sql_up)
          ps.setString(5, e.sql_down)
          ps.setString(6, "applying_up")
          ps.setString(7, "")
          ps.execute()
        }
        case DownScript(e) => {
          execute("update play_evolutions set state = 'applying_down' where id = " + e.revision)
        }
      }
    }

    def logAfter(script: Script)(implicit conn: Connection): Boolean = {
      script match {
        case UpScript(e) => {
          execute("update play_evolutions set state = 'applied' where id = " + e.revision)
        }
        case DownScript(e) => {
          execute("delete from play_evolutions where id = " + e.revision)
        }
      }
    }

    def updateLastProblem(message: String, revision: Int)(implicit conn: Connection): Boolean = {
      val ps = prepare("update play_evolutions set last_problem = ? where id = ?")
      ps.setString(1, message)
      ps.setInt(2, revision)
      ps.execute()
    }

    implicit val connection = dbApi.database(db).getConnection(autocommit = autocommit)
    checkEvolutionsState(db)

    var applying = -1
    var lastScript: Script = null

    try {

      scripts.foreach { script =>
        lastScript = script
        applying = script.evolution.revision
        logBefore(script)
        // Execute script
        script.statements.foreach(execute)
        logAfter(script)
      }

      if (!autocommit) {
        connection.commit();
      }

    } catch {
      case NonFatal(e) => {
        val message = e match {
          case ex: SQLException => ex.getMessage + " [ERROR:" + ex.getErrorCode + ", SQLSTATE:" + ex.getSQLState + "]"
          case ex => ex.getMessage
        }
        if (!autocommit) {
          logger.error(message)

          connection.rollback()

          val humanScript = "# --- Rev:" + lastScript.evolution.revision + "," + (if (lastScript.isInstanceOf[UpScript]) "Ups" else "Downs") + " - " + lastScript.evolution.hash + "\n\n" + (if (lastScript.isInstanceOf[UpScript]) lastScript.evolution.sql_up else lastScript.evolution.sql_down);

          throw InconsistentDatabase(db, humanScript, message, lastScript.evolution.revision)
        } else {
          updateLastProblem(message, applying)
        }
      }
    } finally {
      connection.close()
    }

    checkEvolutionsState(db)
  }

  /**
   * Checks the evolutions state in the database.
   *
   * @param db the database name
   * @throws an error if the database is in an inconsistent state
   */
  private def checkEvolutionsState(db: String): Unit = {
    def createPlayEvolutionsTable()(implicit conn: Connection): Unit = {
      try {
        val createScript = dbApi.database(db).url match {
          case SqlServerJdbcUrl() => CreatePlayEvolutionsSqlServerSql
          case OracleJdbcUrl() => CreatePlayEvolutionsOracleSql
          case _ => CreatePlayEvolutionsSql
        }

        execute(createScript)
      } catch {
        case NonFatal(ex) => logger.warn("could not create play_evolutions table", ex)
      }
    }

    implicit val connection = dbApi.database(db).getConnection(autocommit = true)

    try {
      val problem = executeQuery("select id, hash, apply_script, revert_script, state, last_problem from play_evolutions where state like 'applying_%'")

      if (problem.next) {
        val revision = problem.getInt("id")
        val state = problem.getString("state")
        val hash = problem.getString("hash").take(7)
        val script = state match {
          case "applying_up" => problem.getString("apply_script")
          case _ => problem.getString("revert_script")
        }
        val error = problem.getString("last_problem")

        logger.error(error)

        val humanScript = "# --- Rev:" + revision + "," + (if (state == "applying_up") "Ups" else "Downs") + " - " + hash + "\n\n" + script;

        throw InconsistentDatabase(db, humanScript, error, revision)
      }

    } catch {
      case e: InconsistentDatabase => throw e
      case NonFatal(_) => createPlayEvolutionsTable()
    } finally {
      connection.close()
    }
  }

  /**
   * Resolve evolution conflicts.
   *
   * @param db the database name
   * @param revision the revision to mark as resolved
   */
  def resolve(db: String, revision: Int): Unit = {
    implicit val connection = dbApi.database(db).getConnection(autocommit = true)
    try {
      execute("update play_evolutions set state = 'applied' where state = 'applying_up' and id = " + revision);
      execute("delete from play_evolutions where state = 'applying_down' and id = " + revision);
    } finally {
      connection.close()
    }
  }

  // SQL helpers

  private def executeQuery(sql: String)(implicit c: Connection): ResultSet = {
    c.createStatement.executeQuery(sql)
  }

  private def execute(sql: String)(implicit c: Connection): Boolean = {
    c.createStatement.execute(sql)
  }

  private def prepare(sql: String)(implicit c: Connection): PreparedStatement = {
    c.prepareStatement(sql)
  }
}

private object DefaultEvolutionsApi {
  val SqlServerJdbcUrl = "^jdbc:sqlserver:.*".r
  val OracleJdbcUrl = "^jdbc:oracle:.*".r

  val CreatePlayEvolutionsSql =
    """
      create table play_evolutions (
          id int not null primary key,
          hash varchar(255) not null,
          applied_at timestamp not null,
          apply_script text,
          revert_script text,
          state varchar(255),
          last_problem text
      )
    """

  val CreatePlayEvolutionsSqlServerSql =
    """
      create table play_evolutions (
          id int not null primary key,
          hash varchar(255) not null,
          applied_at datetime not null,
          apply_script text,
          revert_script text,
          state varchar(255),
          last_problem text
      )
    """

  val CreatePlayEvolutionsOracleSql =
    """
      CREATE TABLE play_evolutions (
          id Number(10,0) Not Null Enable,
          hash VARCHAR2(255 Byte),
          applied_at Timestamp Not Null,
          apply_script clob,
          revert_script clob,
          state Varchar2(255),
          last_problem clob,
          CONSTRAINT play_evolutions_pk PRIMARY KEY (id)
      )
    """
}

/**
 * Read evolution files from the application environment.
 */
@Singleton
class EvolutionsReader @Inject() (environment: Environment) {
  /**
   * Read the application evolutions.
   *
   * @param db the database name
   */
  def evolutions(db: String): Seq[Evolution] = {

    val upsMarker = """^#.*!Ups.*$""".r
    val downsMarker = """^#.*!Downs.*$""".r

    val UPS = "UPS"
    val DOWNS = "DOWNS"
    val UNKNOWN = "UNKNOWN"

    val mapUpsAndDowns: PartialFunction[String, String] = {
      case upsMarker() => UPS
      case downsMarker() => DOWNS
      case _ => UNKNOWN
    }

    val isMarker: PartialFunction[String, Boolean] = {
      case upsMarker() => true
      case downsMarker() => true
      case _ => false
    }

    Collections.unfoldLeft(1) { revision =>
      environment.getExistingFile(Evolutions.fileName(db, revision)).map(new FileInputStream(_)).orElse {
        environment.resourceAsStream(Evolutions.resourceName(db, revision))
      }.map { stream =>
        (revision + 1, (revision, PlayIO.readStreamAsString(stream)(Codec.UTF8)))
      }
    }.sortBy(_._1).map {
      case (revision, script) => {

        val parsed = Collections.unfoldLeft(("", script.split('\n').toList.map(_.trim))) {
          case (_, Nil) => None
          case (context, lines) => {
            val (some, next) = lines.span(l => !isMarker(l))
            Some((next.headOption.map(c => (mapUpsAndDowns(c), next.tail)).getOrElse("" -> Nil),
              context -> some.mkString("\n")))
          }
        }.reverse.drop(1).groupBy(i => i._1).mapValues { _.map(_._2).mkString("\n").trim }

        Evolution(
          revision,
          parsed.get(UPS).getOrElse(""),
          parsed.get(DOWNS).getOrElse(""))
      }
    }

  }
}

/**
 * Exception thrown when the database is in inconsistent state.
 *
 * @param db the database name
 * @param script the evolution script
 * @param error an inconsistent state error
 * @param rev the revision
 */
case class InconsistentDatabase(db: String, script: String, error: String, rev: Int) extends PlayException.RichDescription(
  "Database '" + db + "' is in an inconsistent state!",
  "An evolution has not been applied properly. Please check the problem and resolve it manually before marking it as resolved.") {

  def subTitle = "We got the following error: " + error + ", while trying to run this SQL script:"
  def content = script

  private val javascript = """
        document.location = '/@evolutions/resolve/%s/%s?redirect=' + encodeURIComponent(location)
    """.format(db, rev).trim

  def htmlDescription: String = {

    <span>An evolution has not been applied properly. Please check the problem and resolve it manually before marking it as resolved -</span>
    <input name="evolution-button" type="button" value="Mark it resolved" onclick={ javascript }/>

  }.mkString

}
