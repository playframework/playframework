/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import java.sql.Connection;
import javax.sql.DataSource;

import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

import play.api.Application;

/**
 * Provides a high-level API for getting JDBC connections.
 */
public class DB {

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
        return play.api.db.DB.getDataSource(name, play.api.Play.unsafeApplication());
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
        return play.api.db.DB.getConnection(name, autocommit, play.api.Play.unsafeApplication());
    }

    /**
     * Executes a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param name Datasource name
     * @param autocommit Auto-commit setting
     * @param block Code block to execute
     * @param application Play application (<tt>play.api.Play.unsafeApplication()</tt>)
     */
    public static void withConnection(String name, boolean autocommit, ConnectionRunnable block, Application application) {
        play.api.db.DB.withConnection(name, autocommit, connectionFunction(block), application);
    }

    /**
     * Executes a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param name Datasource name
     * @param block Code block to execute
     * @param application Play application (<tt>play.api.Play.unsafeApplication()</tt>)
     */
    public static void withConnection(String name, ConnectionRunnable block, Application application) {
        withConnection(name, true, block, application);
    }

    /**
     * Executes a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param autocommit Auto-commit setting
     * @param block Code block to execute
     * @param application Play application (<tt>play.api.Play.unsafeApplication()</tt>)
     */
    public static void withConnection(boolean autocommit, ConnectionRunnable block, Application application) {
        withConnection("default", autocommit, block, application);
    }

    /**
     * Execute a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param block Code block to execute
     * @param application Play application (<tt>play.api.Play.unsafeApplication()</tt>)
     */
    public static void withConnection(ConnectionRunnable block, Application application) {
        withConnection("default", true, block, application);
    }

    /**
     * Executes a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param name Datasource name
     * @param autocommit Auto-commit setting
     * @param block Code block to execute
     */
    public static void withConnection(String name, boolean autocommit, ConnectionRunnable block) {
        withConnection(name, autocommit, block, play.api.Play.unsafeApplication());
    }

    /**
     * Executes a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param name Datasource name
     * @param block Code block to execute
     */
    public static void withConnection(String name, ConnectionRunnable block) {
        withConnection(name, true, block);
    }

    /**
     * Executes a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param autocommit Auto-commit setting
     * @param block Code block to execute
     */
    public static void withConnection(boolean autocommit, ConnectionRunnable block) {
        withConnection("default", autocommit, block);
    }

    /**
     * Execute a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param block Code block to execute
     */
    public static void withConnection(ConnectionRunnable block) {
        withConnection("default", true, block);
    }

    /**
     * Executes a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param name Datasource name
     * @param autocommit Auto-commit setting
     * @param block Code block to execute
     * @param application Play application (<tt>play.api.Play.unsafeApplication()</tt>)
     */
    public static <A> A withConnection(String name, boolean autocommit, ConnectionCallable<A> block, Application application) {
        return play.api.db.DB.withConnection(name, autocommit, connectionFunction(block), application);
    }

    /**
     * Executes a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param name Datasource name
     * @param block Code block to execute
     * @param application Play application (<tt>play.api.Play.unsafeApplication()</tt>)
     */
    public static <A> A withConnection(String name, ConnectionCallable<A> block, Application application) {
        return withConnection(name, true, block, application);
    }

    /**
     * Executes a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param autocommit Auto-commit setting
     * @param block Code block to execute
     * @param application Play application (<tt>play.api.Play.unsafeApplication()</tt>)
     */
    public static <A> A withConnection(boolean autocommit, ConnectionCallable<A> block, Application application) {
        return withConnection("default", autocommit, block, application);
    }

    /**
     * Execute a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param block Code block to execute
     * @param application Play application (<tt>play.api.Play.unsafeApplication()</tt>)
     */
    public static <A> A withConnection(ConnectionCallable<A> block, Application application) {
        return withConnection("default", true, block, application);
    }

    /**
     * Executes a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param name Datasource name
     * @param autocommit Auto-commit setting
     * @param block Code block to execute
     */
    public static <A> A withConnection(String name, boolean autocommit, ConnectionCallable<A> block) {
        return withConnection(name, autocommit, block, play.api.Play.unsafeApplication());
    }

    /**
     * Executes a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param name Datasource name
     * @param block Code block to execute
     */
    public static <A> A withConnection(String name, ConnectionCallable<A> block) {
        return withConnection(name, true, block);
    }

    /**
     * Executes a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param autocommit Auto-commit setting
     * @param block Code block to execute
     */
    public static <A> A withConnection(boolean autocommit, ConnectionCallable<A> block) {
        return withConnection("default", autocommit, block);
    }

    /**
     * Execute a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param block Code block to execute
     */
    public static <A> A withConnection(ConnectionCallable<A> block) {
        return withConnection("default", true, block);
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
    public static void withTransaction(String name, ConnectionRunnable block, Application application) {
        play.api.db.DB.withTransaction(name, connectionFunction(block), application);
    }

    /**
     * Execute a block of code, in the scope of a JDBC transaction.
     * The connection and all created statements are automatically released.
     * The transaction is automatically committed, unless an exception occurs.
     *
     * @param block Code block to execute
     * @param application Play application (<tt>play.api.Play.unsafeApplication()</tt>)
     */
    public static void withTransaction(ConnectionRunnable block, Application application) {
        withTransaction("default", block, application);

    }

    /**
     * Execute a block of code, in the scope of a JDBC transaction.
     * The connection and all created statements are automatically released.
     * The transaction is automatically committed, unless an exception occurs.
     *
     * @param name Datasource name
     * @param block Code block to execute
     */
    public static void withTransaction(String name, ConnectionRunnable block) {
        withTransaction(name, block, play.api.Play.unsafeApplication());
    }

    /**
     * Execute a block of code, in the scope of a JDBC transaction.
     * The connection and all created statements are automatically released.
     * The transaction is automatically committed, unless an exception occurs.
     *
     * @param block Code block to execute
     */
    public static void withTransaction(ConnectionRunnable block) {
        withTransaction("default", block);
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
    public static <A> A withTransaction(String name, ConnectionCallable<A> block, Application application) {
        return play.api.db.DB.withTransaction(name, connectionFunction(block), application);
    }

    /**
     * Execute a block of code, in the scope of a JDBC transaction.
     * The connection and all created statements are automatically released.
     * The transaction is automatically committed, unless an exception occurs.
     *
     * @param block Code block to execute
     * @param application Play application (<tt>play.api.Play.unsafeApplication()</tt>)
     */
    public static <A> A withTransaction(ConnectionCallable<A> block, Application application) {
        return withTransaction("default", block, application);

    }

    /**
     * Execute a block of code, in the scope of a JDBC transaction.
     * The connection and all created statements are automatically released.
     * The transaction is automatically committed, unless an exception occurs.
     *
     * @param name Datasource name
     * @param block Code block to execute
     */
    public static <A> A withTransaction(String name, ConnectionCallable<A> block) {
        return withTransaction(name, block, play.api.Play.unsafeApplication());
    }

    /**
     * Execute a block of code, in the scope of a JDBC transaction.
     * The connection and all created statements are automatically released.
     * The transaction is automatically committed, unless an exception occurs.
     *
     * @param block Code block to execute
     */
    public static <A> A withTransaction(ConnectionCallable<A> block) {
        return withTransaction("default", block);
    }

    /**
     * Create a Scala function wrapper for ConnectionRunnable.
     */
    public static final AbstractFunction1<Connection, BoxedUnit> connectionFunction(final ConnectionRunnable block) {
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
     */
    public static final <A> AbstractFunction1<Connection, A> connectionFunction(final ConnectionCallable<A> block) {
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
