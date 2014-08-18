/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import java.sql.Connection;
import javax.sql.DataSource;

/**
 * Database API for managing data sources and connections.
 */
public interface Database {

    /**
     * The configuration name for this database.
     */
    public String getName();

    /**
     * The underlying JDBC data source for this database.
     */
    public DataSource getDataSource();

    /**
     * The JDBC connection URL this database, i.e. `jdbc:...`
     * Normally retrieved via a connection.
     */
    public String getUrl();

    /**
     * Get a JDBC connection from the underlying data source.
     * Autocommit is enabled by default.
     *
     * Don't forget to release the connection at some point by calling close().
     *
     * @param autocommit determines whether to autocommit the connection
     * @return a JDBC connection
     */
    public Connection getConnection();

    /**
     * Get a JDBC connection from the underlying data source.
     *
     * Don't forget to release the connection at some point by calling close().
     *
     * @param autocommit determines whether to autocommit the connection
     * @return a JDBC connection
     */
    public Connection getConnection(boolean autocommit);

    /**
     * Execute a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param block code to execute
     * @return the result of the code block
     */
    public <A> A withConnection(ConnectionCallable<A> block);

    /**
     * Execute a block of code in the scope of a JDBC transaction.
     * The connection and all created statements are automatically released.
     * The transaction is automatically committed, unless an exception occurs.
     *
     * @param block code to execute
     * @return the result of the code block
     */
    public <A> A withTransaction(ConnectionCallable<A> block);

    /**
     * Shutdown this database, closing the underlying data source.
     */
    public void shutdown();

}
