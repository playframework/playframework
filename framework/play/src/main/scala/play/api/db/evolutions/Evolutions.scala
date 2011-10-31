package play.api.db.evolutions

import java.io._
import java.sql.{ Date, Connection, SQLException }

import scalax.file._

import play.core._

import play.api._
import play.api.db._
import play.api.libs._
import play.api.libs.Codec._

/**
 * An SQL evolution.
 *
 * @param revision Revision number
 * @param sql_up The SQL statements for UP application.
 * @param sql_down The SQL statements for DOWN application.
 */
case class Evolution(revision: Int, sql_up: String = "", sql_down: String = "") {

  /**
   * Revision hash, automatically computed from the SQL content.
   */
  val hash = sha1(sql_down + sql_up)
}

/**
 * A Script to run on the database.
 */
trait Script {

  /**
   * Original evolution.
   */
  val evolution: Evolution

  /**
   * SQL to be run.
   */
  val sql: String
}

/**
 * An UP Script to run on the database.
 *
 * @param evolution Original evolution.
 * @param sql SQL to be run.
 */
case class UpScript(evolution: Evolution, sql: String) extends Script

/**
 * An DOWN Script to run on the database.
 *
 * @param evolution Original evolution.
 * @param sql SQL to be run.
 */
case class DownScript(evolution: Evolution, sql: String) extends Script

/**
 * This object defines Evolutions utilities functions.
 */
object Evolutions {

  /**
   * Update a local (file based) evolution script.
   */
  def updateEvolutionScript(db: String = "default", revision: Int = 1, comment: String = "Generated", ups: String, downs: String)(implicit application: Application) {
    import play.api.libs._

    val evolutions = application.getFile("db/evolutions/" + db + "/" + revision + ".sql");
    Files.createDirectory(application.getFile("db/evolutions/" + db));
    Files.writeFileIfChanged(evolutions,
      """|# --- %s
               |
               |# --- !Ups
               |%s
               |
               |# --- !Downs
               |%s
               |
               |""".stripMargin.format(comment, ups, downs));
  }

  // --

  private def evolutionsDirectory(applicationPath: File, db: String): Option[File] = {
    Option(new File(applicationPath, "db/evolutions/" + db)).filter(_.exists)
  }

  private def executeQuery(sql: String)(implicit c: Connection) = {
    c.createStatement.executeQuery(sql)
  }

  private def execute(sql: String)(implicit c: Connection) = {
    c.createStatement.execute(sql)
  }

  private def prepare(sql: String)(implicit c: Connection) = {
    c.prepareStatement(sql)
  }

  // --

  /**
   * Resolve evolutions conflicts.
   *
   * @param api The DBApi to use.
   * @param db The database name.
   * @param revision Revision to mark as resolved.
   */
  def resolve(api: DBApi, db: String, revision: Int) {
    implicit val connection = api.getConnection(db, autocommit = true)

    try {
      execute("update play_evolutions set state = 'applied' where state = 'applying_up' and id = " + revision);
      execute("delete from play_evolutions where state = 'applying_down' and id = " + revision);
    } finally {
      connection.close()
    }
  }

  /**
   * Check the evolutions state.
   *
   * @param api The DBApi to use.
   * @param db The database name.
   * @throws An error if the database is in an inconsistent state.
   */
  def checkEvolutionsState(api: DBApi, db: String) {
    implicit val connection = api.getConnection(db, autocommit = true)

    try {

      val problem = executeQuery("select id, hash, apply_script, revert_script, state, last_problem from play_evolutions where state like 'applying_%'")

      if (problem.next) {
        val revision = problem.getInt("id")
        val state = problem.getString("state")
        val hash = problem.getString("hash").substring(0, 7)
        val script = state match {
          case "applying_up" => problem.getString("apply_script")
          case _ => problem.getString("revert_script")
        }

        // script = "# --- Rev:" + revision + "," + (state.equals("applying_up") ? "Ups" : "Downs") + " - " + hash + "\n\n" + script;
        // String error = rs.getString("last_problem");

        throw InconsistentDatabase(db)
      }

    } catch {
      case e: InconsistentDatabase => throw e
      case _ => execute(
        """
                    create table play_evolutions (
                        id int not null primary key, hash varchar(255) not null, 
                        applied_at timestamp not null, 
                        apply_script text, 
                        revert_script text, 
                        state varchar(255), 
                        last_problem text
                    )
                """)
    } finally {
      connection.close()
    }

  }

