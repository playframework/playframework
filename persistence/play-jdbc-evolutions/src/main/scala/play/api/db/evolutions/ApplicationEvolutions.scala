/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db.evolutions

import java.sql.Statement
import java.sql.Connection
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import scala.util.control.Exception.ignoring

import play.api.db.Database
import play.api.db.DBApi
import play.api._
import play.core.HandleWebCommandSupport
import play.core.WebCommands

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
    webCommands: WebCommands
) {
  private val logger = Logger(classOf[ApplicationEvolutions])

  private var invalidDatabaseRevisions = 0

  /**
   * Indicates if the process of applying evolutions scripts is finished or not.
   * Only if that method returns true you can be sure that all evolutions scripts were executed successfully.
   *
   * @return true if all evolutions scripts were applied (or resolved) successfully.
   */
  def upToDate = invalidDatabaseRevisions == 0

  /**
   * Checks the evolutions state. Called on construction.
   */
  def start(): Unit = {
    webCommands.addHandler(new EvolutionsWebCommands(dbApi, evolutions, reader, config))

    // allow db modules to write evolution files
    dynamicEvolutions.create()

    dbApi
      .databases()
      .foreach(
        ApplicationEvolutions.runEvolutions(
          _,
          config,
          evolutions,
          reader,
          (db, dbConfig, scripts, hasDown) => {
            import Evolutions.toHumanReadableScript

            def invalidDatabaseRevision() = {
              invalidDatabaseRevisions += 1
              throw InvalidDatabaseRevision(db, toHumanReadableScript(scripts))
            }

            environment.mode match {
              case Mode.Test => evolutions.evolve(db, scripts, dbConfig.autocommit, dbConfig.schema, dbConfig.metaTable)
              case Mode.Dev if !dbConfig.autoApply =>
                invalidDatabaseRevisions += 1 // In DEV mode EvolutionsWebCommands handle non-autoApply evolutions
              case Mode.Dev if dbConfig.autoApply =>
                evolutions.evolve(db, scripts, dbConfig.autocommit, dbConfig.schema, dbConfig.metaTable)
              case Mode.Prod if !hasDown && dbConfig.autoApply =>
                evolutions.evolve(db, scripts, dbConfig.autocommit, dbConfig.schema, dbConfig.metaTable)
              case Mode.Prod if hasDown && dbConfig.autoApply && dbConfig.autoApplyDowns =>
                evolutions.evolve(db, scripts, dbConfig.autocommit, dbConfig.schema, dbConfig.metaTable)
              case Mode.Prod if hasDown =>
                logger.warn(
                  s"Your production database [$db] needs evolutions, including downs! \n\n${toHumanReadableScript(scripts)}"
                )
                logger.warn(
                  s"Run with -Dplay.evolutions.db.$db.autoApply=true and -Dplay.evolutions.db.$db.autoApplyDowns=true if you want to run them automatically, including downs (be careful, especially if your down evolutions drop existing data)"
                )

                invalidDatabaseRevision()

              case Mode.Prod =>
                logger.warn(s"Your production database [$db] needs evolutions! \n\n${toHumanReadableScript(scripts)}")
                logger.warn(
                  s"Run with -Dplay.evolutions.db.$db.autoApply=true if you want to run them automatically (be careful)"
                )

                invalidDatabaseRevision()

              case _ =>
                invalidDatabaseRevision()
            }
          }
        )
      )
  }

  start() // on construction
}

private object ApplicationEvolutions {
  import EvolutionsHelper._
  private val logger = Logger(classOf[ApplicationEvolutions])

  val SelectPlayEvolutionsLockSql =
    """
      select lock from ${schema}${evolutions_table}_lock
    """

  val SelectPlayEvolutionsLockMysqlSql =
    """
      select `lock` from ${schema}${evolutions_table}_lock
    """

  val SelectPlayEvolutionsLockOracleSql =
    """
      select "lock" from ${schema}${evolutions_table}_lock
    """

  val CreatePlayEvolutionsLockSql =
    """
      create table ${schema}${evolutions_table}_lock (
        lock int not null primary key
      )
    """

  val CreatePlayEvolutionsLockMysqlSql =
    """
      create table ${schema}${evolutions_table}_lock (
        `lock` int not null primary key
      )
    """

  val CreatePlayEvolutionsLockOracleSql =
    """
      CREATE TABLE ${schema}${evolutions_table}_lock (
        "lock" Number(10,0) Not Null Enable,
        CONSTRAINT ${evolutions_table}_lock_pk PRIMARY KEY ("lock")
      )
    """

  val InsertIntoPlayEvolutionsLockSql =
    """
      insert into ${schema}${evolutions_table}_lock (lock) values (1)
    """

  val InsertIntoPlayEvolutionsLockMysqlSql =
    """
      insert into ${schema}${evolutions_table}_lock (`lock`) values (1)
    """

  val InsertIntoPlayEvolutionsLockOracleSql =
    """
      insert into ${schema}${evolutions_table}_lock ("lock") values (1)
    """

