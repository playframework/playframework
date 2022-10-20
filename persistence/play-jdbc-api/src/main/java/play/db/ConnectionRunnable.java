/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Similar to java.lang.Runnable with a Connection as argument. Provides a functional interface for
 * use with Java 8+. To return a result use ConnectionCallable.
 *
 * <p>Vanilla Java: <code>
 * new ConnectionCallable&lt;A&gt;() {
 *   public A call(Connection c) { return ...; }
 * }
 * </code> Java Lambda: <code>(Connection c) -&gt; ...</code>
 */
public interface ConnectionRunnable {
  void run(Connection connection) throws SQLException;
}
