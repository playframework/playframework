/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db;

import play.Environment;
import play.api.db.HikariCPConnectionPool;

/**
 * HikariCP Java components (for compile-time injection).
 */
public interface HikariCPComponents extends AsyncConnectionPoolComponents, ConnectionPoolComponents {

    Environment environment();

    default AsyncConnectionPool asyncConnectionPool() {
        return new HikariCPConnectionPool(environment().asScala()).asJava();
    }

    default ConnectionPool connectionPool() {
        return asyncConnectionPool().asScala().toConnectionPool().asJava();
    }

}