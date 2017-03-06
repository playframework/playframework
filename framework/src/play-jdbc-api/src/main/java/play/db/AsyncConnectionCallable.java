/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletionStage;

/**
 * Represents a function that accepts a {@link Connection} and asynchronously
 * returns a result. This functional interfaace is designed to be used with the
 * methods in the {@link Database} interface.
 */
public interface AsyncConnectionCallable<A> {
    public CompletionStage<A> call(Connection connection) throws SQLException;
}
