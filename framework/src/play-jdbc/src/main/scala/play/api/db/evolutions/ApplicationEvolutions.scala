/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db.evolutions

import java.sql.{ Statement, Connection, SQLException }
import javax.inject.{ Inject, Provider, Singleton }
import javax.sql.DataSource

import scala.util.control.Exception.ignoring

import play.api.db.DBApi
import play.api.{ Configuration, Environment, Mode, Play, PlayException }
import play.core.{ HandleWebCommandSupport, WebCommands }

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

  /**
   * Checks the evolutions state. Called on construction.
   */
  def start(): Unit = {
    import Evolutions.toHumanReadableScript

    webCommands.addHandler(new EvolutionsWebCommands(evolutions, reader, config))

    // allow db modules to write evolution files
    dynamicEvolutions.create()

    dbApi.databases.foreach { database =>
      withLock(database.dataSource) {
        val db = database.name
        val scripts = evolutions.scripts(db, reader)
        val hasDown = scripts.exists(_.isInstanceOf[DownScript])

        val autocommit = config.autocommit

        lazy val applyEvolutions = config.applyEvolutions(db)
        lazy val applyDownEvolutions = config.applyDownEvolutions(db)

        if (!scripts.isEmpty) {
          environment.mode match {
            case Mode.Test => evolutions.evolve(db, scripts, autocommit)
            case Mode.Dev if applyEvolutions => evolutions.evolve(db, scripts, autocommit)
            case Mode.Prod if !hasDown && applyEvolutions => evolutions.evolve(db, scripts, autocommit)
            case Mode.Prod if hasDown && applyEvolutions && applyDownEvolutions => evolutions.evolve(db, scripts, autocommit)
            case Mode.Prod if hasDown => {
              Play.logger.warn("Your production database [" + db + "] needs evolutions, including downs! \n\n" + toHumanReadableScript(scripts))
              Play.logger.warn("Run with -DapplyEvolutions." + db + "=true and -DapplyDownEvolutions." + db + "=true if you want to run them automatically, including downs (be careful, especially if your down evolutions drop existing data)")

              throw InvalidDatabaseRevision(db, toHumanReadableScript(scripts))
            }
            case Mode.Prod => {
              Play.logger.warn("Your production database [" + db + "] needs evolutions! \n\n" + toHumanReadableScript(scripts))
              Play.logger.warn("Run with -DapplyEvolutions." + db + "=true if you want to run them automatically (be careful)")

              throw InvalidDatabaseRevision(db, toHumanReadableScript(scripts))
            }
            case _ => throw InvalidDatabaseRevision(db, toHumanReadableScript(scripts))
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

  start() // on construction
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

/**
 * Default evolutions configuration.
 */
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
@Singleton
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

    lazy val redirectUrl = request.queryString.get("redirect").filterNot(_.isEmpty).map(_(0)).getOrElse("/")

    request.path match {

      case applyEvolutions(db) => {
        Some {
          val scripts = evolutions.scripts(db, reader)
          evolutions.evolve(db, scripts, config.autocommit)
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
