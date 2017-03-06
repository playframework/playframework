/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.db

import java.sql.{ Connection, Driver, DriverManager }
import java.util.concurrent.{ BlockingQueue, LinkedBlockingQueue, TimeUnit }
import javax.sql.DataSource

import com.typesafe.config.Config
import play.api.{ Configuration, Environment }
import play.core.Execution
import play.utils.{ ProxyDriver, Reflect }

import scala.annotation.tailrec
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.control.{ ControlThrowable, NonFatal }
import scala.util.{ Failure, Success, Try }

/**
 * Creation helpers for manually instantiating databases.
 */
object Databases {

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
    val dbConfig = Configuration.reference.get[Configuration]("play.db.prototype") ++
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
    Databases(driver, url, name, config)
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
    val database = Databases(driver, url, name, config)
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

  private val config = Configuration(configuration)
  val databaseConfig = DatabaseConfig.fromConfig(config, environment)

  // abstract methods to be implemented

  protected def createAsyncDataSource(): AsyncDataSource

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

  override lazy val dataSource: AsyncDataSource = {
    driver // trigger driver registration
    createAsyncDataSource()
  }

  lazy val url: String = {
    val connection = dataSource.getConnection
    try {
      connection.getMetaData.getURL
    } finally {
      connection.close()
    }
  }

  // c methods

  override def getConnectionAsync(): Future[Connection] = {
    getConnectionAsync(autocommit = true)
  }

  override def getConnection(): Connection = {
    getConnection(autocommit = true)
  }

  override def getConnectionAsync(autocommit: Boolean): Future[Connection] = {
    dataSource.getConnectionAsync.map { connection =>
      connection.setAutoCommit(autocommit)
      connection
    }(Execution.trampoline)
  }

  override def getConnection(autocommit: Boolean): Connection = {
    val connection = dataSource.getConnection
    connection.setAutoCommit(autocommit)
    connection
  }

  override def withConnectionAsync[A](block: Connection => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    withConnectionAsync(autocommit = true)(block)
  }

  override def withConnection[A](block: Connection => A): A = {
    withConnection(autocommit = true)(block)
  }

  override def withConnectionAsync[A](autocommit: Boolean)(block: Connection => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    getConnectionAsync(autocommit).flatMap { connection =>
      block(connection).transform { result: Try[A] =>
        connection.close()
        result
      }(Execution.trampoline)
    }
  }

  override def withConnection[A](autocommit: Boolean)(block: Connection => A): A = {
    val connection = getConnection(autocommit)
    try {
      block(connection)
    } finally {
      connection.close()
    }
  }

  override def withTransactionAsync[A](block: Connection => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    getConnectionAsync(autocommit = false).flatMap { connection =>
      block(connection).transform {
        case f @ Failure(_: ControlThrowable) =>
          connection.commit()
          f
        case f @ Failure(_) =>
          connection.rollback()
          f
        case s @ Success(_) =>
          connection.commit()
          s
      }(Execution.trampoline)
    }
  }

  override def withTransaction[A](block: Connection => A): A = {
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

  override def shutdown(): Unit = {
    dataSource.close()
    driver.foreach(DriverManager.deregisterDriver)
  }

}

/**
 * Default implementation of the database API using a c pool.
 */
class PooledDatabase(name: String, configuration: Config, environment: Environment, pool: AsyncConnectionPool)
    extends DefaultDatabase(name, configuration, environment) {

  def this(name: String, configuration: Configuration) = this(name, configuration.underlying, Environment.simple(), new HikariCPConnectionPool(Environment.simple()))

  override protected def createAsyncDataSource(): AsyncDataSource = {
    pool.createAsync(name, databaseConfig, configuration)
  }

}
