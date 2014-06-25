/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.async.controllers;

import play.mvc.Result;
import play.libs.HttpExecution;
import play.libs.F.Promise;
import play.mvc.Controller;
import scala.concurrent.ExecutionContext;

public class Application extends Controller {
    //#async
    public static Promise<Result> index() {
      return Promise.promise(() -> intensiveComputation())
                    .map((Integer i) -> ok("Got result: " + i));
    }
    //#async

    private static ExecutionContext myThreadPool = null;

    //#async-explicit-ec
    public static Promise<Result> index2() {
      // Wrap an existing thread pool, using the context from the current thread
      ExecutionContext myEc = HttpExecution.fromThread(myThreadPool);
      return Promise.promise(() -> intensiveComputation(), myEc)
                    .map((Integer i) -> ok("Got result: " + i), myEc);
    }
    //#async-explicit-ec

    public static int intensiveComputation() { return 2;}
}
