/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.sql;

import javax.inject.Inject;
import javax.inject.Singleton;

import play.mvc.Controller;
import play.db.NamedDatabase;
import play.db.Database;

// inject "orders" database instead of "default"
@javax.inject.Singleton
class JavaNamedDatabase {
  private Database db;
  private DatabaseExecutionContext executionContext;

  @Inject
  public JavaNamedDatabase(
      @NamedDatabase("orders") Database db, DatabaseExecutionContext executionContext) {
    this.db = db;
    this.executionContext = executionContext;
  }

  // do whatever you need with the db using supplyAsync(() -> { ... }, executionContext);
}
