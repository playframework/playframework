/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.sql;

import java.sql.Connection;
import javax.inject.Inject;

import play.mvc.Controller;
import play.db.NamedDatabase;
import play.db.Database;

// inject "orders" database instead of "default"
class JavaJdbcConnection extends Controller {
    private Database db;

    @Inject
    public JavaJdbcConnection(Database db) {
        this.db = db;
    }
    // get jdbc connection
    Connection connection = db.getConnection();
}
