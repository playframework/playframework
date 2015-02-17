/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.async.controllers;

import play.mvc.Result;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Controller;
//#async-explicit-ec-imports
import play.libs.HttpExecution;
import scala.concurrent.ExecutionContext;
//#async-explicit-ec-imports

public class Application extends Controller {
  //#async
  public Promise<Result> index() {
    return Promise.promise(() -> intensiveComputation())
            .map((Integer i) -> ok("Got result: " + i));
  }
  //#async

  private ExecutionContext myThreadPool = null;

  //#async-explicit-ec
  public Promise<Result> index2() {
    // Wrap an existing thread pool, using the context from the current thread
    ExecutionContext myEc = HttpExecution.fromThread(myThreadPool);
    return Promise.promise(() -> intensiveComputation(), myEc)
            .map((Integer i) -> ok("Got result: " + i), myEc);
  }
  //#async-explicit-ec

    public int intensiveComputation() { return 2;}
}
