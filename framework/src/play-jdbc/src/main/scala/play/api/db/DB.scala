/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import java.sql.Connection
import javax.sql.DataSource

import play.api.Application

/**
 * Provides a high-level API for getting JDBC connections.
 *
 * For example:
 * {{{
 * val conn = DB.getConnection("customers")
 * }}}
 */
object DB {

  private val dbCache = Application.instanceCache[DBApi]
  private def db(implicit app: Application): DBApi = dbCache(app)

  /**
   * Retrieves a JDBC connection.
   *
   * @param name data source name
   * @param autocommit when `true`, sets this connection to auto-commit
   * @return a JDBC connection
   */
  def getConnection(name: String = "default", autocommit: Boolean = true)(implicit app: Application): Connection =
    db.database(name).getConnection(autocommit)

  /**
   * Retrieves a JDBC connection (autocommit is set to true).
   *
   * @param name data source name
   * @return a JDBC connection
   */
  def getDataSource(name: String = "default")(implicit app: Application): DataSource =
    db.database(name).dataSource

  /**
   * Execute a block of code, providing a JDBC connection. The connection is
   * automatically released.
   *
   * @param name The datasource name.
   * @param autocommit when `true`, sets this connection to auto-commit
   * @param block Code block to execute.
   */
  def withConnection[A](name: String = "default", autocommit: Boolean = true)(block: Connection => A)(implicit app: Application): A =
    db.database(name).withConnection(autocommit)(block)

  /**
   * Execute a block of code, providing a JDBC connection. The connection and all created statements are
   * automatically released.
   *
   * @param block Code block to execute.
   */
  def withConnection[A](block: Connection => A)(implicit app: Application): A =
    db.database("default").withConnection(block)

  /**
   * Execute a block of code, in the scope of a JDBC transaction.
   * The connection and all created statements are automatically released.
   * The transaction is automatically committed, unless an exception occurs.
   *
   * @param name The datasource name.
   * @param block Code block to execute.
   */
  def withTransaction[A](name: String = "default")(block: Connection => A)(implicit app: Application): A =
    db.database(name).withTransaction(block)

  /**
   * Execute a block of code, in the scope of a JDBC transaction.
   * The connection and all created statements are automatically released.
   * The transaction is automatically committed, unless an exception occurs.
   *
   * @param block Code block to execute.
   */
  def withTransaction[A](block: Connection => A)(implicit app: Application): A =
    db.database("default").withTransaction(block)

}
