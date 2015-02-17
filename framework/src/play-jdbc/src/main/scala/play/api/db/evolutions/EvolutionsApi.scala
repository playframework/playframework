/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db.evolutions

import java.io.{ InputStream, File, FileInputStream }
import java.sql.{ Connection, Date, PreparedStatement, ResultSet, SQLException }
import javax.inject.{ Inject, Singleton }

import scala.io.Codec
import scala.util.control.NonFatal

import play.api.db.{ Database, DBApi }
import play.api.libs.Collections
import play.api.{ Environment, Logger, PlayException }
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
   * Get all scripts necessary to reset the database state to its initial state.
   *
   * @param db the database name
   * @return evolution scripts
   */
  def resetScripts(db: String): Seq[Script]

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

  private def databaseEvolutions(name: String) = new DatabaseEvolutions(dbApi.database(name))

  def scripts(db: String, evolutions: Seq[Evolution]) = databaseEvolutions(db).scripts(evolutions)

  def scripts(db: String, reader: EvolutionsReader) = databaseEvolutions(db).scripts(reader)

  def resetScripts(db: String) = databaseEvolutions(db).resetScripts()

  def evolve(db: String, scripts: Seq[Script], autocommit: Boolean) = databaseEvolutions(db).evolve(scripts, autocommit)

  def resolve(db: String, revision: Int) = databaseEvolutions(db).resolve(revision)
}

/**
 * Evolutions for a particular database.
 */
class DatabaseEvolutions(database: Database) {

  import DefaultEvolutionsApi._

  def scripts(evolutions: Seq[Evolution]): Seq[Script] = {
    if (evolutions.nonEmpty) {
      val application = evolutions.reverse
      val database = databaseEvolutions()

      val (nonConflictingDowns, dRest) = database.span(e => !application.headOption.exists(e.revision <= _.revision))
      val (nonConflictingUps, uRest) = application.span(e => !database.headOption.exists(_.revision >= e.revision))

      val (conflictingDowns, conflictingUps) = Evolutions.conflictings(dRest, uRest)

      val ups = (nonConflictingUps ++ conflictingUps).reverse.map(e => UpScript(e))
      val downs = (nonConflictingDowns ++ conflictingDowns).map(e => DownScript(e))

      downs ++ ups
    } else Nil
  }

  def scripts(reader: EvolutionsReader): Seq[Script] = {
    scripts(reader.evolutions(database.name))
  }

  /**
   * Read evolutions from the database.
   */
  private def databaseEvolutions(): Seq[Evolution] = {
    implicit val connection = database.getConnection(autocommit = true)

    checkEvolutionsState()

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

  def evolve(scripts: Seq[Script], autocommit: Boolean): Unit = {
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

    implicit val connection = database.getConnection(autocommit = autocommit)
    checkEvolutionsState()

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

          throw InconsistentDatabase(database.name, humanScript, message, lastScript.evolution.revision)
        } else {
          updateLastProblem(message, applying)
        }
      }
    } finally {
      connection.close()
    }

    checkEvolutionsState()
  }

  /**
   * Checks the evolutions state in the database.
   *
   * @throws an error if the database is in an inconsistent state
   */
  private def checkEvolutionsState(): Unit = {
    def createPlayEvolutionsTable()(implicit conn: Connection): Unit = {
      try {
        val createScript = database.url match {
          case SqlServerJdbcUrl() => CreatePlayEvolutionsSqlServerSql
          case OracleJdbcUrl() => CreatePlayEvolutionsOracleSql
          case _ => CreatePlayEvolutionsSql
        }

        execute(createScript)
      } catch {
        case NonFatal(ex) => logger.warn("could not create play_evolutions table", ex)
      }
    }

    implicit val connection = database.getConnection(autocommit = true)

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

        throw InconsistentDatabase(database.name, humanScript, error, revision)
      }

    } catch {
      case e: InconsistentDatabase => throw e
      case NonFatal(_) => createPlayEvolutionsTable()
    } finally {
      connection.close()
    }
  }

  def resetScripts(): Seq[Script] = {
    val appliedEvolutions = databaseEvolutions()
    appliedEvolutions.map(DownScript)
  }

  def resolve(revision: Int): Unit = {
    implicit val connection = database.getConnection(autocommit = true)
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

  val logger = Logger(classOf[DefaultEvolutionsApi])

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
 * Reader for evolutions
 */
trait EvolutionsReader {
  /**
   * Read the evolutions for the given db
   */
  def evolutions(db: String): Seq[Evolution]
}

/**
 * Evolutions reader that reads evolutions from resources, for example, the file system or the classpath
 */
abstract class ResourceEvolutionsReader extends EvolutionsReader {

  /**
   * Load the evolutions resource for the given database and revision.
   *
   * @return An InputStream to consume the resource, if such a resource exists.
   */
  def loadResource(db: String, revision: Int): Option[InputStream]

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
      loadResource(db, revision).map { stream =>
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
 * Read evolution files from the application environment.
 */
@Singleton
class EnvironmentEvolutionsReader @Inject() (environment: Environment) extends ResourceEvolutionsReader {

  def loadResource(db: String, revision: Int) = {
    environment.getExistingFile(Evolutions.fileName(db, revision)).map(new FileInputStream(_)).orElse {
      environment.resourceAsStream(Evolutions.resourceName(db, revision))
    }
  }
}

/**
 * Evolutions reader that reads evolution files from a class loader.
 *
 * @param classLoader The classloader to read from, defaults to the classloader for this class.
 * @param prefix A prefix that gets added to the resource file names, for example, this could be used to namespace
 *               evolutions in different environments to work with different databases.
 */
class ClassLoaderEvolutionsReader(classLoader: ClassLoader = classOf[ClassLoaderEvolutionsReader].getClassLoader,
    prefix: String = "") extends ResourceEvolutionsReader {
  def loadResource(db: String, revision: Int) = {
    Option(classLoader.getResourceAsStream(prefix + Evolutions.resourceName(db, revision)))
  }
}

/**
 * Evolutions reader that reads evolution files from a class loader.
 */
object ClassLoaderEvolutionsReader {
  /**
   * Create a class loader evolutions reader for the given prefix.
   */
  def forPrefix(prefix: String) = new ClassLoaderEvolutionsReader(prefix = prefix)
}

/**
 * Evolutions reader that reads evolution files from its own classloader.  Only suitable for simple (flat) classloading
 * environments.
 */
object ThisClassLoaderEvolutionsReader extends ClassLoaderEvolutionsReader(classOf[ClassLoaderEvolutionsReader].getClassLoader)

/**
 * Simple map based implementation of the evolutions reader.
 */
class SimpleEvolutionsReader(evolutionsMap: Map[String, Seq[Evolution]]) extends EvolutionsReader {
  def evolutions(db: String) = evolutionsMap.get(db).getOrElse(Nil)
}

/**
 * Simple map based implementation of the evolutions reader.
 */
object SimpleEvolutionsReader {
  /**
   * Create a simple evolutions reader from the given data.
   *
   * @param data A map of database name to a sequence of evolutions.
   */
  def from(data: (String, Seq[Evolution])*) = new SimpleEvolutionsReader(data.toMap)

  /**
   * Create a simple evolutions reader from the given evolutions for the default database.
   *
   * @param evolutions The evolutions.
   */
  def forDefault(evolutions: Evolution*) = new SimpleEvolutionsReader(Map("default" -> evolutions))
}

/**
 * Exception thrown when the database is in an inconsistent state.
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
