/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.async.controllers;

import play.mvc.Result;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Controller;

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
    public static int intensiveComputation() { return 2;}
}
