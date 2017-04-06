/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class HomeController extends Controller {
    public Result index() {
        return ok("Hello");
    }
}
