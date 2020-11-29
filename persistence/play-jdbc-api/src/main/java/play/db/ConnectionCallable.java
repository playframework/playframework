/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Similar to java.util.concurrent.Callable with a Connection as argument. Provides a functional
 * interface for use with Java 8+. If no result needs to be returned, ConnectionRunnable can be used
 * instead.
 *
 * <p>Vanilla Java: <code>
 * new ConnectionCallable&lt;A&gt;() {
 *   public A call(Connection c) { return ...; }
 * }
 * </code> Java Lambda: <code>(Connection c) -&gt; ...</code>
 */
public interface ConnectionCallable<A> {
  A call(Connection connection) throws SQLException;
}
