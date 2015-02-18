/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;

import com.google.common.collect.ImmutableMap;

/**
 * Database API for managing data sources and connections.
 */
public abstract class Database {

    /**
     * The configuration name for this database.
     */
    public abstract String getName();

    /**
     * The underlying JDBC data source for this database.
     */
    public abstract DataSource getDataSource();

    /**
     * The JDBC connection URL this database, i.e. `jdbc:...`
     * Normally retrieved via a connection.
     */
    public abstract String getUrl();

    /**
     * Get a JDBC connection from the underlying data source.
     * Autocommit is enabled by default.
     *
     * Don't forget to release the connection at some point by calling close().
     *
     * @return a JDBC connection
     */
    public abstract Connection getConnection();

    /**
     * Get a JDBC connection from the underlying data source.
     *
     * Don't forget to release the connection at some point by calling close().
     *
     * @param autocommit determines whether to autocommit the connection
     * @return a JDBC connection
     */
    public abstract Connection getConnection(boolean autocommit);

    /**
     * Execute a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param block code to execute
     */
    public abstract void withConnection(ConnectionRunnable block);

    /**
     * Execute a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param block code to execute
     * @return the result of the code block
     */
    public abstract <A> A withConnection(ConnectionCallable<A> block);

    /**
     * Execute a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param autocommit determines whether to autocommit the connection
     * @param block code to execute
     */
    public abstract void withConnection(boolean autocommit, ConnectionRunnable block);

    /**
     * Execute a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param autocommit determines whether to autocommit the connection
     * @param block code to execute
     * @return the result of the code block
     */
    public abstract <A> A withConnection(boolean autocommit, ConnectionCallable<A> block);

    /**
     * Execute a block of code in the scope of a JDBC transaction.
     * The connection and all created statements are automatically released.
     * The transaction is automatically committed, unless an exception occurs.
     *
     * @param block code to execute
     */
    public abstract void withTransaction(ConnectionRunnable block);

    /**
     * Execute a block of code in the scope of a JDBC transaction.
     * The connection and all created statements are automatically released.
     * The transaction is automatically committed, unless an exception occurs.
     *
     * @param block code to execute
     * @return the result of the code block
     */
    public abstract <A> A withTransaction(ConnectionCallable<A> block);

    /**
     * Shutdown this database, closing the underlying data source.
     */
    public abstract void shutdown();

    // ----------------
    // Creation helpers
    // ----------------

    /**
     * Create a pooled database with the given configuration.
     *
     * @param name the database name
     * @param driver the database driver class
     * @param url the database url
     * @param config a map of extra database configuration
     * @return a configured database
     */
    public static Database createFrom(String name, String driver, String url, Map<String, ? extends Object> config) {
        ImmutableMap.Builder<String, Object> dbConfig = new ImmutableMap.Builder<String, Object>();
        dbConfig.put("driver", driver);
        dbConfig.put("url", url);
        dbConfig.putAll(config);
        return new DefaultDatabase(name, dbConfig.build());
    }

    /**
     * Create a pooled database with the given configuration.
     *
     * @param name the database name
     * @param driver the database driver class
     * @param url the database url
     * @return a configured database
     */
    public static Database createFrom(String name, String driver, String url) {
        return createFrom(name, driver, url, ImmutableMap.<String, Object>of());
    }

    /**
     * Create a pooled database named "default" with the given configuration.
     *
     * @param driver the database driver class
     * @param url the database url
     * @param config a map of extra database configuration
     * @return a configured database
     */
    public static Database createFrom(String driver, String url, Map<String, ? extends Object> config) {
        return createFrom("default", driver, url, config);
    }

    /**
     * Create a pooled database named "default" with the given driver and url.
     *
     * @param driver the database driver class
     * @param url the database url
     * @return a configured database
     */
    public static Database createFrom(String driver, String url) {
        return createFrom("default", driver, url, ImmutableMap.<String, Object>of());
    }

    /**
     * Create an in-memory H2 database.
     *
     * @param name the database name
     * @param url the database url
     * @param config a map of extra database configuration
     * @return a configured in-memory h2 database
     */
    public static Database inMemory(String name, String url, Map<String, ? extends Object> config) {
        return createFrom(name, "org.h2.Driver", url, config);
    }

