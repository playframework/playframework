/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db.evolutions

import java.sql.{ Statement, Connection, SQLException }
import javax.inject.{ Inject, Provider, Singleton }

import scala.collection.breakOut
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
        val schema = dbConfig.schema
        val autocommit = dbConfig.autocommit

        val scripts = evolutions.scripts(db, reader, schema)
        val hasDown = scripts.exists(_.isInstanceOf[DownScript])
        val onlyDowns = scripts.forall(_.isInstanceOf[DownScript])

        if (scripts.nonEmpty && !(onlyDowns && dbConfig.skipApplyDownsOnly)) {

          import Evolutions.toHumanReadableScript

          environment.mode match {
            case Mode.Test => evolutions.evolve(db, scripts, autocommit, schema)
            case Mode.Dev if dbConfig.autoApply => evolutions.evolve(db, scripts, autocommit, schema)
            case Mode.Prod if !hasDown && dbConfig.autoApply => evolutions.evolve(db, scripts, autocommit, schema)
            case Mode.Prod if hasDown && dbConfig.autoApply && dbConfig.autoApplyDowns => evolutions.evolve(db, scripts, autocommit, schema)
            case Mode.Prod if hasDown =>
              logger.warn(s"Your production database [$db] needs evolutions, including downs! \n\n${toHumanReadableScript(scripts)}")
              logger.warn(s"Run with -Dplay.evolutions.db.$db.autoApply=true and -Dplay.evolutions.db.$db.autoApplyDowns=true if you want to run them automatically, including downs (be careful, especially if your down evolutions drop existing data)")

              throw InvalidDatabaseRevision(db, toHumanReadableScript(scripts))

            case Mode.Prod =>
              logger.warn(s"Your production database [$db] needs evolutions! \n\n${toHumanReadableScript(scripts)}")
              logger.warn(s"Run with -Dplay.evolutions.db.$db.autoApply=true if you want to run them automatically (be careful)")

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
      createLockTableIfNecessary(url, c, s, dbConfig)
      lock(url, c, s, dbConfig)
      try {
        block
      } finally {
        unlock(c, s)
      }
    } else {
      block
    }
  }

  private def createLockTableIfNecessary(url: String, c: Connection, s: Statement, dbConfig: EvolutionsDatasourceConfig): Unit = {
    import ApplicationEvolutions._
    val (selectScript, createScript, insertScript) = url match {
      case OracleJdbcUrl() =>
        (SelectPlayEvolutionsLockSql, CreatePlayEvolutionsLockOracleSql, InsertIntoPlayEvolutionsLockSql)
      case MysqlJdbcUrl(_) =>
        (SelectPlayEvolutionsLockMysqlSql, CreatePlayEvolutionsLockMysqlSql, InsertIntoPlayEvolutionsLockMysqlSql)
      case _ =>
        (SelectPlayEvolutionsLockSql, CreatePlayEvolutionsLockSql, InsertIntoPlayEvolutionsLockSql)
    }
    try {
      val r = s.executeQuery(applySchema(selectScript, dbConfig.schema))
      r.close()
    } catch {
      case e: SQLException =>
        c.rollback()
        s.execute(applySchema(createScript, dbConfig.schema))
        s.executeUpdate(applySchema(insertScript, dbConfig.schema))
    }
  }

  private def lock(url: String, c: Connection, s: Statement, dbConfig: EvolutionsDatasourceConfig, attempts: Int = 5): Unit = {
    import ApplicationEvolutions._
    val lockScripts = url match {
      case MysqlJdbcUrl(_) => lockPlayEvolutionsLockMysqlSqls
      case _ => lockPlayEvolutionsLockSqls
    }
    try {
      for (script <- lockScripts) s.executeQuery(applySchema(script, dbConfig.schema))
    } catch {
      case e: SQLException =>
        if (attempts == 0) throw e
        else {
          logger.warn("Exception while attempting to lock evolutions (other node probably has lock), sleeping for 1 sec")
          c.rollback()
          Thread.sleep(1000)
          lock(url, c, s, dbConfig, attempts - 1)
        }
    }
  }

  private def unlock(c: Connection, s: Statement): Unit = {
    ignoring(classOf[SQLException])(s.close())
    ignoring(classOf[SQLException])(c.commit())
    ignoring(classOf[SQLException])(c.close())
  }

  start() // on construction

  // SQL helpers

  private def applySchema(sql: String, schema: String): String = {
    sql.replaceAll("\\$\\{schema}", Option(schema).filter(_.trim.nonEmpty).map(_.trim + ".").getOrElse(""))
  }
}

