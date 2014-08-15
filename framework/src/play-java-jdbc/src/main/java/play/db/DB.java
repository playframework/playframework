/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import java.sql.Connection;
import java.util.Map;
import javax.sql.DataSource;

import scala.runtime.AbstractFunction1;

import play.Configuration;
import play.api.Application;

import com.typesafe.config.ConfigFactory;

/**
 * Provides a high-level API for getting JDBC connections.
 */
public class DB {

    /**
     * Create a default BoneCP-backed DBApi.
     */
    public static DBApi api(Configuration configuration, ClassLoader classLoader) {
        return new DefaultDBApi(
            new play.api.db.BoneConnectionPool(
                configuration.getWrappedConfiguration(),
                classLoader));
    }

    /**
     * Create a default BoneCP-backed DBApi.
     */
    public static DBApi api(Map<String, ? extends Object> config, ClassLoader classLoader) {
        return new DefaultDBApi(
            new play.api.db.BoneConnectionPool(
                new play.api.Configuration(ConfigFactory.parseMap(config)),
                classLoader));
    }

    /**
     * Create a default BoneCP-backed DBApi.
     */
    public static DBApi api(Map<String, ? extends Object> config) {
        return api(config, DB.class.getClassLoader());
    }

    /**
     * Returns the default datasource.
     */
    public static DataSource getDataSource() {
        return getDataSource("default");
    }

    /**
     * Returns specified database.
     *
     * @param name Datasource name
     */
    public static DataSource getDataSource(String name) {
        return play.api.db.DB.
            getDataSource(name, play.api.Play.unsafeApplication());
    }

    /**
     * Returns a connection from the default datasource,
     * with auto-commit enabled.
     */
    public static Connection getConnection() {
        return getConnection("default");
    }

    /**
     * Returns a connection from the default datasource,
     * with the specified auto-commit setting.
     */
    public static Connection getConnection(boolean autocommit) {
        return getConnection("default", autocommit);
    }

    /**
     * Returns a connection from any datasource, with auto-commit enabled.
     *
     * @param name Datasource name
     */
    public static Connection getConnection(String name) {
        return getConnection(name, true);
    }

    /**
     * Get a connection from any datasource,
     * with the specified auto-commit setting.
     *
     * @param name Datasource name
     * @param autocommit Auto-commit setting
     */
    public static Connection getConnection(String name, boolean autocommit) {
        return play.api.db.DB.
            getConnection(name, autocommit,
                          play.api.Play.unsafeApplication());
    }

    /**
     * Executes a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param name Datasource name
     * @param block Code block to execute
     * @param application Play application (<tt>play.api.Play.unsafeApplication()</tt>)
     */
    public static <A> A withConnection(String name,
                                       ConnectionCallable<A> block,
                                       Application application) {

        return play.api.db.DB.
            withConnection(name, connectionFunction(block), application);

    }

    /**
     * Execute a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param block Code block to execute
     * @param application Play application (<tt>play.api.Play.unsafeApplication()</tt>)
     */
    public static <A> A withConnection(ConnectionCallable<A> block,
                                       Application application) {

        return play.api.db.DB.
            withConnection(connectionFunction(block), application);
    }

    /**
     * Executes a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param name Datasource name
     * @param block Code block to execute
     */
    public static <A> A withConnection(String name, ConnectionCallable<A> block) {
        return play.api.db.DB.withConnection(name, connectionFunction(block),
                                             play.api.Play.unsafeApplication());

    }

    /**
     * Execute a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param block Code block to execute
     */
    public static <A> A withConnection(ConnectionCallable<A> block) {
        return play.api.db.DB.withConnection(connectionFunction(block),
                                             play.api.Play.unsafeApplication());
    }

    /**
     * Execute a block of code, in the scope of a JDBC transaction.
     * The connection and all created statements are automatically released.
     * The transaction is automatically committed, unless an exception occurs.
     *
     * @param name Datasource name
     * @param block Code block to execute
     * @param application Play application (<tt>play.api.Play.unsafeApplication()</tt>)
     */
    public static <A> A withTransaction(String name,
                                        ConnectionCallable<A> block,
                                        Application application) {

        return play.api.db.DB.
            withTransaction(name, connectionFunction(block), application);
    }

    /**
     * Execute a block of code, in the scope of a JDBC transaction.
     * The connection and all created statements are automatically released.
     * The transaction is automatically committed, unless an exception occurs.
     *
     * @param block Code block to execute
     * @param application Play application (<tt>play.api.Play.unsafeApplication()</tt>)
     */
    public static <A> A withTransaction(ConnectionCallable<A> block,
                                        Application application) {

        return play.api.db.DB.
            withTransaction(connectionFunction(block), application);

    }

    /**
     * Execute a block of code, in the scope of a JDBC transaction.
     * The connection and all created statements are automatically released.
     * The transaction is automatically committed, unless an exception occurs.
     *
     * @param name Datasource name
     * @param block Code block to execute
     */
    public static <A> A withTransaction(String name,
                                        ConnectionCallable<A> block) {

        return play.api.db.DB.
            withTransaction(name, connectionFunction(block),
                            play.api.Play.unsafeApplication());
    }

    /**
     * Execute a block of code, in the scope of a JDBC transaction.
     * The connection and all created statements are automatically released.
     * The transaction is automatically committed, unless an exception occurs.
     *
     * @param block Code block to execute
     */
    public static <A> A withTransaction(ConnectionCallable<A> block) {

        return play.api.db.DB.
            withTransaction(connectionFunction(block),
                            play.api.Play.unsafeApplication());

    }

    /** Returns a function wrapper from Java callable. */
    public static final <A> AbstractFunction1<Connection, A> connectionFunction(final ConnectionCallable<A> block) {
        return new AbstractFunction1<Connection, A>() {
            public A apply(Connection con) {
                try {
                    return block.call(con);
                } catch (java.sql.SQLException e) {
                    throw new RuntimeException("Connection callable failed", e);
                }
            }
        };
    }
}
