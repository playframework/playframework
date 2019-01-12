/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db

import com.typesafe.config.Config
import play.api.inject.{ Injector, NewInstanceInjector }
import play.api.{ Configuration, Environment, Logger }

import scala.util.control.NonFatal

/**
 * Default implementation of the DB API.
 */
class DefaultDBApi(
    configuration: Map[String, Config],
    defaultConnectionPool: ConnectionPool = new HikariCPConnectionPool(Environment.simple()),
    environment: Environment = Environment.simple(),
    injector: Injector = NewInstanceInjector) extends DBApi {

  import DefaultDBApi._

  lazy val databases: Seq[Database] = {
    configuration.map {
      case (name, config) =>
        val pool = ConnectionPool.fromConfig(config.getString("pool"), injector, environment, defaultConnectionPool)
        new PooledDatabase(name, config, environment, pool)
    }.toSeq
  }

  private lazy val databaseByName: Map[String, Database] =
    databases.map(db => (db.name, db))(scala.collection.breakOut)

  def database(name: String): Database = {
    databaseByName.getOrElse(name, throw new IllegalArgumentException(s"Could not find database for $name"))
  }

  /**
   * Try to connect to all data sources.
   */
  @deprecated("Use initialize instead, which does not try to connect to the database", "2.7.0")
  def connect(logConnection: Boolean = false): Unit = {
    databases foreach { db =>
      try {
        db.getConnection().close()
        if (logConnection) logger.info(s"Database [${db.name}] connected at ${db.url}")
      } catch {
        case NonFatal(e) =>
          throw Configuration(configuration(db.name)).reportError("url", s"Cannot connect to database [${db.name}]", Some(e))
      }
    }
  }

  /**
   * Try to initialize all the configured databases. This ensures that the configurations will be checked, but the application
   * initialization will not be affected if one of the databases is offline.
   *
   * @param logInitialization if we need to log all the database initialization.
   */
  def initialize(logInitialization: Boolean): Unit = {
    // Accessing the dataSource for the database makes the connection pool to
    // initialize. We will then be able to check for configuration errors.
    databases.foreach { db =>
      try {
        if (logInitialization) logger.info(s"Database [${db.name}] initialized at ${db.url}")
        // Calling db.dataSource forces the underlying pool to initialize
        db.dataSource
      } catch {
        case NonFatal(e) =>
          throw Configuration(configuration(db.name)).reportError("url", s"Cannot initialize to database [${db.name}]", Some(e))
      }
    }
  }

  def shutdown(): Unit = databases.foreach(_.shutdown())
}

object DefaultDBApi {
  private val logger = Logger(classOf[DefaultDBApi])
}