private object ApplicationEvolutions {

  val SelectPlayEvolutionsLockSql =
    """
      select lock from ${schema}play_evolutions_lock
    """

  val SelectPlayEvolutionsLockMysqlSql =
    """
      select `lock` from ${schema}play_evolutions_lock
    """

  val CreatePlayEvolutionsLockSql =
    """
      create table ${schema}play_evolutions_lock (
        lock int not null primary key
      )
    """

  val CreatePlayEvolutionsLockMysqlSql =
    """
      create table ${schema}play_evolutions_lock (
        `lock` int not null primary key
      )
    """

  val CreatePlayEvolutionsLockOracleSql =
    """
      CREATE TABLE ${schema}play_evolutions_lock (
        lock Number(10,0) Not Null Enable,
        CONSTRAINT play_evolutions_lock_pk PRIMARY KEY (lock)
      )
    """

  val InsertIntoPlayEvolutionsLockSql =
    """
      insert into ${schema}play_evolutions_lock (lock) values (1)
    """

  val InsertIntoPlayEvolutionsLockMysqlSql =
    """
      insert into ${schema}play_evolutions_lock (`lock`) values (1)
    """

  val lockPlayEvolutionsLockSqls =
    List(
      """
        select lock from ${schema}play_evolutions_lock where lock = 1 for update nowait
      """
    )

  val lockPlayEvolutionsLockMysqlSqls =
    List(
      """
        set innodb_lock_wait_timeout = 1
      """,
      """
        select `lock` from ${schema}play_evolutions_lock where `lock` = 1 for update
      """
    )
}

/**
 * Evolutions configuration for a given datasource.
 */
trait EvolutionsDatasourceConfig {
  def enabled: Boolean
  def schema: String
  def autocommit: Boolean
  def useLocks: Boolean
  def autoApply: Boolean
  def autoApplyDowns: Boolean
  def skipApplyDownsOnly: Boolean
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
    schema: String,
    autocommit: Boolean,
    useLocks: Boolean,
    autoApply: Boolean,
    autoApplyDowns: Boolean,
    skipApplyDownsOnly: Boolean) extends EvolutionsDatasourceConfig

/**
 * Default evolutions configuration.
 */
class DefaultEvolutionsConfig(
    defaultDatasourceConfig: EvolutionsDatasourceConfig,
    datasources: Map[String, EvolutionsDatasourceConfig]) extends EvolutionsConfig {
  def forDatasource(db: String) = datasources.getOrElse(db, defaultDatasourceConfig)
}

/**
 * A provider that creates an EvolutionsConfig from the play.api.Configuration.
 */
@Singleton
class DefaultEvolutionsConfigParser @Inject() (rootConfig: Configuration) extends Provider[EvolutionsConfig] {

  private val logger = Logger(classOf[DefaultEvolutionsConfigParser])

  def get = parse()

  def parse(): EvolutionsConfig = {
    val config = rootConfig.get[Configuration]("play.evolutions")

    // Since the evolutions config was completely inverted and has changed massively, we have our own deprecated
    // implementation that reads deprecated keys from the root config, otherwise reads from the passed in config
    def getDeprecated[A: ConfigLoader](config: Configuration, baseKey: => String, path: String, deprecated: String): A = {
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
        rootConfig.get[Configuration](path).subKeys
      } else {
        Set.empty[String]
      }
    }
    val datasources = config.get[Configuration]("db").subKeys ++
      loadDatasources("applyEvolutions") ++
      loadDatasources("applyDownEvolutions")

