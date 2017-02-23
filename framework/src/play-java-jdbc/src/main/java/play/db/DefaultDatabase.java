/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db;

import java.sql.Connection;
import java.util.Map;
import javax.sql.DataSource;

import play.Configuration;

import com.typesafe.config.ConfigFactory;

/**
 * Default delegating implementation of the database API.
 */
public class DefaultDatabase implements Database {

    private final play.api.db.Database db;

    public DefaultDatabase(play.api.db.Database database) {
        this.db = database;
    }

    /**
     * Create a default BoneCP-backed database.
     *
     * @param name name for the db's underlying datasource
     * @param configuration the database's configuration
     */
    public DefaultDatabase(String name, Configuration configuration) {
        this(new play.api.db.PooledDatabase(name, new play.api.Configuration(
                configuration.underlying()
                        .withFallback(ConfigFactory.defaultReference().getConfig("play.db.prototype"))
        )));
    }

    /**
     * Create a default BoneCP-backed database.
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
        db.withConnection(DB.connectionFunction(block));
    }

    @Override
    public <A> A withConnection(ConnectionCallable<A> block) {
        return db.withConnection(DB.connectionFunction(block));
    }

    @Override
    public void withConnection(boolean autocommit, ConnectionRunnable block) {
        db.withConnection(autocommit, DB.connectionFunction(block));
    }

    @Override
    public <A> A withConnection(boolean autocommit, ConnectionCallable<A> block) {
        return db.withConnection(autocommit, DB.connectionFunction(block));
    }

    @Override
    public void withTransaction(ConnectionRunnable block) {
        db.withTransaction(DB.connectionFunction(block));
    }

    @Override
    public <A> A withTransaction(ConnectionCallable<A> block) {
        return db.withTransaction(DB.connectionFunction(block));
    }

    @Override
    public void shutdown() {
        db.shutdown();
    }

    @Override
    public play.api.db.Database toScala() {
        return db;
    }
}
