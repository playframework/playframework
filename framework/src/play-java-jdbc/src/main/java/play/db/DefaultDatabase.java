/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import java.sql.Connection;
import java.util.Map;
import javax.sql.DataSource;

import com.typesafe.config.Config;

import com.typesafe.config.ConfigFactory;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

/**
 * Default delegating implementation of the database API.
 */
public class DefaultDatabase implements Database {

    private final play.api.db.Database db;

    public DefaultDatabase(play.api.db.Database database) {
        this.db = database;
    }

    /**
     * Create a default HikariCP-backed database.
     *
     * @param name name for the db's underlying datasource
     * @param configuration the database's configuration
     */
    public DefaultDatabase(String name, Config configuration) {
        this(new play.api.db.PooledDatabase(name, new play.api.Configuration(
                configuration.withFallback(ConfigFactory.defaultReference().getConfig("play.db.prototype"))
        )));
    }

    /**
     * Create a default HikariCP-backed database.
     *
     * @param name name for the db's underlying datasource
     * @param config the db's configuration
     */
    public DefaultDatabase(String name, Map<String, ? extends Object> config) {
        this(new play.api.db.PooledDatabase(name, new play.api.Configuration(
                ConfigFactory.parseMap(config)
                        .withFallback(ConfigFactory.defaultReference().getConfig("play.db.prototype"))
        )));
    }

    @Override
    public String getName() {
        return db.name();
    }

    @Override
    public DataSource getDataSource() {
        return db.dataSource();
    }

    @Override
    public String getUrl() {
        return db.url();
    }

    @Override
    public Connection getConnection() {
        return db.getConnection();
    }

    @Override
    public Connection getConnection(boolean autocommit) {
        return db.getConnection(autocommit);
    }

    @Override
    public void withConnection(ConnectionRunnable block) {
        db.withConnection(connectionFunction(block));
    }

    @Override
    public <A> A withConnection(ConnectionCallable<A> block) {
        return db.withConnection(connectionFunction(block));
    }

    @Override
    public void withConnection(boolean autocommit, ConnectionRunnable block) {
        db.withConnection(autocommit, connectionFunction(block));
    }

    @Override
    public <A> A withConnection(boolean autocommit, ConnectionCallable<A> block) {
        return db.withConnection(autocommit, connectionFunction(block));
    }

    @Override
    public void withTransaction(ConnectionRunnable block) {
        db.withTransaction(connectionFunction(block));
    }

    @Override
    public <A> A withTransaction(ConnectionCallable<A> block) {
        return db.withTransaction(connectionFunction(block));
    }

    @Override
    public void shutdown() {
        db.shutdown();
    }

    @Override
    public play.api.db.Database toScala() {
        return db;
    }


    /**
     * Create a Scala function wrapper for ConnectionRunnable.
     *
     * @param block a Java functional interface instance to wrap
     * @return a scala function that wraps the given block
     */
    AbstractFunction1<Connection, BoxedUnit> connectionFunction(final ConnectionRunnable block) {
        return new AbstractFunction1<Connection, BoxedUnit>() {
            public BoxedUnit apply(Connection connection) {
                try {
                    block.run(connection);
                    return BoxedUnit.UNIT;
                } catch (java.sql.SQLException e) {
                    throw new RuntimeException("Connection runnable failed", e);
                }
            }
        };
    }

    /**
     * Create a Scala function wrapper for ConnectionCallable.
     *
     * @param block a Java functional interface instance to wrap
     * @param <A> the provided block's return type
     * @return a scala function wrapping the given block
     */
    <A> AbstractFunction1<Connection, A> connectionFunction(final ConnectionCallable<A> block) {
        return new AbstractFunction1<Connection, A>() {
            public A apply(Connection connection) {
                try {
                    return block.call(connection);
                } catch (java.sql.SQLException e) {
                    throw new RuntimeException("Connection callable failed", e);
                }
            }
        };
    }

}
