/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package jdatabase;

// #java-jdbc-connection
import java.sql.Connection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.db.Database;

class JavaJdbcConnection {
  private Database db;
  private DatabaseExecutionContext executionContext;

  @Inject
  public JavaJdbcConnection(Database db, DatabaseExecutionContext executionContext) {
    this.db = db;
    this.executionContext = executionContext;
  }

  public CompletionStage<Void> updateSomething() {
    return CompletableFuture.runAsync(
        () -> {
          // get jdbc connection
          Connection connection = db.getConnection();

          // do whatever you need with the db connection
          return;
        },
        executionContext);
  }
}
// #java-jdbc-connection
