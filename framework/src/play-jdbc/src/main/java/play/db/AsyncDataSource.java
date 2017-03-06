/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db;

import play.api.db.AsyncDataSource$;
import scala.compat.java8.FutureConverters;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;

public interface AsyncDataSource extends DataSource, Closeable {

    CompletionStage<Connection> getConnectionAsync();

    /**
     * Wraps a {@link DataSource} to make an {@link AsyncDataSource}. The asynchrony is
     * achieved by requesting connections on a separate thread.
     *
     * @param ds The {@link DataSource} to wrap.
     * @param closeable A {@link Closeable} object to call when closing the `DataSource`.
     *                  Since many {@link DataSource} implementations are also
     *                  {@link Closeable} this parameter will often be the value for
     *                  the {@code dataSource} parameter repeated again.
     * @return An {@link AsyncDataSource} that wraps the given {@link DataSource}.
     */
    static AsyncDataSource wrap(DataSource ds, Closeable closeable) {
        return asJava(AsyncDataSource$.MODULE$.wrap(ds, closeable));
    }

    static AsyncDataSource asJava(final play.api.db.AsyncDataSource sads) {
        return new AsyncDataSource() {
            @Override
            public CompletionStage<Connection> getConnectionAsync() {
                return FutureConverters.toJava(sads.getConnectionAsync());
            }

            @Override
            public void close() throws IOException {
                sads.close();
            }

            @Override
            public Connection getConnection() throws SQLException {
                return sads.getConnection();
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return sads.getConnection(username, password);
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                return sads.unwrap(iface);
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return sads.isWrapperFor(iface);
            }

            @Override
            public PrintWriter getLogWriter() throws SQLException {
                return sads.getLogWriter();
            }

            @Override
            public void setLogWriter(PrintWriter out) throws SQLException {
                sads.setLogWriter(out);
            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {
                sads.setLoginTimeout(seconds);
            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return sads.getLoginTimeout();
            }

            @Override
            public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                return sads.getParentLogger();
            }
        };
    }
}
