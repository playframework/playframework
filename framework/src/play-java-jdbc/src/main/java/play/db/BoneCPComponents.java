/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db;

import com.typesafe.config.Config;
import play.Environment;
import play.api.db.BoneConnectionPool;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * BoneCP Java components (for compile-time injection).
 */
public interface BoneCPComponents extends AsyncConnectionPoolComponents, ConnectionPoolComponents {

    Environment environment();

    default AsyncConnectionPool asyncConnectionPool() {
        return new BoneConnectionPool(environment().asScala()).asJava();
    }

    default ConnectionPool connectionPool() {
        return asyncConnectionPool().asScala().toConnectionPool().asJava();
    }
}
