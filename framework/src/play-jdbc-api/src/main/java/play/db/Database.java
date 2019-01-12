/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Database API for managing data sources and connections.
 */
public interface Database {

    /**
     * @return the configuration name for this database.
     */
    public String getName();

    /**
     * @return the underlying JDBC data source for this database.
     */
    public DataSource getDataSource();

    /**
     * @return the JDBC connection URL this database, i.e. `jdbc:...` Normally retrieved
     * via a connection.
     */
    public String getUrl();

    /**
     * Get a JDBC connection from the underlying data source. Autocommit is
     * enabled by default.
     * <p>
     * Don't forget to release the connection at some point by calling close().
     *
     * @return a JDBC connection
     */
    public Connection getConnection();

    /**
     * Get a JDBC connection from the underlying data source.
     * <p>
     * Don't forget to release the connection at some point by calling close().
     *
     * @param autocommit determines whether to autocommit the connection
     * @return a JDBC connection
     */
    public Connection getConnection(boolean autocommit);

    /**
     * Execute a block of code, providing a JDBC connection. The connection and
     * all created statements are automatically released.
     *
     * @param block code to execute
     */
    public void withConnection(ConnectionRunnable block);

    /**
     * Execute a block of code, providing a JDBC connection. The connection and
     * all created statements are automatically released.
     *
     * @param <A> the return value's type
     * @param block code to execute
     * @return the result of the code block
     */
    public <A> A withConnection(ConnectionCallable<A> block);

    /**
     * Execute a block of code, providing a JDBC connection. The connection and
     * all created statements are automatically released.
     *
     * @param autocommit determines whether to autocommit the connection
     * @param block      code to execute
     */
    public void withConnection(boolean autocommit, ConnectionRunnable block);

    /**
     * Execute a block of code, providing a JDBC connection. The connection and
     * all created statements are automatically released.
     *
     * @param <A> the return value's type
     * @param autocommit determines whether to autocommit the connection
     * @param block      code to execute
     * @return the result of the code block
     */
    public <A> A withConnection(boolean autocommit, ConnectionCallable<A> block);

    /**
     * Execute a block of code in the scope of a JDBC transaction. The
     * connection and all created statements are automatically released. The
     * transaction is automatically committed, unless an exception occurs.
     *
     * @param block code to execute
     */
    public void withTransaction(ConnectionRunnable block);

    /**
     * Execute a block of code in the scope of a JDBC transaction. The
     * connection and all created statements are automatically released. The
     * transaction is automatically committed, unless an exception occurs.
     *
     * @param isolationLevel determines transaction isolation level
     * @param block code to execute
     */
    public void withTransaction(TransactionIsolationLevel isolationLevel, ConnectionRunnable block);

    /**
     * Execute a block of code in the scope of a JDBC transaction. The
     * connection and all created statements are automatically released. The
     * transaction is automatically committed, unless an exception occurs.
     *
     * @param <A> the return value's type
     * @param block code to execute
     * @return the result of the code block
     */
    public <A> A withTransaction(ConnectionCallable<A> block);

    /**
     * Execute a block of code in the scope of a JDBC transaction. The
     * connection and all created statements are automatically released. The
     * transaction is automatically committed, unless an exception occurs.
     *
     * @param isolationLevel determines transaction isolation level
     * @param <A> the return value's type
     * @param block code to execute
     * @return the result of the code block
     */
    public <A> A withTransaction(TransactionIsolationLevel isolationLevel, ConnectionCallable<A> block);

    /**
     * Shutdown this database, closing the underlying data source.
     */
    public void shutdown();

    /**
     * Converts the given database to a Scala database
     * @return the database for scala API.
     * @deprecated As of release 2.6.0. Use {@link #asScala()}
     */
    @Deprecated
    public default play.api.db.Database toScala() {
        return asScala();
    }

    /**
     * Converts the given database to a Scala database
     * @return the database for scala API.
     */
    public default play.api.db.Database asScala() {
        return new play.api.db.Database() {
            @Override
            public String name() {
                return Database.this.getName();
            }

            @Override
            public Connection getConnection() {
                return Database.this.getConnection();
            }

            @Override
            public void shutdown() {
                Database.this.shutdown();
            }

            @Override
            public <A> A withConnection(boolean autocommit,
                                        final scala.Function1<Connection, A> block) {
                return Database.this.withConnection(autocommit, block::apply);
            }

            @Override
            public <A> A withConnection(
                    final scala.Function1<Connection, A> block) {
                return Database.this.withConnection(block::apply);
            }

            @Override
            public String url() {
                return Database.this.getUrl();
            }

            @Override
            public DataSource dataSource() {
                return Database.this.getDataSource();
            }

            @Override
            public Connection getConnection(boolean autocommit) {
                return Database.this.getConnection(autocommit);
            }

            public <A> A withTransaction(
                    final scala.Function1<Connection, A> block) {
                return Database.this.withTransaction(block::apply);
            }

            public <A> A withTransaction(play.api.db.TransactionIsolationLevel isolationLevel,
                                         final scala.Function1<Connection, A> block) {
                return Database.this.withTransaction(isolationLevel.asJava(), block::apply);
            }
        };
    }
}
