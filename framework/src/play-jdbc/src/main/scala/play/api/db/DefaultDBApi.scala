/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db

import com.typesafe.config.Config
import play.api.inject.{ Injector, NewInstanceInjector }
import play.api.{ Environment, Logger }

/**
 * Default implementation of the DB API.
 */
class DefaultDBApi(
    configuration: Map[String, Config],
    defaultConnectionPool: ConnectionPool = new HikariCPConnectionPool(Environment.simple()),
    environment: Environment = Environment.simple(),
    injector: Injector = NewInstanceInjector) extends DBApi {

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

  def initialize(logInitialization: Boolean): Unit = {
    // Accessing the dataSource for the database makes the connection pool to
    // initialize. We will then be able to check for configuration errors.
    databases.foreach(_.dataSource)
  }

  def shutdown(): Unit = databases.foreach(_.shutdown())
}

object DefaultDBApi {
  private val logger = Logger(classOf[DefaultDBApi])
}
