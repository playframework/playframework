/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Similar to java.util.concurrent.Callable with a Connection as argument.
 * Provides a functional interface for use with Java 8+.
 * If no result needs to be returned, ConnectionRunnable can be used instead.
 *
 * Vanilla Java:
 * <code>
 * new ConnectionCallable&lt;A&gt;() {
 *   public A call(Connection c) { return ...; }
 * }
 * </code>
 *
 * Java Lambda:
 * <code>(Connection c) -&gt; ...</code>
 */
public interface ConnectionCallable<A> {
    public A call(Connection connection) throws SQLException;
}