    // Load defaults
    val enabled = config.get[Boolean]("enabled")
    val schema = config.get[String]("schema")
    val autocommit = getDeprecated[Boolean](config, "play.evolutions", "autocommit", "evolutions.autocommit")
    val useLocks = getDeprecated[Boolean](config, "play.evolutions", "useLocks", "evolutions.use.locks")
    val autoApply = config.get[Boolean]("autoApply")
    val autoApplyDowns = config.get[Boolean]("autoApplyDowns")
    val skipApplyDownsOnly = config.get[Boolean]("skipApplyDownsOnly")

    val defaultConfig = new DefaultEvolutionsDatasourceConfig(enabled, schema, autocommit, useLocks, autoApply,
      autoApplyDowns, skipApplyDownsOnly)

    // Load config specific to datasources
    // Since not all the datasources will necessarily appear in the db map, because some will come from deprecated
    // configuration, we create a map of them to the default config, and then override any of them with the ones
    // from db.
    val datasourceConfigMap = (datasources.map(_ -> config)(
      breakOut): Map[String, Configuration]) ++ config.
      getPrototypedMap("db", "")

    val datasourceConfig: Map[String, DefaultEvolutionsDatasourceConfig] =
      datasourceConfigMap.map {
        case (datasource, dsConfig) =>
          val enabled = dsConfig.get[Boolean]("enabled")
          val schema = dsConfig.get[String]("schema")
          val autocommit = dsConfig.get[Boolean]("autocommit")
          val useLocks = dsConfig.get[Boolean]("useLocks")
          val autoApply = getDeprecated[Boolean](dsConfig, s"play.evolutions.db.$datasource", "autoApply", s"applyEvolutions.$datasource")
          val autoApplyDowns = getDeprecated[Boolean](dsConfig, s"play.evolutions.db.$datasource", "autoApplyDowns", s"applyDownEvolutions.$datasource")
          val skipApplyDownsOnly = getDeprecated[Boolean](dsConfig, s"play.evolutions.db.$datasource", "skipApplyDownsOnly", s"skipApplyDownsOnly.$datasource")
          datasource -> new DefaultEvolutionsDatasourceConfig(enabled, schema, autocommit, useLocks, autoApply, autoApplyDowns, skipApplyDownsOnly)
      }(breakOut)

    new DefaultEvolutionsConfig(defaultConfig, datasourceConfig)
  }

  /**
   * Convert configuration sections of key-boolean pairs to a set of enabled keys.
   */
  def enabledKeys(configuration: Configuration, section: String): Set[String] = {
    configuration.getOptional[Configuration](section).fold(Set.empty[String]) { conf =>
      conf.keys.filter(conf.getOptional[Boolean](_).getOrElse(false))
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
    val applyEvolutions = """/@evolutions/apply/([a-zA-Z0-9_-]+)""".r
    val resolveEvolutions = """/@evolutions/resolve/([a-zA-Z0-9_-]+)/([0-9]+)""".r

    lazy val redirectUrl = request.queryString.get("redirect").filterNot(_.isEmpty).map(_.head).getOrElse("/")

    // Regex removes all parent directories from request path
    request.path.replaceFirst("^((?!/@evolutions).)*(/@evolutions.*$)", "$2") match {

      case applyEvolutions(db) => {
        Some {
          val scripts = evolutions.scripts(db, reader, config.forDatasource(db).schema)
          evolutions.evolve(db, scripts, config.forDatasource(db).autocommit, config.forDatasource(db).schema)
          buildLink.forceReload()
          play.api.mvc.Results.Redirect(redirectUrl)
        }
      }

      case resolveEvolutions(db, rev) => {
        Some {
          evolutions.resolve(db, rev.toInt, config.forDatasource(db).schema)
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
        window.location = window.location.href.split(/[?#]/)[0].replace(/\/@evolutions.*$|\/$/, '') + '/@evolutions/apply/%s?redirect=' + encodeURIComponent(location)
    """.format(db).trim

  def htmlDescription = {

    <span>An SQL script will be run on your database -</span>
    <input name="evolution-button" type="button" value="Apply this script now!" onclick={ javascript }/>

  }.mkString

}
