/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
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
	 * The JDBC connection URL this database, i.e. `jdbc:...` Normally retrieved
	 * via a connection.
	 */
	public String getUrl();

	/**
	 * Get a JDBC connection from the underlying data source. Autocommit is
	 * enabled by default.
	 *
	 * Don't forget to release the connection at some point by calling close().
	 *
	 * @return a JDBC connection
	 */
	public Connection getConnection();

	/**
	 * Get a JDBC connection from the underlying data source.
	 *
	 * Don't forget to release the connection at some point by calling close().
	 *
	 * @param autocommit
	 *            determines whether to autocommit the connection
	 * @return a JDBC connection
	 */
	public Connection getConnection(boolean autocommit);

	/**
	 * Execute a block of code, providing a JDBC connection. The connection and
	 * all created statements are automatically released.
	 *
	 * @param block
	 *            code to execute
	 */
	public void withConnection(ConnectionRunnable block);

	/**
	 * Execute a block of code, providing a JDBC connection. The connection and
	 * all created statements are automatically released.
	 *
	 * @param block
	 *            code to execute
	 * @return the result of the code block
	 */
	public <A> A withConnection(ConnectionCallable<A> block);

	/**
	 * Execute a block of code, providing a JDBC connection. The connection and
	 * all created statements are automatically released.
	 *
	 * @param autocommit
	 *            determines whether to autocommit the connection
	 * @param block
	 *            code to execute
	 */
	public void withConnection(boolean autocommit, ConnectionRunnable block);

	/**
	 * Execute a block of code, providing a JDBC connection. The connection and
	 * all created statements are automatically released.
	 *
	 * @param autocommit
	 *            determines whether to autocommit the connection
	 * @param block
	 *            code to execute
	 * @return the result of the code block
	 */
	public <A> A withConnection(boolean autocommit, ConnectionCallable<A> block);

	/**
	 * Execute a block of code in the scope of a JDBC transaction. The
	 * connection and all created statements are automatically released. The
	 * transaction is automatically committed, unless an exception occurs.
	 *
	 * @param block
	 *            code to execute
	 */
	public void withTransaction(ConnectionRunnable block);

	/**
	 * Execute a block of code in the scope of a JDBC transaction. The
	 * connection and all created statements are automatically released. The
	 * transaction is automatically committed, unless an exception occurs.
	 *
	 * @param block
	 *            code to execute
	 * @return the result of the code block
	 */
	public <A> A withTransaction(ConnectionCallable<A> block);

	/**
	 * Shutdown this database, closing the underlying data source.
	 */
	public void shutdown();

	/**
	 * Converts the given database to a Scala database
	 */
	public default play.api.db.Database toScala() {
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

		};
	}
}
