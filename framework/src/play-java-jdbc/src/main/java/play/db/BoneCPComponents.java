/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db;

import play.Environment;
import play.api.db.BoneConnectionPool;

/**
 * BoneCP Java components (for compile-time injection).
 */
public interface BoneCPComponents extends ConnectionPoolComponents {

    Environment environment();

    default ConnectionPool connectionPool() {
        return new DefaultConnectionPool(new BoneConnectionPool(environment().asScala()));
    }
}
