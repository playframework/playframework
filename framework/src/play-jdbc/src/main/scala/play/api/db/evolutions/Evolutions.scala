package play.api.db.evolutions

import java.io._

import scalax.io.JavaConverters._

import play.core._

import play.api._
import play.api.db._
import play.api.libs._
import play.api.libs.Codecs._
import javax.sql.DataSource
import java.sql.{ Statement, Date, Connection, SQLException }
import scala.util.control.Exception._
import scala.util.control.NonFatal

/**
 * An SQL evolution - database changes associated with a software version.
 *
 * An evolution includes ‘up’ changes, to upgrade to to the version, as well as ‘down’ changes, to downgrade the database
 * to the previous version.
 *
 * @param revision revision number
 * @param sql_up the SQL statements for UP application
 * @param sql_down the SQL statements for DOWN application
 */
private[evolutions] case class Evolution(revision: Int, sql_up: String = "", sql_down: String = "") {

  /**
   * Revision hash, automatically computed from the SQL content.
   */
  val hash = sha1(sql_down.trim + sql_up.trim)

}

/**
 * A Script to run on the database.
 */
private[evolutions] trait Script {

  /**
   * Original evolution.
   */
  val evolution: Evolution

  /**
   * The complete SQL to be run.
   */
  val sql: String

  /**
   * The sql string separated into constituent ";"-delimited statements.
   *
   * Any ";;" found in the sql are escaped to ";".
   */
  def statements: Seq[String] = {
    // Regex matches on semicolons that neither precede nor follow other semicolons
    sql.split("(?<!;);(?!;)").map(_.trim.replace(";;", ";")).filter(_ != "")
  }
}

/**
 * An UP Script to run on the database.
 *
 * @param evolution the original evolution
 * @param sql the SQL to be run
 */
private[evolutions] case class UpScript(evolution: Evolution, sql: String) extends Script

/**
 * A DOWN Script to run on the database.
 *
 * @param evolution the original evolution
 * @param sql the SQL to be run
 */
private[evolutions] case class DownScript(evolution: Evolution, sql: String) extends Script

/**
 * Defines Evolutions utilities functions.
 */
object Evolutions {

  /**
   * Apply pending evolutions for the given DB.
   */
  def applyFor(dbName: String, path: java.io.File = new java.io.File(".")) {
    Play.current.plugin[DBPlugin] map { db =>
      val script = Evolutions.evolutionScript(db.api, path, db.getClass.getClassLoader, dbName)
      Evolutions.applyScript(db.api, dbName, script)
    }
  }

