/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import play.libs.concurrent.HttpExecution;
import scala.compat.java8.FutureConverters;
import scala.concurrent.ExecutionContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Default delegating implementation of the database API.
 */
public class DefaultDatabase implements Database {

    private final play.api.db.Database sdb;
    private final ExecutionContext ec;

    @Deprecated
    public DefaultDatabase(play.api.db.Database database) {
        this(database, HttpExecution.defaultContext());
    }

    public DefaultDatabase(play.api.db.Database database, ExecutionContext executionContext) {
        this.sdb = database;
        this.ec = executionContext;
    }

    /**
     * Create a default BoneCP-backed database.
     *
     * @param name name for the db's underlying datasource
     * @param configuration the database's configuration
     */
    @Deprecated
    public DefaultDatabase(String name, Config configuration) { this(name, configuration, HttpExecution.defaultContext()); }

    /**
     * Create a default BoneCP-backed database.
     *
     * @param name name for the db's underlying datasource
     * @param configuration the database's configuration
     * @param executionContext the executor for asynchronous operations
     */
    public DefaultDatabase(String name, Config configuration, ExecutionContext executionContext) {
        this(new play.api.db.PooledDatabase(name, new play.api.Configuration(
                configuration.withFallback(ConfigFactory.defaultReference().getConfig("play.db.prototype"))
        )), executionContext);
    }

    /**
     * Create a default BoneCP-backed database.
     *
     * @param name name for the db's underlying datasource
     * @param config the db's configuration
     */
    @Deprecated
    public DefaultDatabase(String name, Map<String, ? extends Object> config) {
        this(name, config, HttpExecution.defaultContext());
    }

    /**
     * Create a default BoneCP-backed database.
     *
     * @param name name for the db's underlying datasource
     * @param config the db's configuration
     * @param executionContext the executor for asynchronous operations
     */
    public DefaultDatabase(String name, Map<String, ? extends Object> config, ExecutionContext executionContext) {
        this(new play.api.db.PooledDatabase(name, new play.api.Configuration(
                ConfigFactory.parseMap(config)
                        .withFallback(ConfigFactory.defaultReference().getConfig("play.db.prototype"))
        )), executionContext);
    }

    @Override
    public String getName() {
        return sdb.name();
    }

    @Override
    public DataSource getDataSource() {
        return sdb.dataSource();
    }

    @Override
    public String getUrl() {
        return sdb.url();
    }

    @Override
    public CompletionStage<Connection> getConnectionAsync() {
        return FutureConverters.toJava(sdb.getConnectionAsync());
    }

    @Override
    public Connection getConnection() {
        return sdb.getConnection();
    }

    @Override
    public CompletionStage<Connection> getConnectionAsync(boolean autocommit) {
        return FutureConverters.toJava(sdb.getConnectionAsync(autocommit));
    }

    @Override
    public Connection getConnection(boolean autocommit) {
        return sdb.getConnection(autocommit);
    }

    @Override
    public void withConnection(ConnectionRunnable block) {
        sdb.withConnection((Connection c) -> {
            try {
                block.run(c);
                return null;
            } catch (SQLException e) {
                throw new RuntimeException("Rethrowing checked exception", e);
            }
        });
    }

    @Override
    public <A> A withConnection(ConnectionCallable<A> block) {
        return sdb.withConnection((Connection c) -> {
            try {
                return block.call(c);
            } catch (SQLException e) {
                throw new RuntimeException("Rethrowing checked exception", e);
            }
        });
    }

    @Override
    public <A> CompletionStage<A> withConnectionAsync(AsyncConnectionCallable<A> block) {
        return FutureConverters.toJava(sdb.withConnectionAsync((Connection c) -> {
            try {
                return FutureConverters.toScala(block.call(c));
            } catch (SQLException e) {
                throw new RuntimeException("Rethrowing checked exception", e);
            }
        }, ec));
    }

    @Override
    public void withConnection(boolean autocommit, ConnectionRunnable block) {
        sdb.withConnection(autocommit, (Connection c) -> {
            try {
                block.run(c);
                return null;
            } catch (SQLException e) {
                throw new RuntimeException("Rethrowing checked exception", e);
            }
        });
    }

    @Override
    public <A> A withConnection(boolean autocommit, ConnectionCallable<A> block) {
        return sdb.withConnection(autocommit, (Connection c) -> {
            try {
                return block.call(c);
            } catch (SQLException e) {
                throw new RuntimeException("Rethrowing checked exception", e);
            }
        });
    }

    @Override
    public <A> CompletionStage<A> withConnectionAsync(boolean autocommit, AsyncConnectionCallable<A> block) {
        return FutureConverters.toJava(sdb.withConnectionAsync(autocommit, (Connection c) -> {
            try {
                return FutureConverters.toScala(block.call(c));
            } catch (SQLException e) {
                throw new RuntimeException("Rethrowing checked exception", e);
            }
        }, ec));
    }

    @Override
    public void withTransaction(ConnectionRunnable block) {
        sdb.withTransaction((Connection c) -> {
            try {
                block.run(c);
                return null;
            } catch (SQLException e) {
                throw new RuntimeException("Rethrowing checked exception", e);
            }
        });

    }

    @Override
    public <A> A withTransaction(ConnectionCallable<A> block) {
        return sdb.withTransaction((Connection c) -> {
            try {
                return block.call(c);
            } catch (SQLException e) {
                throw new RuntimeException("Rethrowing checked exception", e);
            }
        });
    }

    @Override
    public <A> CompletionStage<A> withTransactionAsync(AsyncConnectionCallable<A> block) {
        return FutureConverters.toJava(sdb.withTransactionAsync((Connection c) -> {
            try {
                return FutureConverters.toScala(block.call(c));
            } catch (SQLException e) {
                throw new RuntimeException("Rethrowing checked exception", e);
            }
        }, ec));
    }

    @Override
    public void shutdown() {
        sdb.shutdown();
    }

    @Override
    public play.api.db.Database asScala() {
        return sdb;
    }
}
