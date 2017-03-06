/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db;

import com.typesafe.config.Config;
import play.Environment;
import play.api.Configuration$;
import play.api.db.DatabaseConfig;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

/**
 * A {@link ConnectionPool} that delegates to a Scala implementation.
 *
 * @deprecated Use an AsyncConnectionPool instead. To get a Java implementation call asJava on a Scala AsyncConnectionPool.
 */
@Deprecated
@Singleton
public class DefaultConnectionPool implements ConnectionPool {

    /** The Scala implementation that we're delegating to. */
    private final play.api.db.ConnectionPool scp;

    @Inject
    public DefaultConnectionPool(play.api.db.ConnectionPool scp) {
        this.scp = scp;
    }

    @Override
    public DataSource create(String name, Config configuration, Environment environment) {
        DatabaseConfig dbConfig = DatabaseConfig.fromConfig(Configuration$.MODULE$.apply(configuration), environment.asScala());
        return scp.create(name, dbConfig, configuration);
    }

    @Override
    public void close(DataSource dataSource) {
        scp.close(dataSource);
    }

    @Override
    public play.api.db.ConnectionPool asScala() {
        return scp;
    }

}

