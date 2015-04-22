/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db.evolutions

import java.sql.{ Statement, Connection, SQLException }
import javax.inject.{ Inject, Provider, Singleton }

import scala.util.control.Exception.ignoring

import play.api.db.{ Database, DBApi }
import play.api._
import play.core.{ HandleWebCommandSupport, WebCommands }

import play.api.db.evolutions.DatabaseUrlPatterns._

/**
 * Run evolutions on application startup. Automatically runs on construction.
 */
@Singleton
class ApplicationEvolutions @Inject() (
    config: EvolutionsConfig,
    reader: EvolutionsReader,
    evolutions: EvolutionsApi,
    dynamicEvolutions: DynamicEvolutions,
    dbApi: DBApi,
    environment: Environment,
    webCommands: WebCommands) {

  private val logger = Logger(classOf[ApplicationEvolutions])

  /**
   * Checks the evolutions state. Called on construction.
   */
  def start(): Unit = {

    webCommands.addHandler(new EvolutionsWebCommands(evolutions, reader, config))

    // allow db modules to write evolution files
    dynamicEvolutions.create()

    dbApi.databases().foreach(runEvolutions)
  }

  private def runEvolutions(database: Database): Unit = {
    val db = database.name
    val dbConfig = config.forDatasource(db)
    if (dbConfig.enabled) {
      withLock(database, dbConfig) {
        val scripts = evolutions.scripts(db, reader)
        val hasDown = scripts.exists(_.isInstanceOf[DownScript])

        val autocommit = dbConfig.autocommit

        if (scripts.nonEmpty) {

          import Evolutions.toHumanReadableScript

          environment.mode match {
            case Mode.Test => evolutions.evolve(db, scripts, autocommit)
            case Mode.Dev if dbConfig.autoApply => evolutions.evolve(db, scripts, autocommit)
            case Mode.Prod if !hasDown && dbConfig.autoApply => evolutions.evolve(db, scripts, autocommit)
            case Mode.Prod if hasDown && dbConfig.autoApply && dbConfig.autoApplyDowns => evolutions.evolve(db, scripts, autocommit)
            case Mode.Prod if hasDown =>
              logger.warn(s"Your production database [$db] needs evolutions, including downs! \n\n${toHumanReadableScript(scripts)}")
              logger.warn(s"Run with -Dplay.modules.evolutions.db.$db.autoApply=true and -Dplay.modules.evolutions.db.$db.autoApplyDowns=true if you want to run them automatically, including downs (be careful, especially if your down evolutions drop existing data)")

              throw InvalidDatabaseRevision(db, toHumanReadableScript(scripts))

            case Mode.Prod =>
              logger.warn(s"Your production database [$db] needs evolutions! \n\n${toHumanReadableScript(scripts)}")
              logger.warn(s"Run with -Dplay.modules.evolutions.db.$db.autoApply=true if you want to run them automatically (be careful)")

              throw InvalidDatabaseRevision(db, toHumanReadableScript(scripts))

            case _ => throw InvalidDatabaseRevision(db, toHumanReadableScript(scripts))
          }
        }
      }
    }
  }

  private def withLock(db: Database, dbConfig: EvolutionsDatasourceConfig)(block: => Unit): Unit = {
    if (dbConfig.useLocks) {
      val ds = db.dataSource
      val url = db.url
      val c = ds.getConnection
      c.setAutoCommit(false)
      val s = c.createStatement()
      createLockTableIfNecessary(url, c, s)
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

  private def createLockTableIfNecessary(url: String, c: Connection, s: Statement): Unit = {
    import ApplicationEvolutions._
    try {
      val r = s.executeQuery("select lock from play_evolutions_lock")
      r.close()
    } catch {
      case e: SQLException =>
        c.rollback()
        val createScript = url match {
          case OracleJdbcUrl() => CreatePlayEvolutionsLockOracleSql
          case _ => CreatePlayEvolutionsLockSql
        }
        s.execute(createScript)
        s.executeUpdate("insert into play_evolutions_lock (lock) values (1)")
    }
  }

  private def lock(c: Connection, s: Statement, attempts: Int = 5): Unit = {
    try {
      s.executeQuery("select lock from play_evolutions_lock where lock = 1 for update nowait")
    } catch {
      case e: SQLException =>
        if (attempts == 0) throw e
        else {
          logger.warn("Exception while attempting to lock evolutions (other node probably has lock), sleeping for 1 sec")
          c.rollback()
          Thread.sleep(1000)
          lock(c, s, attempts - 1)
        }
    }
  }

  private def unlock(c: Connection, s: Statement): Unit = {
    ignoring(classOf[SQLException])(s.close())
    ignoring(classOf[SQLException])(c.commit())
    ignoring(classOf[SQLException])(c.close())
  }

  start() // on construction
}

private object ApplicationEvolutions {

  val CreatePlayEvolutionsLockSql =
    """
      create table play_evolutions_lock (
        lock int not null primary key
      )
    """

  val CreatePlayEvolutionsLockOracleSql =
    """
      CREATE TABLE play_evolutions_lock (
        lock Number(10,0) Not Null Enable,
        CONSTRAINT play_evolutions_lock_pk PRIMARY KEY (lock)
      )
    """
}

/**
 * Evolutions configuration for a given datasource.
 */
trait EvolutionsDatasourceConfig {
  def enabled: Boolean
  def autocommit: Boolean
  def useLocks: Boolean
  def autoApply: Boolean
  def autoApplyDowns: Boolean
}

/**
 * Evolutions configuration for all datasources.
 */
trait EvolutionsConfig {
  def forDatasource(db: String): EvolutionsDatasourceConfig
}

/**
 * Default evolutions datasource configuration.
 */
case class DefaultEvolutionsDatasourceConfig(
  enabled: Boolean,
  autocommit: Boolean,
  useLocks: Boolean,
  autoApply: Boolean,
  autoApplyDowns: Boolean) extends EvolutionsDatasourceConfig

/**
 * Default evolutions configuration.
 */
class DefaultEvolutionsConfig(defaultDatasourceConfig: EvolutionsDatasourceConfig,
    datasources: Map[String, EvolutionsDatasourceConfig]) extends EvolutionsConfig {
  def forDatasource(db: String) = datasources.getOrElse(db, defaultDatasourceConfig)
}

/**
 * A provider that creates an EvolutionsConfig from the play.api.Configuration.
 */
@Singleton
class DefaultEvolutionsConfigParser @Inject() (configuration: Configuration) extends Provider[EvolutionsConfig] {

  private val logger = Logger(classOf[DefaultEvolutionsConfigParser])

  def get = parse()

  def parse(): EvolutionsConfig = {
    val rootConfig = PlayConfig(configuration)
    val config = rootConfig.get[PlayConfig]("play.evolutions")

    // Since the evolutions config was completely inverted and has changed massively, we have our own deprecated
    // implementation that reads deprecated keys from the root config, otherwise reads from the passed in config
    def getDeprecated[A: ConfigLoader](config: PlayConfig, baseKey: => String, path: String, deprecated: String): A = {
      if (rootConfig.underlying.hasPath(deprecated)) {
        rootConfig.reportDeprecation(s"$baseKey.$path", deprecated)
        rootConfig.get[A](deprecated)
      } else {
        config.get[A](path)
      }
    }

    // Find all the defined datasources, both using the old format, and the new format
    def loadDatasources(path: String) = {
      if (rootConfig.underlying.hasPath(path)) {
        rootConfig.get[PlayConfig](path).subKeys
      } else {
        Set.empty[String]
      }
    }
    val datasources = config.get[PlayConfig]("db").subKeys ++
      loadDatasources("applyEvolutions") ++
      loadDatasources("applyDownEvolutions")

    // Load defaults
    val enabled = config.get[Boolean]("enabled")
    val autocommit = getDeprecated[Boolean](config, "play.evolutions", "autocommit", "evolutions.autocommit")
    val useLocks = getDeprecated[Boolean](config, "play.evolutions", "useLocks", "evolutions.use.locks")
    val autoApply = config.get[Boolean]("autoApply")
    val autoApplyDowns = config.get[Boolean]("autoApplyDowns")

    val defaultConfig = new DefaultEvolutionsDatasourceConfig(enabled, autocommit, useLocks, autoApply,
      autoApplyDowns)

    // Load config specific to datasources
    // Since not all the datasources will necessarily appear in the db map, because some will come from deprecated
    // configuration, we create a map of them to the default config, and then override any of them with the ones
    // from db.
    val datasourceConfigMap = datasources.map(_ -> config).toMap ++ config.getPrototypedMap("db", "")
    val datasourceConfig = datasourceConfigMap.map {
      case (datasource, dsConfig) =>
        val enabled = dsConfig.get[Boolean]("enabled")
        val autocommit = dsConfig.get[Boolean]("autocommit")
        val useLocks = dsConfig.get[Boolean]("useLocks")
        val autoApply = getDeprecated[Boolean](dsConfig, s"play.evolutions.db.$datasource", "autoApply", s"applyEvolutions.$datasource")
        val autoApplyDowns = getDeprecated[Boolean](dsConfig, s"play.evolutions.db.$datasource", "autoApplyDowns", s"applyDownEvolutions.$datasource")
        datasource -> new DefaultEvolutionsDatasourceConfig(enabled, autocommit, useLocks, autoApply, autoApplyDowns)
    }.toMap

    new DefaultEvolutionsConfig(defaultConfig, datasourceConfig)
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
 * Default implementation for optional dynamic evolutions.
 */
@Singleton
class DynamicEvolutions {
  def create(): Unit = ()
}

/**
 * Web command handler for applying evolutions on application start.
 */
@Singleton
class EvolutionsWebCommands @Inject() (evolutions: EvolutionsApi, reader: EvolutionsReader, config: EvolutionsConfig) extends HandleWebCommandSupport {
  def handleWebCommand(request: play.api.mvc.RequestHeader, buildLink: play.core.BuildLink, path: java.io.File): Option[play.api.mvc.Result] = {
    val applyEvolutions = """/@evolutions/apply/([a-zA-Z0-9_]+)""".r
    val resolveEvolutions = """/@evolutions/resolve/([a-zA-Z0-9_]+)/([0-9]+)""".r

    lazy val redirectUrl = request.queryString.get("redirect").filterNot(_.isEmpty).map(_.head).getOrElse("/")

    request.path match {

      case applyEvolutions(db) => {
        Some {
          val scripts = evolutions.scripts(db, reader)
          evolutions.evolve(db, scripts, config.forDatasource(db).autocommit)
          buildLink.forceReload()
          play.api.mvc.Results.Redirect(redirectUrl)
        }
      }

      case resolveEvolutions(db, rev) => {
        Some {
          evolutions.resolve(db, rev.toInt)
          buildLink.forceReload()
          play.api.mvc.Results.Redirect(redirectUrl)
        }
      }

      case _ => None

    }
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
