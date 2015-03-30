/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import javax.sql.DataSource;

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
    public DataSource create(String name, Configuration configuration, Environment environment);

    /**
     * Close the given data source.
     *
     * @param dataSource the data source to close
     */
    public void close(DataSource dataSource);

}
