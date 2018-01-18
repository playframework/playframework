/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.sql;

//#java-named-database
import javax.inject.Inject;

import play.db.NamedDatabase;
import play.db.Database;

@javax.inject.Singleton
class JavaNamedDatabase {

    private Database db;
    private DatabaseExecutionContext executionContext;

    @Inject // inject "orders" database instead of "default"
    public JavaNamedDatabase(@NamedDatabase("orders") Database db, DatabaseExecutionContext executionContext) {
        this.db = db;
        this.executionContext = executionContext;
    }

    // do whatever you need with the db using supplyAsync(() -> { ... }, executionContext);
}
//#java-named-database
