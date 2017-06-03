/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.db

import java.sql.Connection
import java.util.concurrent.{ BlockingQueue, LinkedBlockingQueue, TimeUnit }
import javax.sql.DataSource

import com.typesafe.config.Config
import org.jdbcdslog.{ ConnectionLogger, ConnectionLoggingProxy, LogSqlDataSource }
import play.api.{ Environment, Mode }
import play.api.inject.Injector
import play.db
import play.db.DefaultConnectionPool
import play.utils.Reflect

import scala.annotation.tailrec
import scala.concurrent.{ Future, Promise }
import scala.util.Try

/**
 * Connection pool API for managing data sources.
 *
 * Subclasses should override the `createAsync` method. The `create` and
 * `close` methods are not called externally.
 *
 * A simple implementation of `createAsync` is:
 * <pre>
 * val ds: DataSource with Closeable = // create a DataSource
 * AsyncDataSource.wrap(ds, ds)
 * </pre>
 */
@deprecated("Use AsyncConnectionPool instead.", "2.6.0")
trait ConnectionPool {

  /**
   * Create a data source with the given configuration.
   *
   * @param name the database name
   * @param configuration the data source configuration
   * @return a data source backed by a connection pool
   */
  @deprecated("Use AsyncConnectionPool instead.", "2.6.0")
  def create(name: String, dbConfig: DatabaseConfig, configuration: Config): DataSource

  /**
   * Close the given data source.
   *
   * @param dataSource the data source to close
   */
  @deprecated("Use AsyncConnectionPool instead.", "2.6.0")
  def close(dataSource: DataSource): Unit

  def asJava: play.db.ConnectionPool = new DefaultConnectionPool(this)

}