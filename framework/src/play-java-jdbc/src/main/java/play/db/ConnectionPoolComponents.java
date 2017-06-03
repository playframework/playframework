/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db;

/**
 * A base for Java connection pool components.
 *
 * @see ConnectionPool
 * @deprecated Use {@link AsyncConnectionPoolComponents} instead.
 */
@Deprecated
public interface ConnectionPoolComponents {

    ConnectionPool connectionPool();

}