  val lockPlayEvolutionsLockSqls =
    List(
      """
        select lock from ${schema}${evolutions_table}_lock where lock = 1 for update nowait
      """
    )

  val lockPlayEvolutionsLockMysqlSqls =
    List(
      """
        set innodb_lock_wait_timeout = 1
      """,
      """
        select `lock` from ${schema}${evolutions_table}_lock where `lock` = 1 for update
      """
    )

  val lockPlayEvolutionsLockOracleSqls =
    List(
      """
        select "lock" from ${schema}${evolutions_table}_lock where "lock" = 1 for update nowait
      """
    )

  def runEvolutions(
      database: Database,
      config: EvolutionsConfig,
      evolutions: EvolutionsApi,
      reader: EvolutionsReader,
      block: (String, EvolutionsDatasourceConfig, Seq[Script], Boolean) => Unit
  ): Unit = {
    val db       = database.name
    val dbConfig = config.forDatasource(db)
    if (dbConfig.enabled) {
      withLock(database, dbConfig) {
        val scripts   = evolutions.scripts(db, reader, dbConfig.schema, dbConfig.metaTable)
        val hasDown   = scripts.exists(_.isInstanceOf[DownScript])
        val onlyDowns = scripts.forall(_.isInstanceOf[DownScript])

        if (scripts.nonEmpty && !(onlyDowns && dbConfig.skipApplyDownsOnly)) {
          block.apply(db, dbConfig, scripts, hasDown)
        }
      }
    }
  }

