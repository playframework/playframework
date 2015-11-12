/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.sql.DataSource;

import play.Configuration;
import play.Environment;
import play.api.PlayConfig;

/**
 * Default delegating implementation of the connection pool API.
 */
@Singleton
public class DefaultConnectionPool implements ConnectionPool {

    private final play.api.db.ConnectionPool cp;
    private final Provider<play.api.db.DatabaseConfig> dcp;

    @Inject
    public DefaultConnectionPool(play.api.db.ConnectionPool connectionPool,
                                 Provider<play.api.db.DatabaseConfig> databaseConfigProvider) {
        this.cp = connectionPool;
        this.dcp = databaseConfigProvider;
    }

    public DataSource create(String name, Configuration configuration, Environment environment) {
        PlayConfig config = new PlayConfig(configuration.getWrappedConfiguration().underlying());
        return cp.create(name, dcp.get(), config.underlying());
    }

    public void close(DataSource dataSource) {
        cp.close(dataSource);
    }

}