  /**
   * Apply a script to the databse.
   *
   * @param api The DBApi to use.
   * @param db The database name.
   * @param script The script to run.
   */
  def applyScript(api: DBApi, db: String, script: Seq[Script]) {
    implicit val connection = api.getConnection(db, autocommit = true)

    checkEvolutionsState(api, db)

    var applying = -1

    try {

      script.foreach { s =>
        applying = s.evolution.revision

        // Insert into log
        s match {

          case UpScript(e, _) => {
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

          case DownScript(e, _) => {
            execute("update play_evolutions set state = 'applying_down' where id = " + e.revision)
          }

        }

        // Execute script
        s.sql.split(";").map(_.trim).foreach {
          case "" =>
          case statement => execute(statement)
        }

        // Insert into logs
        s match {

          case UpScript(e, _) => {
            execute("update play_evolutions set state = 'applied' where id = " + e.revision)
          }

          case DownScript(e, _) => {
            execute("delete from play_evolutions where id = " + e.revision)
          }

        }

      }

    } catch {
      case e => {
        val message = e match {
          case ex: SQLException => ex.getMessage + " [ERROR:" + ex.getErrorCode + ", SQLSTATE:" + ex.getSQLState + "]"
          case ex => ex.getMessage
        }
        val ps = prepare("update play_evolutions set last_problem = ? where id = ?")
        ps.setString(1, message)
        ps.setInt(2, applying)
        ps.execute()
      }
    } finally {
      connection.close()
    }

  }

  /**
   * Translate an evolution script to something useful for humans.
   *
   * @param scripts The script.
   * @return A formatted script as String.
   */
  def toHumanReadableScript(script: Seq[Script]) = {
    val txt = script.map {
      case UpScript(ev, sql) => "# --- Rev:" + ev.revision + ",Ups - " + ev.hash.take(7) + "\n" + sql + "\n"
      case DownScript(ev, sql) => "# --- Rev:" + ev.revision + ",Downs - " + ev.hash.take(7) + "\n" + sql + "\n"
    }.mkString("\n")

    script.find {
      case DownScript(_, _) => true
      case UpScript(_, _) => false
    }.map(_ => "# !!! WARNING! This script contains DOWNS evolutions that are likely destructives\n\n").getOrElse("") + txt
  }

  /**
   * Compute the evolution script.
   *
   * @param api The DBApi to use.
   * @param applicationPath The applicationPath path.
   * @param The db name.
   */
  def evolutionScript(api: DBApi, applicationPath: File, db: String) = {
    val application = applicationEvolutions(applicationPath, db)
    val database = databaseEvolutions(api, db)

    val (nonConflictingDowns, dRest) = database.span(e => !application.headOption.exists(e.revision <= _.revision))
    val (nonConflictingUps, uRest) = application.span(e => !database.headOption.exists(_.revision >= e.revision))

    val (conflictingDowns, conflictingUps) = dRest.zip(uRest).takeWhile {
      case (down, up) => down.hash != up.hash
    }.unzip

    val ups = (nonConflictingUps ++ conflictingUps).reverse.map(e => UpScript(e, e.sql_up))
    val downs = (nonConflictingDowns ++ conflictingDowns).map(e => DownScript(e, e.sql_down))

    downs ++ ups
  }

  /**
   * Read evolutions from the database.
   *
   * @param api The DBApi to use.
   * @param The db name.
   */
  def databaseEvolutions(api: DBApi, db: String) = {
    implicit val connection = api.getConnection(db, autocommit = true)

    checkEvolutionsState(api, db)

    try {

      Collections.unfoldLeft(executeQuery(
        """
                    select id, hash, apply_script, revert_script from play_evolutions order by id
                """)) { rs =>
        rs.next match {
          case false => None
          case true => {
            Some(rs, Evolution(
              rs.getInt(1),
              rs.getString(3),
              rs.getString(4)))
          }
        }
      }

    } finally {
      connection.close()
    }
  }

  /**
   * Read the evolutions from the application.
   *
   * @param applicationPath The applicationPath path.
   * @param The db name.
   */
  def applicationEvolutions(applicationPath: File, db: String) = {
    evolutionsDirectory(applicationPath, db).map { dir =>

      val evolutionScript = """^([0-9]+)[.]sql$""".r
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

      Path(dir).children().map(f => f.name -> f).collect {
        case (evolutionScript(revision), script) => Integer.parseInt(revision) -> script.slurpString
      }.toList.sortBy(_._1).map {
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
      }.reverse

    }.getOrElse(Nil)
  }

}

/**
 * Play Evolutions plugin
 */
class EvolutionsPlugin(app: Application) extends Plugin {

