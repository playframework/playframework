/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;

import play.libs.F.Tuple;

/**
 * Database API for managing data sources.
 */
public interface DBApi {

    /**
     * List of all data sources (with their database names).
     */
    public List<Tuple<DataSource, String>> dataSources();

    /**
     * Shutdown the connection pool for the given data source.
     */
    public void shutdownPool(DataSource ds);

    /**
    * Retrieve a JDBC connection, with auto-commit set to `true`.
    *
    * Don't forget to release the connection at some point by calling close().
    *
    * @param name the data source name
    * @return a JDBC connection
    * @throws an error if the required data source is not registered
    */
    public DataSource getDataSource(String name);

    /**
     * Retrieve the JDBC connection URL for a particular data source.
     *
     * @param name the data source name
     * @return The JDBC URL connection string, i.e. `jdbc:...`
     * @throws an error if the required data source is not registered
     */
    public String getDataSourceURL(String name);

    /**
     * Retrieve a JDBC connection.
     *
     * Don't forget to release the connection at some point by calling close().
     *
     * @param name the data source name
     * @param autocommit set this connection to auto-commit
     * @return a JDBC connection
     * @throws an error if the required data source is not registered
     */
    public Connection getConnection(String name, boolean autocommit);

    /**
     * Execute a block of code, providing a JDBC connection.
     * The connection and all created statements are automatically released.
     *
     * @param name the data source name
     * @param block the block of code to execute
     */
    public <A> A withConnection(String name, ConnectionCallable<A> block);

    /**
     * Execute a block of code, in the scope of a JDBC transaction.
     * The connection and all created statements are automatically released.
     * The transaction is automatically committed, unless an exception occurs.
     *
     * @param name the data source name
     * @param block the block of code to execute
     */
    public <A> A withTransaction(String name, ConnectionCallable<A> block);

}
