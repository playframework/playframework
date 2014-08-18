/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import javax.inject.{ Inject, Provider, Singleton }

import scala.util.control.NonFatal

import play.api.{ Configuration, Play }

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
    configuration: Configuration,
    connectionPool: ConnectionPool = new BoneConnectionPool,
    classLoader: ClassLoader = classOf[DefaultDBApi].getClassLoader) extends DBApi {

  lazy val databases: Seq[Database] = {
    configuration.subKeys.toList map { name =>
      val conf = configuration.getConfig(name).getOrElse {
        throw configuration.globalError(s"Missing configuration [db.$name]")
      }
      new PooledDatabase(name, conf, classLoader, connectionPool)
    }
  }

  private lazy val databaseByName: Map[String, Database] = {
    databases.map(db => (db.name, db)).toMap
  }

  def database(name: String): Database = {
    databaseByName.get(name).getOrElse {
      throw configuration.globalError(s"Could not find database for $name")
    }
  }

  /**
   * Try to connect to all data sources.
   */
  def connect(logConnection: Boolean = false): Unit = {
    databases foreach { db =>
      try {
        db.getConnection().close()
        if (logConnection) Play.logger.info(s"Database [${db.name}] connected at ${db.url}")
      } catch {
        case NonFatal(e) =>
          throw configuration.reportError(s"${db.name}.url", s"Cannot connect to database [${db.name}]", Some(e))
      }
    }
  }

  def shutdown(): Unit = {
    databases foreach (_.shutdown())
  }

}
