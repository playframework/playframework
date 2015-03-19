/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import com.typesafe.config.Config
import play.api.inject.{ NewInstanceInjector, Injector }

import scala.util.control.NonFatal

import play.api.{ Environment, Configuration, Logger }

/**
 * DB API for managing application databases.
 */
trait DBApi {

  /**
   * All configured databases.
   */
  def databases(): Seq[Database]

  /**
   * Get database with given configuration name.
   *
   * @param name the configuration name of the database
   */
  def database(name: String): Database

  /**
   * Shutdown all databases, releasing resources.
   */
  def shutdown(): Unit

}

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

  private lazy val databaseByName: Map[String, Database] = {
    databases.map(db => (db.name, db)).toMap
  }

  def database(name: String): Database = {
    databaseByName.getOrElse(name, throw new IllegalArgumentException(s"Could not find database for $name"))
  }

  /**
   * Try to connect to all data sources.
   */
  def connect(logConnection: Boolean = false): Unit = {
    databases foreach { db =>
      try {
        db.getConnection.close()
        if (logConnection) logger.info(s"Database [${db.name}] connected at ${db.url}")
      } catch {
        case NonFatal(e) =>
          throw Configuration(configuration(db.name)).reportError("url", s"Cannot connect to database [${db.name}]", Some(e))
      }
    }
  }

  def shutdown(): Unit = {
    databases foreach (_.shutdown())
  }

}

object DefaultDBApi {
  private val logger = Logger(classOf[DefaultDBApi])
}
