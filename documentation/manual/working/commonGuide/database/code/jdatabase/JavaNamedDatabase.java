/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package jdatabase;

// #java-jdbc-named-database

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.db.Database;
import play.db.NamedDatabase;

@Singleton
class JavaNamedDatabase {
  private Database db;
  private DatabaseExecutionContext executionContext;

  @Inject
  public JavaNamedDatabase(
      // inject "orders" database instead of "default"
      @NamedDatabase("orders") Database db, DatabaseExecutionContext executionContext) {
    this.db = db;
    this.executionContext = executionContext;
  }

  public CompletionStage<Integer> updateSomething() {
    return CompletableFuture.supplyAsync(
        () ->
            db.withConnection(
                connection -> {
                  // do whatever you need with the db connection
                  return 1;
                }),
        executionContext);
  }
}
// #java-jdbc-named-database
