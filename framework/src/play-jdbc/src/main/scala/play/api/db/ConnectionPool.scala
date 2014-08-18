/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import javax.sql.DataSource

import play.api.Configuration

/**
 * Connection pool API for managing data sources.
 */
trait ConnectionPool {

  /**
   * Create a data source with the given configuration.
   *
   * @param name the database name
   * @param configuration the data source configuration
   * @param classLoader the database class loader
   * @return a data source backed by a connection pool
   */
  def create(name: String, configuration: Configuration, classLoader: ClassLoader): DataSource

  /**
   * Close the given data source.
   *
   * @param dataSource the data source to close
   */
  def close(dataSource: DataSource): Unit

}
