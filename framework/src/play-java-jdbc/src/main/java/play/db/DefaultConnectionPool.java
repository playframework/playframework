/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

import play.Configuration;

/**
 * Default delegating implementation of the connection pool API.
 */
@Singleton
public class DefaultConnectionPool implements ConnectionPool {

    private final play.api.db.ConnectionPool cp;

    @Inject
    public DefaultConnectionPool(play.api.db.ConnectionPool connectionPool) {
        this.cp = connectionPool;
    }

    public DataSource create(String name, Configuration configuration, ClassLoader classLoader) {
        return cp.create(name, configuration.getWrappedConfiguration(), classLoader);
    }

    public void close(DataSource dataSource) {
        cp.close(dataSource);
    }

}
