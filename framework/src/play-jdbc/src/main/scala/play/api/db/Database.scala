/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import java.sql.{ Connection, Driver, DriverManager }
import javax.sql.DataSource

import com.typesafe.config.Config

import scala.util.control.{ NonFatal, ControlThrowable }

import play.api.{ Environment, PlayConfig, Configuration }
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
   * Create a pooled database named "default" with the given driver and url.
   *
   * @param driver the database driver class
   * @param url the database url
   * @param name the database name
   * @param config a map of extra database configuration
   * @return a configured database
   */
  def apply(driver: String, url: String, name: String = "default", config: Map[String, _ <: Any] = Map.empty): Database = {
    val dbConfig = Configuration.reference.getConfig("play.db.prototype").get ++
      Configuration.from(Map("driver" -> driver, "url" -> url) ++ config)
    new PooledDatabase(name, dbConfig)
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
    Database(driver, url, name, config)
  }

  /**
   * Run the given block with a database, cleaning up afterwards.
   *
   * @param driver the database driver class
   * @param url the database url
   * @param name the database name
   * @param config a map of extra database configuration
   * @param block The block of code to run
   * @return The result of the block
   */
  def withDatabase[T](driver: String, url: String, name: String = "default",
    config: Map[String, _ <: Any] = Map.empty)(block: Database => T): T = {
    val database = Database(driver, url, name, config)
    try {
      block(database)
    } finally {
      database.shutdown()
    }
  }

  /**
   * Run the given block with an in-memory h2 database, cleaning up afterwards.
   *
   * @param name the database name (defaults to "default")
   * @param urlOptions a map of extra url options
   * @param config a map of extra database configuration
   * @param block The block of code to run
   * @return The result of the block
   */
  def withInMemory[T](name: String = "default", urlOptions: Map[String, String] = Map.empty,
    config: Map[String, _ <: Any] = Map.empty)(block: Database => T): T = {
    val database = inMemory(name, urlOptions, config)
    try {
      block(database)
    } finally {
      database.shutdown()
    }
  }
}

/**
 * Default implementation of the database API.
 * Provides driver registration and connection methods.
 */
abstract class DefaultDatabase(val name: String, configuration: Config, environment: Environment) extends Database {

  private val config = PlayConfig(configuration)
  val databaseConfig = DatabaseConfig.fromConfig(config, environment)

  // abstract methods to be implemented

  def createDataSource(): DataSource

  def closeDataSource(dataSource: DataSource): Unit

  // driver registration

  lazy val driver: Option[Driver] = {
    databaseConfig.driver.map { driverClassName =>
      try {
        val proxyDriver = new ProxyDriver(Reflect.createInstance[Driver](driverClassName, environment.classLoader))
        DriverManager.registerDriver(proxyDriver)
        proxyDriver
      } catch {
        case NonFatal(e) => throw config.reportError("driver", s"Driver not found: [$driverClassName}]", Some(e))
      }
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
    driver.foreach(DriverManager.deregisterDriver)
  }

}

/**
 * Default implementation of the database API using a connection pool.
 */
class PooledDatabase(name: String, configuration: Config, environment: Environment, pool: ConnectionPool)
    extends DefaultDatabase(name, configuration, environment) {

  def this(name: String, configuration: Configuration) = this(name, configuration.underlying, Environment.simple(), new HikariCPConnectionPool(Environment.simple()))

  def createDataSource(): DataSource = pool.create(name, databaseConfig, configuration)

  def closeDataSource(dataSource: DataSource): Unit = pool.close(dataSource)

}
