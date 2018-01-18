/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.sql;

//#java-jdbc-connection
import java.sql.Connection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;

import play.db.NamedDatabase;
import play.db.Database;

class JavaJdbcConnection {
    private Database db;
    private DatabaseExecutionContext executionContext;

    @Inject // inject "orders" database instead of "default"
    public JavaJdbcConnection(@NamedDatabase("orders") Database db, DatabaseExecutionContext executionContext) {
        this.db = db;
        this.executionContext = executionContext;
    }

    public CompletionStage<Void> updateSomething() {
        return CompletableFuture.runAsync(() -> {
            // get jdbc connection
            Connection connection = db.getConnection();

            // do whatever you need with the db connection
            return;
        }, executionContext);
    }

}
//#java-jdbc-connection
