/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import play.Environment;
import play.api.db.HikariCPConnectionPool;

/**
 * HikariCP Java components (for compile-time injection).
 */
public interface HikariCPComponents extends ConnectionPoolComponents {

    Environment environment();

    default ConnectionPool connectionPool() {
        return new DefaultConnectionPool(new HikariCPConnectionPool(environment().asScala()));
    }
}
