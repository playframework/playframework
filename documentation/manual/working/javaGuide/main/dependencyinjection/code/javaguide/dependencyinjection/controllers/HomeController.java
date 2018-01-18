/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.dependencyinjection.controllers;

import play.mvc.BaseController;
import play.mvc.Result;

public class HomeController extends BaseController {
    public Result index() {
        return ok();
    }
}
