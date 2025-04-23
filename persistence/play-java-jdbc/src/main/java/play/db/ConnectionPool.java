/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import com.typesafe.config.Config;
import javax.sql.DataSource;
import play.Environment;

/** Connection pool API for managing data sources. */
public interface ConnectionPool {

  /**
   * Create a data source with the given configuration.
   *
   * @param name the database name
   * @param configuration the data source configuration
   * @param environment the database environment
   * @return a data source backed by a connection pool
   */
  DataSource create(String name, Config configuration, Environment environment);

  /**
   * Close the given data source.
   *
   * @param dataSource the data source to close
   */
  void close(DataSource dataSource);

  /**
   * @return the Scala version for this connection pool.
   */
  play.api.db.ConnectionPool asScala();
}