    /**
     * Create an in-memory H2 database.
     *
     * @param name the database name
     * @param urlOptions a map of extra url options
     * @param config a map of extra database configuration
     * @return a configured in-memory h2 database
     */
    public static Database inMemory(String name, Map<String, String> urlOptions, Map<String, ? extends Object> config) {
        String urlExtra = "";
        for (Map.Entry<String, String> option : urlOptions.entrySet()) {
            urlExtra += ";" + option.getKey() + "=" + option.getValue();
        }
        String url = "jdbc:h2:mem:" + name + urlExtra;
        return inMemory(name, url, config);
    }

    /**
     * Create an in-memory H2 database.
     *
     * @param name the database name
     * @param config a map of extra database configuration
     * @return a configured in-memory h2 database
     */
    public static Database inMemory(String name, Map<String, ? extends Object> config) {
        return inMemory(name, "jdbc:h2:mem:" + name, config);
    }

    /**
     * Create an in-memory H2 database.
     *
     * @param name the database name
     * @return a configured in-memory h2 database
     */
    public static Database inMemory(String name) {
        return inMemory(name, ImmutableMap.<String, Object>of());
    }

    /**
     * Create an in-memory H2 database with name "default".
     *
     * @param config a map of extra database configuration
     * @return a configured in-memory h2 database
     */
    public static Database inMemory(Map<String, ? extends Object> config) {
        return inMemory("default", config);
    }

    /**
     * Create an in-memory H2 database with name "default".
     *
     * @return a configured in-memory h2 database
     */
    public static Database inMemory() {
        return inMemory("default");
    }

    /**
     * Create an in-memory H2 database with name "default" and with
     * extra configuration provided by the given entries.
     */
    public static Database inMemoryWith(String k1, Object v1) {
        return inMemory(ImmutableMap.of(k1, v1));
    }

    /**
     * Create an in-memory H2 database with name "default" and with
     * extra configuration provided by the given entries.
     */
    public static Database inMemoryWith(String k1, Object v1, String k2, Object v2) {
        return inMemory(ImmutableMap.of(k1, v1, k2, v2));
    }

    /**
     * Create an in-memory H2 database with name "default" and with
     * extra configuration provided by the given entries.
     */
    public static Database inMemoryWith(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        return inMemory(ImmutableMap.of(k1, v1, k2, v2, k3, v3));
    }

    /**
     * Converts the given database to a Scala database
     */
    public static play.api.db.Database toScala(final Database database) {
        if (database instanceof DefaultDatabase) {
            return ((DefaultDatabase) database).toScala();
        } else {
            return new play.api.db.Database() {
                @Override
                public String name() {
                    return database.getName();
                }

                @Override
                public Connection getConnection() {
                    return database.getConnection();
                }

                @Override
                public void shutdown() {
                    database.shutdown();
                }

                @Override
                public <A> A withConnection(boolean autocommit, final scala.Function1<Connection, A> block) {
                    return database.withConnection(autocommit, new ConnectionCallable<A>() {
                        @Override
                        public A call(Connection connection) throws SQLException {
                            return block.apply(connection);
                        }
                    });
                }

                @Override
                public <A> A withConnection(final scala.Function1<Connection, A> block) {
                    return database.withConnection(new ConnectionCallable<A>() {
                        @Override
                        public A call(Connection connection) throws SQLException {
                            return block.apply(connection);
                        }
                    });
                }

                @Override
                public String url() {
                    return database.getUrl();
                }

                @Override
                public DataSource dataSource() {
                    return database.getDataSource();
                }

                @Override
                public Connection getConnection(boolean autocommit) {
                    return database.getConnection(autocommit);
                }

                public <A> A withTransaction(final scala.Function1<Connection, A> block) {
                    return database.withTransaction(new ConnectionCallable<A>() {
                        @Override
                        public A call(Connection connection) throws SQLException {
                            return block.apply(connection);
                        }
                    });
                }

            };
        }
    }
}
