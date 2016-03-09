/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.db.evolutions

import java.io.File
import java.nio.charset.Charset
import java.nio.file._

import play.api.db.{ DBApi, Database }
import play.api.inject.DefaultApplicationLifecycle
import play.api.libs.Codecs.sha1
import play.api.{ Configuration, Environment, Logger, Mode, Play }
import play.core.DefaultWebCommands
import play.utils.PlayIO

/**
 * An SQL evolution - database changes associated with a software version.
 *
 * An evolution includes ‘up’ changes, to upgrade to the next version, as well
 * as ‘down’ changes, to downgrade the database to the previous version.
 *
 * @param revision revision number
 * @param sql_up the SQL statements for UP application
 * @param sql_down the SQL statements for DOWN application
 */
case class Evolution(revision: Int, sql_up: String = "", sql_down: String = "") {

  /**
   * Revision hash, automatically computed from the SQL content.
   */
  val hash = sha1(sql_down.trim + sql_up.trim)

}

/**
 * A Script to run on the database.
 */
trait Script {

  /**
   * Original evolution.
   */
  def evolution: Evolution

  /**
   * The complete SQL to be run.
   */
  def sql: String

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
 */
case class UpScript(evolution: Evolution) extends Script {
  def sql: String = evolution.sql_up
}

/**
 * A DOWN Script to run on the database.
 *
 * @param evolution the original evolution
 */
case class DownScript(evolution: Evolution) extends Script {
  def sql: String = evolution.sql_down
}

/**
 * Defines database url patterns.
 */
private[evolutions] object DatabaseUrlPatterns {
  lazy val SqlServerJdbcUrl = "^jdbc:sqlserver:.*".r
  lazy val OracleJdbcUrl = "^jdbc:oracle:.*".r
  lazy val MysqlJdbcUrl = "^(jdbc:)?mysql:.*".r
  lazy val DerbyJdbcUrl = "^jdbc:derby:.*".r
}

/**
 * Defines Evolutions utilities functions.
 */
object Evolutions {

  /**
   * Default evolutions directory location.
   */
  def directoryName(db: String): String = s"conf/evolutions/${db}"

  /**
   * Default evolution file location.
   */
  def fileName(db: String, revision: Int): String = s"${directoryName(db)}/${revision}.sql"

  /**
   * Default evolution resource name.
   */
  def resourceName(db: String, revision: Int): String = s"evolutions/${db}/${revision}.sql"

  /**
   * Apply pending evolutions for the given DB.
   */
  def applyFor(dbName: String, path: java.io.File = new java.io.File("."), autocommit: Boolean = true, schema: String = ""): Unit = {
    val evolutions = Play.current.injector.instanceOf[EvolutionsApi]
    val scripts = evolutions.scripts(dbName, new EnvironmentEvolutionsReader(Environment.simple(path = path)), schema)
    evolutions.evolve(dbName, scripts, autocommit, schema)
  }

  /**
   * Updates a local (file-based) evolution script.
   */
  def updateEvolutionScript(db: String = "default", revision: Int = 1, comment: String = "Generated", ups: String, downs: String)(implicit environment: Environment) {
    val evolutions = environment.getFile(fileName(db, revision))
    Files.createDirectory(environment.getFile(directoryName(db)).toPath)
    writeFileIfChanged(evolutions,
      """|# --- %s
         |
         |# --- !Ups
         |%s
         |
         |# --- !Downs
         |%s
         |
         |""".stripMargin.format(comment, ups, downs))
  }

  private def writeFileIfChanged(path: File, content: String): Unit = {
    if (content != PlayIO.readFileAsString(path)) {
      writeFile(path, content)
    }
  }

  private def writeFile(destination: File, content: String): Unit = {
    Files.write(destination.toPath, content.getBytes(utf8))
  }

  private lazy val utf8 = Charset.forName("UTF8")

  /**
   * Translates evolution scripts into something human-readable.
   *
   * @param scripts the evolution scripts
   * @return a formatted script
   */
  def toHumanReadableScript(scripts: Seq[Script]): String = {
    val txt = scripts.map {
      case UpScript(ev) => "# --- Rev:" + ev.revision + ",Ups - " + ev.hash.take(7) + "\n" + ev.sql_up + "\n"
      case DownScript(ev) => "# --- Rev:" + ev.revision + ",Downs - " + ev.hash.take(7) + "\n" + ev.sql_down + "\n"
    }.mkString("\n")

    val hasDownWarning =
      "# !!! WARNING! This script contains DOWNS evolutions that are likely destructive\n\n"

    if (scripts.exists(_.isInstanceOf[DownScript])) hasDownWarning + txt else txt
  }

