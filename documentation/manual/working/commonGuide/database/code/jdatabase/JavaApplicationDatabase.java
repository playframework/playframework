/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package jdatabase;

// #java-jdbc-database
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.*;
import play.db.*;

@Singleton
class JavaApplicationDatabase {

  private Database db;
  private DatabaseExecutionContext executionContext;

  @Inject
  public JavaApplicationDatabase(Database db, DatabaseExecutionContext context) {
    this.db = db;
    this.executionContext = executionContext;
  }

  public CompletionStage<Integer> updateSomething() {
    return CompletableFuture.supplyAsync(
        () -> {
          return db.withConnection(
              connection -> {
                // do whatever you need with the db connection
                return 1;
              });
        },
        executionContext);
  }
}
// #java-jdbc-database
