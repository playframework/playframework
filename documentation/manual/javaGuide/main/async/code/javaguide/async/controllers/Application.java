/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
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
    public static Promise<Result> index() {
      Promise<Integer> promiseOfInt = Promise.promise(
        new Function0<Integer>() {
          public Integer apply() {
            return intensiveComputation();
          }
        }
      );
      return promiseOfInt.map(
        new Function<Integer, Result>() {
          public Result apply(Integer i) {
            return ok("Got result: " + i);
          } 
        }
      );
    }
    //#async

    private static ExecutionContext myThreadPool = null;

    //#async-explicit-ec
    public static Promise<Result> index2() {
      // Wrap an existing thread pool, using the context from the current thread
      ExecutionContext myEc = HttpExecution.fromThread(myThreadPool);
      Promise<Integer> promiseOfInt = Promise.promise(
        new Function0<Integer>() {
          public Integer apply() {
            return intensiveComputation();
          }
        },
        myEc
      );
      return promiseOfInt.map(
        new Function<Integer, Result>() {
          public Result apply(Integer i) {
            return ok("Got result: " + i);
          } 
        },
        myEc
      );
    }
    //#async-explicit-ec

    public static int intensiveComputation() { return 2;}
}