  /**
   *
   * Compare two evolution sequences.
   *
   * @param downs the seq of downs
   * @param ups the seq of ups
   * @return the downs and ups to run to have the db synced to the current stage
   */
  def conflictings(downs: Seq[Evolution], ups: Seq[Evolution]): (Seq[Evolution], Seq[Evolution]) =
    downs.zip(ups).reverse.dropWhile {
      case (down, up) => down.hash == up.hash
    }.reverse.unzip

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param evolutionsReader The reader to read the evolutions.
   * @param autocommit Whether to use autocommit or not, evolutions will be manually committed if false.
   * @param schema The schema where all the play evolution tables are saved in
   */
  def applyEvolutions(database: Database, evolutionsReader: EvolutionsReader = ThisClassLoaderEvolutionsReader,
    autocommit: Boolean = true, schema: String = ""): Unit = {
    val dbEvolutions = new DatabaseEvolutions(database, schema)
    val evolutions = dbEvolutions.scripts(evolutionsReader)
    dbEvolutions.evolve(evolutions, autocommit)
  }

  /**
   * Cleanup evolutions for the given database.
   *
   * This will leave the database in the original state it was before evolutions were applied, by running the down
   * scripts for all the evolutions that have been previously applied to the database.
   *
   * @param database The database to clean the evolutions for.
   * @param autocommit Whether to use atocommit or not, evolutions will be manually committed if false.
   * @param schema The schema where all the play evolution tables are saved in
   */
  def cleanupEvolutions(database: Database, autocommit: Boolean = true, schema: String = ""): Unit = {
    val dbEvolutions = new DatabaseEvolutions(database, schema)
    val evolutions = dbEvolutions.resetScripts()
    dbEvolutions.evolve(evolutions, autocommit)
  }

  /**
   * Execute the following code block with the evolutions for the database, cleaning up afterwards by running the downs.
   *
   * @param database The database to execute the evolutions on
   * @param evolutionsReader The evolutions reader to use.  Defaults to reading evolutions from the evolution readers own classloader.
   * @param autocommit Whether to use autocommit or not, evolutions will be manually committed if false.
   * @param block The block to execute
   * @param schema The schema where all the play evolution tables are saved in
   */
  def withEvolutions[T](database: Database, evolutionsReader: EvolutionsReader = ThisClassLoaderEvolutionsReader,
    autocommit: Boolean = true, schema: String = "")(block: => T): T = {
    applyEvolutions(database, evolutionsReader, autocommit, schema)
    try {
      block
    } finally {
      try {
        cleanupEvolutions(database, autocommit, schema)
      } catch {
        case e: Exception =>
          Logger.warn("Error resetting evolutions", e)
      }
    }
  }
}

/**
 * Can be used to run off-line evolutions, i.e. outside a running application.
 */
object OfflineEvolutions {

  private val logger = Logger(this.getClass)

  private def isTest: Boolean = Play.privateMaybeApplication.exists(_.mode == Mode.Test)

  private def getEvolutions(appPath: File, classloader: ClassLoader, dbApi: DBApi): EvolutionsComponents = {
    val _dbApi = dbApi
    new EvolutionsComponents {
      lazy val environment = Environment(appPath, classloader, Mode.Dev)
      lazy val configuration = Configuration.load(environment)
      lazy val applicationLifecycle = new DefaultApplicationLifecycle
      lazy val dbApi: DBApi = _dbApi
      lazy val webCommands = new DefaultWebCommands
    }
  }

  /**
   * Computes and applies an evolutions script.
   *
   * @param appPath the application path
   * @param classloader the classloader used to load the driver
   * @param dbName the database name
   * @param dbApi the database api for managing application databases
   * @param schema The schema where all the play evolution tables are saved in
   */
  def applyScript(appPath: File, classloader: ClassLoader, dbApi: DBApi, dbName: String, autocommit: Boolean = true, schema: String = ""): Unit = {
    val evolutions = getEvolutions(appPath, classloader, dbApi)
    val scripts = evolutions.evolutionsApi.scripts(dbName, evolutions.evolutionsReader, schema)
    if (!isTest) {
      logger.warn("Applying evolution scripts for database '" + dbName + "':\n\n" + Evolutions.toHumanReadableScript(scripts))
    }
    evolutions.evolutionsApi.evolve(dbName, scripts, autocommit, schema)
  }

  /**
   * Resolve an inconsistent evolution.
   *
   * @param appPath the application path
   * @param classloader the classloader used to load the driver
   * @param dbApi the database api for managing application databases
   * @param dbName the database name
   * @param revision the revision
   * @param schema The schema where all the play evolution tables are saved in
   */
  def resolve(appPath: File, classloader: ClassLoader, dbApi: DBApi, dbName: String, revision: Int, schema: String = ""): Unit = {
    val evolutions = getEvolutions(appPath, classloader, dbApi)
    if (!isTest) {
      logger.warn("Resolving evolution [" + revision + "] for database '" + dbName + "'")
    }
    evolutions.evolutionsApi.resolve(dbName, revision, schema)
  }

}
