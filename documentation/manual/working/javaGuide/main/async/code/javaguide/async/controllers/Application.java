/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.async.controllers;

import play.mvc.Result;
import play.mvc.Controller;

// #async-explicit-ec-imports
import play.libs.concurrent.HttpExecution;

import javax.inject.Inject;
import java.util.concurrent.Executor;
import java.util.concurrent.CompletionStage;
import static java.util.concurrent.CompletableFuture.supplyAsync;
// #async-explicit-ec-imports

// #async-explicit-ec
public class Application extends Controller {

  private MyExecutionContext myExecutionContext;

  @Inject
  public Application(MyExecutionContext myExecutionContext) {
    this.myExecutionContext = myExecutionContext;
  }

  public CompletionStage<Result> index() {
    // Wrap an existing thread pool, using the context from the current thread
    Executor myEc = HttpExecution.fromThread((Executor) myExecutionContext);
    return supplyAsync(() -> intensiveComputation(), myEc)
        .thenApplyAsync(i -> ok("Got result: " + i), myEc);
  }

  public int intensiveComputation() {
    return 2;
  }
}
// #async-explicit-ec
