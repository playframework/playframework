/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db.evolutions

import java.io._
import javax.inject.{ Inject, Provider, Singleton }

import play.core._

import play.api._
import play.api.db._
import play.api.inject._
import play.api.libs._
import play.api.libs.Codecs._
import javax.sql.DataSource
import java.sql.{ Statement, Date, Connection, SQLException }
import scala.util.control.Exception._
import scala.util.control.NonFatal
import play.utils.PlayIO
import scala.io.Codec

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
 * Evolutions API.
 */
trait EvolutionsApi {
  /**
   * Updates a local (file-based) evolution script.
   */
  def updateEvolutionScript(db: String = "default", revision: Int = 1, comment: String = "Generated", ups: String, downs: String): Unit

  /**
   * Computes the evolution script.
   *
   * @param db the database name
   * @return evolution scripts
   */
  def evolutionScript(db: String): Seq[Product with Serializable with Script]

  /**
   * Applies a script to the database.
   *
   * @param db the database name
   * @param script the script to run
   */
  def applyScript(db: String, script: Seq[Script], autocommit: Boolean): Unit

  /**
   * Resolves evolution conflicts.
   *
   * @param db the database name
   * @param revision the revision to mark as resolved
   */
  def resolve(db: String, revision: Int): Unit
}

@Singleton
class DefaultEvolutionsApi @Inject() (config: EvolutionsConfig, environment: Environment, dbApi: DBApi, webCommands: WebCommands) extends EvolutionsApi with HandleWebCommandSupport {

