/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

/**
 * A base for Java connection pool components.
 *
 * @see ConnectionPool
 */
public interface ConnectionPoolComponents {

    ConnectionPool connectionPool();

}
