/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import java.sql.{ Connection, Driver, DriverManager }
import javax.sql.DataSource

import scala.util.control.{ NonFatal, ControlThrowable }

import play.api.Configuration
import play.utils.{ ProxyDriver, Reflect }

/**
 * Database API.
 */
trait Database {

  /**
   * The configuration name for this database.
   */
  def name: String

  /**
   * The underlying JDBC data source for this database.
   */
  def dataSource: DataSource

  /**
   * The JDBC connection URL this database, i.e. `jdbc:...`
   * Normally retrieved via a connection.
   */
  def url: String

  /**
   * Get a JDBC connection from the underlying data source.
   * Autocommit is enabled by default.
   *
   * Don't forget to release the connection at some point by calling close().
   *
   * @param autocommit determines whether to autocommit the connection
   * @return a JDBC connection
   */
  def getConnection(): Connection

  /**
   * Get a JDBC connection from the underlying data source.
   *
   * Don't forget to release the connection at some point by calling close().
   *
   * @param autocommit determines whether to autocommit the connection
   * @return a JDBC connection
   */
  def getConnection(autocommit: Boolean): Connection

  /**
   * Execute a block of code, providing a JDBC connection.
   * The connection and all created statements are automatically released.
   *
   * @param block code to execute
   * @return the result of the code block
   */
  def withConnection[A](block: Connection => A): A

  /**
   * Execute a block of code, providing a JDBC connection.
   * The connection and all created statements are automatically released.
   *
   * @param autocommit determines whether to autocommit the connection
   * @param block code to execute
   * @return the result of the code block
   */
  def withConnection[A](autocommit: Boolean)(block: Connection => A): A

  /**
   * Execute a block of code in the scope of a JDBC transaction.
   * The connection and all created statements are automatically released.
   * The transaction is automatically committed, unless an exception occurs.
   *
   * @param block code to execute
   * @return the result of the code block
   */
  def withTransaction[A](block: Connection => A): A

  /**
   * Shutdown this database, closing the underlying data source.
   */
  def shutdown(): Unit

}

/**
 * Creation helpers for manually instantiating databases.
 */
object Database {

  /**
   * Create a pooled database with the given configuration.
   *
   * @param name the database name
   * @param driver the database driver class
   * @param url the database url
   * @param config a map of extra database configuration
   * @return a configured database
   */
  def apply(name: String, driver: String, url: String, config: Map[String, _ <: Any] = Map.empty): Database = {
    val dbConfig = Configuration.from(Map("driver" -> driver, "url" -> url) ++ config)
    new PooledDatabase(name, dbConfig)
  }

  /**
   * Create a pooled database named "default" with the given configuration.
   *
   * @param driver the database driver class
   * @param url the database url
   * @param config a map of extra database configuration
   * @return a configured database
   */
  def apply(driver: String, url: String, config: Map[String, _ <: Any]): Database = {
    Database("default", driver, url, config)
  }

  /**
   * Create a pooled database named "default" with the given driver and url.
   *
   * @param driver the database driver class
   * @param url the database url
   * @return a configured database
   */
  def apply(driver: String, url: String): Database = {
    Database("default", driver, url, Map.empty)
  }

  /**
   * Create an in-memory H2 database.
   *
   * @param name the database name (defaults to "default")
   * @param urlOptions a map of extra url options
   * @param config a map of extra database configuration
   * @return a configured in-memory h2 database
   */
  def inMemory(name: String = "default", urlOptions: Map[String, String] = Map.empty, config: Map[String, _ <: Any] = Map.empty): Database = {
    val driver = "org.h2.Driver"
    val urlExtra = urlOptions.map { case (k, v) => k + "=" + v }.mkString(";", ";", "")
    val url = "jdbc:h2:mem:" + name + urlExtra
    Database(name, driver, url, config)
  }

}

/**
 * Default implementation of the database API.
 * Provides driver registration and connection methods.
 */
abstract class DefaultDatabase(val name: String, configuration: Configuration, classLoader: ClassLoader) extends Database {

  // abstract methods to be implemented

  def createDataSource(): DataSource

  def closeDataSource(dataSource: DataSource): Unit

  // driver registration

  lazy val driver: Driver = {
    val driverClass = configuration.getString("driver").getOrElse {
      throw configuration.reportError(name, s"Missing configuration [db.$name.driver]")
    }
    try {
      val proxyDriver = new ProxyDriver(Reflect.createInstance[Driver](driverClass, classLoader))
      DriverManager.registerDriver(proxyDriver)
      proxyDriver
    } catch {
      case NonFatal(e) => throw configuration.reportError("driver", s"Driver not found: [$driverClass]", Some(e))
    }
  }

  // lazy data source creation

  lazy val dataSource: DataSource = {
    driver // trigger driver registration
    createDataSource
  }

  lazy val url: String = {
    val connection = dataSource.getConnection
    try {
      connection.getMetaData.getURL
    } finally {
      connection.close()
    }
  }

  // connection methods

  def getConnection(): Connection = {
    getConnection(autocommit = true)
  }

  def getConnection(autocommit: Boolean): Connection = {
    val connection = dataSource.getConnection
    connection.setAutoCommit(autocommit)
    connection
  }

  def withConnection[A](block: Connection => A): A = {
    withConnection(autocommit = true)(block)
  }

  def withConnection[A](autocommit: Boolean)(block: Connection => A): A = {
    val connection = getConnection(autocommit)
    try {
      block(connection)
    } finally {
      connection.close()
    }
  }

  def withTransaction[A](block: Connection => A): A = {
    withConnection(autocommit = false) { connection =>
      try {
        val r = block(connection)
        connection.commit()
        r
      } catch {
        case e: ControlThrowable =>
          connection.commit()
          throw e
        case e: Throwable =>
          connection.rollback()
          throw e
      }
    }
  }

  // shutdown

  def shutdown(): Unit = {
    closeDataSource(dataSource)
    deregisterDriver()
  }

  def deregisterDriver(): Unit = {
    DriverManager.deregisterDriver(driver)
  }

}

/**
 * Default implementation of the database API using a connection pool.
 */
class PooledDatabase(name: String, configuration: Configuration, classLoader: ClassLoader, pool: ConnectionPool)
    extends DefaultDatabase(name, configuration, classLoader) {

  def this(name: String, configuration: Configuration) = this(name, configuration, classOf[PooledDatabase].getClassLoader, new BoneConnectionPool)

  def createDataSource(): DataSource = pool.create(name, configuration, classLoader)

  def closeDataSource(dataSource: DataSource): Unit = pool.close(dataSource)

}