  private def withLock(db: Database, dbConfig: EvolutionsDatasourceConfig)(block: => Unit): Unit = {
    if (dbConfig.useLocks) {
      val ds  = db.dataSource
      val url = db.url
      val c   = ds.getConnection
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

  private def createLockTableIfNecessary(
      url: String,
      c: Connection,
      s: Statement,
      dbConfig: EvolutionsDatasourceConfig
  ): Unit = {
    val (selectScript, createScript, insertScript) = url match {
      case OracleJdbcUrl() =>
        (SelectPlayEvolutionsLockOracleSql, CreatePlayEvolutionsLockOracleSql, InsertIntoPlayEvolutionsLockOracleSql)
      case MysqlJdbcUrl(_) =>
        (SelectPlayEvolutionsLockMysqlSql, CreatePlayEvolutionsLockMysqlSql, InsertIntoPlayEvolutionsLockMysqlSql)
      case _ =>
        (SelectPlayEvolutionsLockSql, CreatePlayEvolutionsLockSql, InsertIntoPlayEvolutionsLockSql)
    }
    try {
      val r = s.executeQuery(applyConfig(selectScript, dbConfig))
      r.close()
    } catch {
      case e: SQLException =>
        c.rollback()
        s.execute(applyConfig(createScript, dbConfig))
        s.executeUpdate(applyConfig(insertScript, dbConfig))
    }
  }

  private def lock(
      url: String,
      c: Connection,
      s: Statement,
      dbConfig: EvolutionsDatasourceConfig,
      attempts: Int = 5
  ): Unit = {
    val lockScripts = url match {
      case MysqlJdbcUrl(_) => lockPlayEvolutionsLockMysqlSqls
      case OracleJdbcUrl() => lockPlayEvolutionsLockOracleSqls
      case _               => lockPlayEvolutionsLockSqls
    }
    try {
      for (script <- lockScripts) s.execute(applyConfig(script, dbConfig))
    } catch {
      case e: SQLException =>
        if (attempts == 0) throw e
        else {
          logger.warn(
            "Exception while attempting to lock evolutions (other node probably has lock), sleeping for 1 sec"
          )
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
}

/**
 * Evolutions configuration for a given datasource.
 */
trait EvolutionsDatasourceConfig {
  def enabled: Boolean
  def schema: String
  def metaTable: String
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
    metaTable: String,
    autocommit: Boolean,
    useLocks: Boolean,
    autoApply: Boolean,
    autoApplyDowns: Boolean,
    skipApplyDownsOnly: Boolean
) extends EvolutionsDatasourceConfig

/**
 * Default evolutions configuration.
 */
class DefaultEvolutionsConfig(
    defaultDatasourceConfig: EvolutionsDatasourceConfig,
    datasources: Map[String, EvolutionsDatasourceConfig]
) extends EvolutionsConfig {
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
    def getDeprecated[A: ConfigLoader](
        config: Configuration,
        baseKey: => String,
        path: String,
        deprecated: String
    ): A = {
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
    val enabled            = config.get[Boolean]("enabled")
    val schema             = config.get[String]("schema")
    val metaTable          = config.get[String]("metaTable")
    val autocommit         = getDeprecated[Boolean](config, "play.evolutions", "autocommit", "evolutions.autocommit")
    val useLocks           = getDeprecated[Boolean](config, "play.evolutions", "useLocks", "evolutions.use.locks")
    val autoApply          = config.get[Boolean]("autoApply")
    val autoApplyDowns     = config.get[Boolean]("autoApplyDowns")
    val skipApplyDownsOnly = config.get[Boolean]("skipApplyDownsOnly")

    val defaultConfig = DefaultEvolutionsDatasourceConfig(
      enabled,
      schema,
      metaTable,
      autocommit,
      useLocks,
      autoApply,
      autoApplyDowns,
      skipApplyDownsOnly
    )

    // Load config specific to datasources
    // Since not all the datasources will necessarily appear in the db map, because some will come from deprecated
    // configuration, we create a map of them to the default config, and then override any of them with the ones
    // from db.
    val datasourceConfigMap = datasources.map(_ -> config).toMap ++ config.getPrototypedMap("db", "")

    val datasourceConfig: Map[String, DefaultEvolutionsDatasourceConfig] =
      datasourceConfigMap.map {
        case (datasource, dsConfig) =>
          val enabled    = dsConfig.get[Boolean]("enabled")
          val schema     = dsConfig.get[String]("schema")
          val metaTable  = dsConfig.get[String]("metaTable")
          val autocommit = dsConfig.get[Boolean]("autocommit")
          val useLocks   = dsConfig.get[Boolean]("useLocks")
          val autoApply = getDeprecated[Boolean](
            dsConfig,
            s"play.evolutions.db.$datasource",
            "autoApply",
            s"applyEvolutions.$datasource"
          )
          val autoApplyDowns = getDeprecated[Boolean](
            dsConfig,
            s"play.evolutions.db.$datasource",
            "autoApplyDowns",
            s"applyDownEvolutions.$datasource"
          )
          val skipApplyDownsOnly = getDeprecated[Boolean](
            dsConfig,
            s"play.evolutions.db.$datasource",
            "skipApplyDownsOnly",
            s"skipApplyDownsOnly.$datasource"
          )
          datasource -> DefaultEvolutionsDatasourceConfig(
            enabled,
            schema,
            metaTable,
            autocommit,
            useLocks,
            autoApply,
            autoApplyDowns,
            skipApplyDownsOnly
          )
      }

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
class EvolutionsWebCommands @Inject() (
    dbApi: DBApi,
    evolutions: EvolutionsApi,
    reader: EvolutionsReader,
    config: EvolutionsConfig
) extends HandleWebCommandSupport {
  var checkedAlready = false
  def handleWebCommand(
      request: play.api.mvc.RequestHeader,
      buildLink: play.core.BuildLink,
      path: java.io.File
  ): Option[play.api.mvc.Result] = {
    val applyEvolutions   = """/@evolutions/apply/([a-zA-Z0-9_-]+)""".r
    val resolveEvolutions = """/@evolutions/resolve/([a-zA-Z0-9_-]+)/([0-9]+)""".r

    lazy val redirectUrl = request.queryString.get("redirect").filterNot(_.isEmpty).map(_.head).getOrElse("/")

    // Regex removes all parent directories from request path
    request.path.replaceFirst("^((?!/@evolutions).)*(/@evolutions.*$)", "$2") match {
      case applyEvolutions(db) => {
        val dbConfig = config.forDatasource(db)
        Some {
          val scripts = evolutions.scripts(db, reader, dbConfig.schema, dbConfig.metaTable)
          evolutions.evolve(db, scripts, dbConfig.autocommit, dbConfig.schema, dbConfig.metaTable)
          buildLink.forceReload()
          play.api.mvc.Results.Redirect(redirectUrl)
        }
      }

      case resolveEvolutions(db, rev) => {
        val dbConfig = config.forDatasource(db)
        Some {
          evolutions.resolve(db, rev.toInt, dbConfig.schema, dbConfig.metaTable)
          buildLink.forceReload()
          play.api.mvc.Results.Redirect(redirectUrl)
        }
      }

      case _ => {
        synchronized {
          if (!checkedAlready) {
            dbApi
              .databases()
              .foreach(
                ApplicationEvolutions.runEvolutions(
                  _,
                  config,
                  evolutions,
                  reader,
                  (db, dbConfig, scripts, hasDown) => {
                    import Evolutions.toHumanReadableScript
                    throw InvalidDatabaseRevision(db, toHumanReadableScript(scripts))
                  }
                )
              )
            checkedAlready = true
          }
        }
        None
      }
    }
  }
}

/**
 * Exception thrown when the database is not up to date.
 *
 * @param db the database name
 * @param script the script to be run to resolve the conflict.
 */
case class InvalidDatabaseRevision(db: String, script: String)
    extends PlayException.RichDescription(
      "Database '" + db + "' needs evolution!",
      "An SQL script need to be run on your database."
    ) {
  def subTitle = "This SQL script must be run:"
  def content  = script

  private val javascript =
    """
        window.location = window.location.href.split(/[?#]/)[0].replace(/\/@evolutions.*$|\/$/, '') + '/@evolutions/apply/%s?redirect=' + encodeURIComponent(location)
    """.format(db).trim

  def htmlDescription = {
    <span>An SQL script will be run on your database -</span>
    <input name="evolution-button" type="button" value="Apply this script now!" onclick={javascript}/>
  }.mkString
}