  /**
   * Updates a local (file-based) evolution script.
   */
  def updateEvolutionScript(db: String = "default", revision: Int = 1, comment: String = "Generated", ups: String, downs: String)(implicit application: Application) {
    import play.api.libs._

    val evolutions = application.getFile(evolutionsFilename(db, revision));
    Files.createDirectory(application.getFile(evolutionsDirectoryName(db)));
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
   * Resolves evolution conflicts.
   *
   * @param api the `DBApi` to use
   * @param db the database name
   * @param revision the revision to mark as resolved
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
   * Checks the evolutions state.
   *
   * @param api the `DBApi` to use
   * @param db the database name
   * @throws an error if the database is in an inconsistent state
   */
  def checkEvolutionsState(api: DBApi, db: String) {
    def createPlayEvolutionsTable()(implicit conn: Connection): Unit = {
      try {
        execute(
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
      } catch {
        case NonFatal(ex) => Logger.warn("play_evolutions table already existed")
      }
    }

    implicit val connection = api.getConnection(db, autocommit = true)

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

        Play.logger.error(error)

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
   * Applies a script to the database.
   *
   * @param api the `DBApi` to use
   * @param db the database name
   * @param script the script to run
   */
  def applyScript(api: DBApi, db: String, script: Seq[Script]) {
    def logBefore(s: Script)(implicit conn: Connection): Unit = {
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
    }

    def logAfter(s: Script)(implicit conn: Connection): Boolean = {
      s match {
        case UpScript(e, _) => {
          execute("update play_evolutions set state = 'applied' where id = " + e.revision)
        }
        case DownScript(e, _) => {
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

    implicit val connection = api.getConnection(db, autocommit = true)
    checkEvolutionsState(api, db)

    var applying = -1

    try {

      script.foreach { s =>
        applying = s.evolution.revision
        logBefore(s)
        // Execute script
        s.statements.foreach(execute)
        logAfter(s)
      }

    } catch {
      case NonFatal(e) => {
        val message = e match {
          case ex: SQLException => ex.getMessage + " [ERROR:" + ex.getErrorCode + ", SQLSTATE:" + ex.getSQLState + "]"
          case ex => ex.getMessage
        }
        updateLastProblem(message, applying)
      }
    } finally {
      connection.close()
    }

    checkEvolutionsState(api, db)

  }

  /**
   * Translates an evolution script to something human-readable.
   *
   * @param script the script
   * @return a formatted script
   */
  def toHumanReadableScript(script: Seq[Script]): String = {
    val txt = script.map {
      case UpScript(ev, sql) => "# --- Rev:" + ev.revision + ",Ups - " + ev.hash.take(7) + "\n" + sql + "\n"
      case DownScript(ev, sql) => "# --- Rev:" + ev.revision + ",Downs - " + ev.hash.take(7) + "\n" + sql + "\n"
    }.mkString("\n")

    val hasDownWarning =
      "# !!! WARNING! This script contains DOWNS evolutions that are likely destructives\n\n"

    if (script.exists(_.isInstanceOf[DownScript])) hasDownWarning + txt else txt
  }

  /**
   * Computes the evolution script.
   *
   * @param api the `DBApi` to use
   * @param path the application path
   * @param applicationClassloader the classloader used to load the driver
   * @param db the database name
   * @return evolution scripts
   */
  def evolutionScript(api: DBApi, path: File, applicationClassloader: ClassLoader, db: String): Seq[Product with Serializable with Script] = {
    val application = applicationEvolutions(path, applicationClassloader, db)

    Option(application).filterNot(_.isEmpty).map {
      case application =>
        val database = databaseEvolutions(api, db)

        val (nonConflictingDowns, dRest) = database.span(e => !application.headOption.exists(e.revision <= _.revision))
        val (nonConflictingUps, uRest) = application.span(e => !database.headOption.exists(_.revision >= e.revision))

        val (conflictingDowns, conflictingUps) = conflictings(dRest, uRest)

        val ups = (nonConflictingUps ++ conflictingUps).reverse.map(e => UpScript(e, e.sql_up))
        val downs = (nonConflictingDowns ++ conflictingDowns).map(e => DownScript(e, e.sql_down))

        downs ++ ups
    }.getOrElse(Nil)
  }

  /**
   *
   * Compares the two evolution sequences.
   *
   * @param downRest the seq of downs
   * @param upRest the seq of ups
   * @return the downs and ups to run to have the db synced to the current stage
   */
  def conflictings(downRest: Seq[Evolution], upRest: Seq[Evolution]) = downRest.zip(upRest).reverse.dropWhile {
    case (down, up) => down.hash == up.hash
  }.reverse.unzip

  /**
   * Reads evolutions from the database.
   *
   * @param api the `DBApi` to use
   * @param db the database name
   */
  def databaseEvolutions(api: DBApi, db: String): Seq[Evolution] = {
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
            Some((rs, Evolution(
              rs.getInt(1),
              rs.getString(3),
              rs.getString(4))))
          }
        }
      }

    } finally {
      connection.close()
    }
  }

  private def evolutionsDirectoryName(db: String): String = s"conf/evolutions/${db}"

  private def evolutionsFilename(db: String, revision: Int): String = s"conf/evolutions/${db}/${revision}.sql"

  private def evolutionsResourceName(db: String, revision: Int): String = s"evolutions/${db}/${revision}.sql"

  /**
   * Reads the evolutions from the application.
   *
   * @param path the application path
   * @param applicationClassloader the classloader used to load the driver
   * @param db the database name
   */
  def applicationEvolutions(path: File, applicationClassloader: ClassLoader, db: String): Seq[Evolution] = {

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
      Option(new File(path, evolutionsFilename(db, revision))).filter(_.exists).map(new FileInputStream(_)).orElse {
        Option(applicationClassloader.getResourceAsStream(evolutionsResourceName(db, revision)))
      }.map { stream =>
        (revision + 1, (revision, stream.asInput.string))
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
    }.reverse

  }

}

/**
 * Play Evolutions plugin.
 */
class EvolutionsPlugin(app: Application) extends Plugin with HandleWebCommandSupport {

  import Evolutions._

  lazy val dbApi = app.plugin[DBPlugin].map(_.api).getOrElse(throw new Exception("there should be a database plugin registered at this point but looks like it's not available, so evolution won't work. Please make sure you register a db plugin properly"))

  /**
   * Is this plugin enabled.
   *
   * {{{
   * evolutionplugin = disabled
   * }}}
   */
  override lazy val enabled = app.configuration.getConfig("db").isDefined && {
    !app.configuration.getString("evolutionplugin").exists(_ == "disabled")
  }

  /**
   * Checks the evolutions state.
   */
  override def onStart() {
    dbApi.datasources.foreach {
      case (ds, db) => {
        withLock(ds) {
          val script = evolutionScript(dbApi, app.path, app.classloader, db)
          val hasDown = script.exists(_.isInstanceOf[DownScript])

          lazy val applyEvolutions = app.configuration.getBoolean("applyEvolutions." + db).getOrElse(false)
          lazy val applyDownEvolutions = app.configuration.getBoolean("applyDownEvolutions." + db).getOrElse(false)

          if (!script.isEmpty) {
            app.mode match {
              case Mode.Test => Evolutions.applyScript(dbApi, db, script)
              case Mode.Dev if applyEvolutions => Evolutions.applyScript(dbApi, db, script)
              case Mode.Prod if !hasDown && applyEvolutions => Evolutions.applyScript(dbApi, db, script)
              case Mode.Prod if hasDown && applyEvolutions && applyDownEvolutions => Evolutions.applyScript(dbApi, db, script)
              case Mode.Prod if hasDown => {
                Play.logger.warn("Your production database [" + db + "] needs evolutions, including downs! \n\n" + toHumanReadableScript(script))
                Play.logger.warn("Run with -DapplyEvolutions." + db + "=true and -DapplyDownEvolutions." + db + "=true if you want to run them automatically, including downs (be careful, especially if your down evolutions drop existing data)")

                throw InvalidDatabaseRevision(db, toHumanReadableScript(script))
              }
              case Mode.Prod => {
                Play.logger.warn("Your production database [" + db + "] needs evolutions! \n\n" + toHumanReadableScript(script))
                Play.logger.warn("Run with -DapplyEvolutions." + db + "=true if you want to run them automatically (be careful)")

                throw InvalidDatabaseRevision(db, toHumanReadableScript(script))
              }
              case _ => throw InvalidDatabaseRevision(db, toHumanReadableScript(script))
            }
          }
        }
      }
    }
  }

  def withLock(ds: DataSource)(block: => Unit) {
    if (app.configuration.getBoolean("evolutions.use.locks").getOrElse(false)) {
      val c = ds.getConnection
      c.setAutoCommit(false)
      val s = c.createStatement()
      createLockTableIfNecessary(c, s)
      lock(c, s)
      try {
        block
      } finally {
        unlock(c, s)
      }
    } else {
      block
    }
  }

  def createLockTableIfNecessary(c: Connection, s: Statement) {
    try {
      val r = s.executeQuery("select lock from play_evolutions_lock")
      r.close()
    } catch {
      case e: SQLException =>
        c.rollback()
        s.execute("""
        create table play_evolutions_lock (
          lock int not null primary key
        )
        """)
        s.executeUpdate("insert into play_evolutions_lock (lock) values (1)")
    }
  }

  def lock(c: Connection, s: Statement, attempts: Int = 5) {
    try {
      s.executeQuery("select lock from play_evolutions_lock where lock = 1 for update nowait")
    } catch {
      case e: SQLException =>
        if (attempts == 0) throw e
        else {
          Play.logger.warn("Exception while attempting to lock evolutions (other node probably has lock), sleeping for 1 sec")
          c.rollback()
          Thread.sleep(1000)
          lock(c, s, attempts - 1)
        }
    }
  }

  def unlock(c: Connection, s: Statement) {
    ignoring(classOf[SQLException])(s.close())
    ignoring(classOf[SQLException])(c.commit())
    ignoring(classOf[SQLException])(c.close())
  }

  def handleWebCommand(request: play.api.mvc.RequestHeader, sbtLink: play.core.SBTLink, path: java.io.File): Option[play.api.mvc.SimpleResult] = {

    val applyEvolutions = """/@evolutions/apply/([a-zA-Z0-9_]+)""".r
    val resolveEvolutions = """/@evolutions/resolve/([a-zA-Z0-9_]+)/([0-9]+)""".r

    lazy val redirectUrl = request.queryString.get("redirect").filterNot(_.isEmpty).map(_(0)).getOrElse("/")

    request.path match {

      case applyEvolutions(db) => {
        Some {
          val script = Evolutions.evolutionScript(dbApi, app.path, app.classloader, db)
          Evolutions.applyScript(dbApi, db, script)
          sbtLink.forceReload()
          play.api.mvc.Results.Redirect(redirectUrl)
        }
      }

      case resolveEvolutions(db, rev) => {
        Some {
          Evolutions.resolve(dbApi, db, rev.toInt)
          sbtLink.forceReload()
          play.api.mvc.Results.Redirect(redirectUrl)
        }
      }

      case _ => None

    }

  }

}

/**
 * Can be used to run off-line evolutions, i.e. outside a running application.
 */
object OfflineEvolutions {

  private def isTest: Boolean = Play.maybeApplication.exists(_.mode == Mode.Test)

  private def getDBApi(appPath: File, classloader: ClassLoader): DBApi = {
    val c = Configuration.load(appPath).getConfig("db").get
    new BoneCPApi(c, classloader)
  }

  /**
   * Computes and applies an evolutions script.
   *
   * @param appPath the application path
   * @param classloader the classloader used to load the driver
   * @param dbName the database name
   */
  def applyScript(appPath: File, classloader: ClassLoader, dbName: String) {
    val dbApi = getDBApi(appPath, classloader)
    val script = Evolutions.evolutionScript(dbApi, appPath, classloader, dbName)
    if (!isTest) {
      Play.logger.warn("Applying evolution script for database '" + dbName + "':\n\n" + Evolutions.toHumanReadableScript(script))
    }
    Evolutions.applyScript(dbApi, dbName, script)
  }

  /**
   * Resolve an inconsistent evolution..
   *
   * @param appPath the application path
   * @param classloader the classloader used to load the driver
   * @param dbName the database name
   * @param revision the revision
   */
  def resolve(appPath: File, classloader: ClassLoader, dbName: String, revision: Int) {
    val dbApi = getDBApi(appPath, classloader)
    if (!isTest) {
      Play.logger.warn("Resolving evolution [" + revision + "] for database '" + dbName + "'")
    }
    Evolutions.resolve(dbApi, dbName, revision)
  }

}

/**
 * Exception thrown when the database is not up to date.
 *
 * @param db the database name
 * @param script the script to be run to resolve the conflict.
 */
case class InvalidDatabaseRevision(db: String, script: String) extends PlayException.RichDescription(
  "Database '" + db + "' needs evolution!",
  "An SQL script need to be run on your database.") {

  def subTitle = "This SQL script must be run:"
  def content = script

  private val javascript = """
        document.location = '/@evolutions/apply/%s?redirect=' + encodeURIComponent(location)
    """.format(db).trim

  def htmlDescription = {

    <span>An SQL script will be run on your database -</span>
    <input name="evolution-button" type="button" value="Apply this script now!" onclick={ javascript }/>

  }.mkString

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
