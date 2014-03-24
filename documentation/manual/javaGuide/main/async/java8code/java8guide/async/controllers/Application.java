/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.async.controllers;

import play.mvc.Result;
import play.libs.F.Promise;
import play.mvc.Controller;

public class Application extends Controller {
    //#async
    public static Promise<Result> index() {
      return Promise.promise(() -> intensiveComputation())
                    .map((Integer i) -> ok("Got result: " + i));
    }
    //#async
    public static int intensiveComputation() { return 2;}
}
