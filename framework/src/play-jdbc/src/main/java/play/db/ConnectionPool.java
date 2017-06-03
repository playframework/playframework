/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db;

import com.typesafe.config.Config;
import play.Environment;
import play.api.Configuration$;
import play.api.db.DatabaseConfig;

import javax.sql.DataSource;

/**
 * Connection pool API for managing data sources.
 *
 * @deprecated Use an AsyncConnectionPool instead.
 */
@Deprecated
public interface ConnectionPool {

    /**
     * Create a data source with the given configuration.
     *
     * @param name the database name
     * @param configuration the data source configuration
     * @param environment the database environment
     * @return a data source backed by a connection pool
     * @deprecated Use an AsyncConnectionPool instead.
     */
    @Deprecated
    DataSource create(String name, Config configuration, Environment environment);

    /**
     * Close the given data source.
     *
     * @param dataSource the data source to close
     * @deprecated Use an AsyncConnectionPool instead.
     */
    @Deprecated
    void close(DataSource dataSource);

    /**
     * @return the Scala version for this connection pool.
     */
    play.api.db.ConnectionPool asScala();

}