  import Evolutions._

  override def enabled = app.configuration.getSub("db").isDefined
  /**
   * Check the evolutions state.
   */
  override def onStart {
    val api = app.plugin[DBPlugin].map(_.api).getOrElse(throw new Exception("there should be a database plugin registered at this point but looks like it's not available, so evolution won't work. Please make sure you register a db plugin properly"))

    api.datasources.foreach {
      case (db, (ds, _)) => {
        val script = evolutionScript(api, app.path, db)
        if (!script.isEmpty) {
          //if(ds.getJdbcUrl.startsWith("jdbc:h2:mem:") && script.headOption.filter {case UpScript(ev,_) => ev.revision == 1}.isDefined) {
          //    Evolutions.applyScript(api, db, script)
          //} else  {
          throw InvalidDatabaseRevision(db, toHumanReadableScript(script))
          //}
        }
      }
    }
  }

}

/**
 * This object can be used to run offline evolutions (outside of a running application)
 */
object OfflineEvolutions {

  /**
   * Compute and apply evolutions script.
   *
   * @param applicationPath The application path.
   * @param classloader Classloader used to load the driver.
   * @param dbName Database name.
   */
  def applyScript(applicationPath: File, classloader: ClassLoader, dbName: String) = {

    import play.api._

    val api = DBApi(
      Map(dbName -> DBApi.createDataSource(
        Configuration.fromFile(new File(applicationPath, "conf/application.conf")).sub("db." + dbName), classloader)))
    val script = Evolutions.evolutionScript(api, applicationPath, dbName)

    Logger("play").warn("Applying evolution script for database '" + dbName + "':\n\n" + Evolutions.toHumanReadableScript(script))

    Evolutions.applyScript(api, dbName, script)

  }

}

/**
 * Exception thrown when the database is not up to date.
 *
 * @param db Database name
 * @param script Script to be run to resolve the conflict.
 */
case class InvalidDatabaseRevision(db: String, script: String) extends PlayException(
  "Database '" + db + "' needs evolution!",
  "An SQL script need to be run on your database.",
  None) with PlayException.ExceptionAttachment with PlayException.RichDescription {

  def subTitle = "This SQL script must be run:"
  def content = script

  private val javascript = """
        document.location = '/@evolutions/apply/%s?redirect=' + encodeURIComponent(location)
    """.format(db).trim

  def htmlDescription = {

    <span>An SQL script will be run on your database -</span>
    <input type="button" value="Apply this script now!" onclick={ javascript }/>

  }.map(_.toString).mkString

}

/**
 * Exception thrown when the databse is in inconsistent state.
 *
 * @param db Database name
 */
case class InconsistentDatabase(db: String) extends PlayException(
  "Database '" + db + "' is in inconsistent state!",
  "An evolution has not been applied properly. Please check the problem and resolve it manually before making it as resolved.",
  None)