  /**
   * Updates a local (file-based) evolution script.
   */
  def updateEvolutionScript(db: String = "default", revision: Int = 1, comment: String = "Generated", ups: String, downs: String): Unit = {
    import play.api.libs._

    val evolutions = environment.getFile(evolutionsFilename(db, revision));
    Files.createDirectory(environment.getFile(evolutionsDirectoryName(db)));
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

  private def evolutionsDirectoryName(db: String): String = s"conf/evolutions/${db}"

  private def evolutionsFilename(db: String, revision: Int): String = s"conf/evolutions/${db}/${revision}.sql"

  private def evolutionsResourceName(db: String, revision: Int): String = s"evolutions/${db}/${revision}.sql"

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
   * @param db the database name
   * @param revision the revision to mark as resolved
   */
  def resolve(db: String, revision: Int): Unit = {
    implicit val connection = dbApi.getConnection(db, autocommit = true)

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
   * @param db the database name
   * @throws an error if the database is in an inconsistent state
   */
  def checkEvolutionsState(db: String): Unit = {
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
        case NonFatal(ex) => Logger.warn("could not create play_evolutions table", ex)
      }
    }

    implicit val connection = dbApi.getConnection(db, autocommit = true)

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
   * @param db the database name
   * @param script the script to run
   */
  def applyScript(db: String, script: Seq[Script], autocommit: Boolean): Unit = {
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

    implicit val connection = dbApi.getConnection(db, autocommit = autocommit)
    checkEvolutionsState(db)

    var applying = -1
    var lastScript: Script = null

    try {

      script.foreach { s =>
        lastScript = s
        applying = s.evolution.revision
        logBefore(s)
        // Execute script
        s.statements.foreach(execute)
        logAfter(s)
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
          Play.logger.error(message)

          connection.rollback();

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
   * Computes the evolution script.
   *
   * @param db the database name
   * @return evolution scripts
   */
  def evolutionScript(db: String): Seq[Product with Serializable with Script] = {
    val application = applicationEvolutions(db)

    Option(application).filterNot(_.isEmpty).map {
      case application =>
        val database = databaseEvolutions(db)

        val (nonConflictingDowns, dRest) = database.span(e => !application.headOption.exists(e.revision <= _.revision))
        val (nonConflictingUps, uRest) = application.span(e => !database.headOption.exists(_.revision >= e.revision))

        val (conflictingDowns, conflictingUps) = Evolutions.conflictings(dRest, uRest)

        val ups = (nonConflictingUps ++ conflictingUps).reverse.map(e => UpScript(e, e.sql_up))
        val downs = (nonConflictingDowns ++ conflictingDowns).map(e => DownScript(e, e.sql_down))

        downs ++ ups
    }.getOrElse(Nil)
  }

  /**
   * Reads the evolutions from the application.
   *
   * @param db the database name
   */
  def applicationEvolutions(db: String): Seq[Evolution] = {

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
      Option(new File(environment.rootPath, evolutionsFilename(db, revision))).filter(_.exists).map(new FileInputStream(_)).orElse {
        Option(environment.classLoader.getResourceAsStream(evolutionsResourceName(db, revision)))
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
    }.reverse

  }

  /**
   * Reads evolutions from the database.
   *
   * @param db the database name
   */
  def databaseEvolutions(db: String): Seq[Evolution] = {
    implicit val connection = dbApi.getConnection(db, autocommit = true)

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
              rs.getString(3),
              rs.getString(4))))
          }
        }
      }

    } finally {
      connection.close()
    }
  }

  /**
   * Checks the evolutions state.
   */
  private def start(): Unit = {
    import Evolutions.toHumanReadableScript

    dbApi.datasources.foreach {
      case (ds, db) => {
        withLock(ds) {
          val script = evolutionScript(db)
          val hasDown = script.exists(_.isInstanceOf[DownScript])

          val autocommit = config.autocommit

          lazy val applyEvolutions = config.applyEvolutions(db)
          lazy val applyDownEvolutions = config.applyDownEvolutions(db)

          if (!script.isEmpty) {
            environment.mode match {
              case Mode.Test => applyScript(db, script, autocommit)
              case Mode.Dev if applyEvolutions => applyScript(db, script, autocommit)
              case Mode.Prod if !hasDown && applyEvolutions => applyScript(db, script, autocommit)
              case Mode.Prod if hasDown && applyEvolutions && applyDownEvolutions => applyScript(db, script, autocommit)
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

  private def withLock(ds: DataSource)(block: => Unit) {
    if (config.useLocks) {
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

  private def createLockTableIfNecessary(c: Connection, s: Statement) {
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

  private def lock(c: Connection, s: Statement, attempts: Int = 5) {
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

  private def unlock(c: Connection, s: Statement) {
    ignoring(classOf[SQLException])(s.close())
    ignoring(classOf[SQLException])(c.commit())
    ignoring(classOf[SQLException])(c.close())
  }

  def handleWebCommand(request: play.api.mvc.RequestHeader, buildLink: play.core.BuildLink, path: java.io.File): Option[play.api.mvc.Result] = {
    val applyEvolutions = """/@evolutions/apply/([a-zA-Z0-9_]+)""".r
    val resolveEvolutions = """/@evolutions/resolve/([a-zA-Z0-9_]+)/([0-9]+)""".r

    lazy val redirectUrl = request.queryString.get("redirect").filterNot(_.isEmpty).map(_(0)).getOrElse("/")

    request.path match {

      case applyEvolutions(db) => {
        Some {
          val script = evolutionScript(db)
          applyScript(db, script, config.autocommit)
          buildLink.forceReload()
          play.api.mvc.Results.Redirect(redirectUrl)
        }
      }

      case resolveEvolutions(db, rev) => {
        Some {
          resolve(db, rev.toInt)
          buildLink.forceReload()
          play.api.mvc.Results.Redirect(redirectUrl)
        }
      }

      case _ => None

    }
  }

  webCommands.addHandler(this)

  start() // on construction
}

/**
 * Defines Evolutions utilities functions.
 */
object Evolutions {

  /**
   * Apply pending evolutions for the given DB.
   */
  def applyFor(dbName: String, path: java.io.File): Unit = {
    // NB: path is ignored -- evolutions api gets the application path from environment
    applyFor(dbName, true)
  }

  /**
   * Apply pending evolutions for the given DB.
   */
  def applyFor(dbName: String, path: java.io.File, autocommit: Boolean): Unit = {
    // NB: path is ignored -- evolutions api gets the application path from environment
    applyFor(dbName, autocommit)
  }

  /**
   * Apply pending evolutions for the given DB.
   */
  def applyFor(dbName: String, autocommit: Boolean = true): Unit = {
    val evolutionsApi = Play.current.injector.instanceOf[EvolutionsApi]
    val script = evolutionsApi.evolutionScript(dbName)
    evolutionsApi.applyScript(dbName, script, autocommit)
  }

  /**
   * Updates a local (file-based) evolution script.
   */
  def updateEvolutionScript(db: String = "default", revision: Int = 1, comment: String = "Generated", ups: String, downs: String)(implicit application: Application) {
    val evolutionsApi = application.injector.instanceOf[EvolutionsApi]
    evolutionsApi.updateEvolutionScript(db, revision, comment, ups, downs)
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
}

/**
 * Evolutions configuration interface.
 */
trait EvolutionsConfig {
  def autocommit: Boolean
  def useLocks: Boolean
  def applyEvolutions(db: String): Boolean
  def applyDownEvolutions(db: String): Boolean
}

case class DefaultEvolutionsConfig(
    autocommit: Boolean,
    useLocks: Boolean,
    enabledEvolutions: Set[String],
    enabledDownEvolutions: Set[String]) extends EvolutionsConfig {
  def applyEvolutions(db: String): Boolean = enabledEvolutions(db)
  def applyDownEvolutions(db: String): Boolean = enabledDownEvolutions(db)
}

/**
 * A provider that creates an EvolutionsConfig from the play.api.Configuration.
 */
class DefaultEvolutionsConfigParser @Inject() (configuration: Configuration) extends Provider[EvolutionsConfig] {
  def get = parse()

  def parse(): EvolutionsConfig = {
    val autocommit = configuration.getBoolean("evolutions.autocommit").getOrElse(true)
    val useLocks = configuration.getBoolean("evolutions.use.locks").getOrElse(false)
    val applyEvolutions = enabledKeys(configuration, "applyEvolutions")
    val applyDownEvolutions = enabledKeys(configuration, "applyDownEvolutions")
    DefaultEvolutionsConfig(autocommit, useLocks, applyEvolutions, applyDownEvolutions)
  }

  /**
   * Convert configuration sections of key-boolean pairs to a set of enabled keys.
   */
  def enabledKeys(configuration: Configuration, section: String): Set[String] = {
    configuration.getConfig(section).fold(Set.empty[String]) { conf =>
      conf.keys.filter(conf.getBoolean(_).getOrElse(false))
    }
  }
}

/**
 * Default module for evolutions API.
 */
@Singleton
class EvolutionsModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    if (configuration.underlying.getBoolean("play.modules.evolutions.enabled")) {
      Seq(
        bind[EvolutionsApi].to[DefaultEvolutionsApi].eagerly,
        bind[EvolutionsConfig].toProvider[DefaultEvolutionsConfigParser].in[Singleton]
      )
    } else {
      Nil
    }
  }
}

/**
 * Components for default implementation of the evolutions API.
 */
trait EvolutionsComponents {
  def environment: Environment
  def configuration: Configuration
  def dbApi: DBApi
  def webCommands: WebCommands

  lazy val evolutionsConfig: EvolutionsConfig = new DefaultEvolutionsConfigParser(configuration).parse
  lazy val evolutionsApi: EvolutionsApi = new DefaultEvolutionsApi(evolutionsConfig, environment, dbApi, webCommands)
}

/**
 * Can be used to run off-line evolutions, i.e. outside a running application.
 */
object OfflineEvolutions {

  private def isTest: Boolean = Play.maybeApplication.exists(_.mode == Mode.Test)

  private def getEvolutionsApi(appPath: File, classloader: ClassLoader): EvolutionsApi = {
    val components = new {
      val environment = Environment(appPath, classloader, Mode.Dev)
      val configuration = Configuration.load(appPath)
      val applicationLifecycle = new DefaultApplicationLifecycle
      val webCommands = new DefaultWebCommands
    } with BoneCPComponents with EvolutionsComponents
    components.evolutionsApi
  }

  /**
   * Computes and applies an evolutions script.
   *
   * @param appPath the application path
   * @param classloader the classloader used to load the driver
   * @param dbName the database name
   */
  def applyScript(appPath: File, classloader: ClassLoader, dbName: String, autocommit: Boolean = true): Unit = {
    val evolutionsApi = getEvolutionsApi(appPath, classloader)
    val script = evolutionsApi.evolutionScript(dbName)
    if (!isTest) {
      Play.logger.warn("Applying evolution script for database '" + dbName + "':\n\n" + Evolutions.toHumanReadableScript(script))
    }
    evolutionsApi.applyScript(dbName, script, autocommit)
  }

  /**
   * Resolve an inconsistent evolution..
   *
   * @param appPath the application path
   * @param classloader the classloader used to load the driver
   * @param dbName the database name
   * @param revision the revision
   */
  def resolve(appPath: File, classloader: ClassLoader, dbName: String, revision: Int): Unit = {
    val evolutionsApi = getEvolutionsApi(appPath, classloader)
    if (!isTest) {
      Play.logger.warn("Resolving evolution [" + revision + "] for database '" + dbName + "'")
    }
    evolutionsApi.resolve(dbName, revision)
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
