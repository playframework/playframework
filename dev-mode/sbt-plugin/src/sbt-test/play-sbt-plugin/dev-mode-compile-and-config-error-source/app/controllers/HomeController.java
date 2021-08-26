/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import javax.inject.Inject;

import modules.SomeComponent;

import play.mvc.Controller;
import play.mvc.Result;

public class HomeController extends Controller {

    @Inject
    public HomeController(final SomeComponent someComponent) { }

    public Result index() {
        return ok("app compiles and runs!");
    }
}
