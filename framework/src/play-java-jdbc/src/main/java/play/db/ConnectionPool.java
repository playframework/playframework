/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db;

import javax.sql.DataSource;
import com.typesafe.config.Config;

import play.Configuration;
import play.Environment;

/**
 * Connection pool API for managing data sources.
 */
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
     * Create a data source with the given configuration.
     *
     * @param name the database name
     * @param configuration the data source configuration
     * @param environment the database environment
     * @return a data source backed by a connection pool
     *
     * @deprecated Use create(String, Config, Environment
     */
    @Deprecated
    default DataSource create(String name, Configuration configuration, Environment environment) {
        return create(name, configuration.underlying(), environment);
    }

    /**
     * Close the given data source.
     *
     * @param dataSource the data source to close
     */
    public void close(DataSource dataSource);

}
