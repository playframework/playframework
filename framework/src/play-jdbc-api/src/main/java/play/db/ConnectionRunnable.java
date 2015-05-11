/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Similar to java.lang.Runnable with a Connection as argument.
 * Provides a functional interface for use with Java 8+.
 * To return a result use ConnectionCallable.
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
public interface ConnectionRunnable {
    public void run(Connection connection) throws SQLException;
}
