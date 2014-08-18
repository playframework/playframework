/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
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
     */
    public DefaultDatabase(String name, Configuration configuration) {
        this(new play.api.db.PooledDatabase(name, configuration.getWrappedConfiguration()));
    }

    /**
     * Create a default BoneCP-backed database.
     */
    public DefaultDatabase(String name, Map<String, ? extends Object> config) {
        this(new play.api.db.PooledDatabase(name, new play.api.Configuration(ConfigFactory.parseMap(config))));
    }

    /**
     * Create a default BoneCP-backed database.
     */
    public DefaultDatabase(Map<String, ? extends Object> config) {
        this("default", config);
    }

    public String getName() {
        return db.name();
    }

    public DataSource getDataSource() {
        return db.dataSource();
    }

    public String getUrl() {
        return db.url();
    }

    public Connection getConnection() {
        return db.getConnection();
    }

    public Connection getConnection(boolean autocommit) {
        return db.getConnection(autocommit);
    }

    public <A> A withConnection(ConnectionCallable<A> block) {
        return db.withConnection(DB.connectionFunction(block));
    }

    public <A> A withTransaction(ConnectionCallable<A> block) {
        return db.withTransaction(DB.connectionFunction(block));
    }

    public void shutdown() {
        db.shutdown();
    }

}
