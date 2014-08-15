/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Similar to java.util.concurrent.Callable with a connection as argument.
 * Usable from vanilla Java 6+, or as lambda as it's a functionnal interface.
 *
 * Vanilla Java:
 * <code>
 * new ConnectionCallable<A>() {
 *   public A call(Connection con) { ... }
 * }
 * </code>
 *
 * Java Lambda:
 * <code>(Connection con) -> ...</code>
 */
public interface ConnectionCallable<A> {
    public A call(Connection con) throws SQLException;
}
