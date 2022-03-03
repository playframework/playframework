/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class HomeController extends Controller {
    public Result controllerFail() {
        throw new RuntimeException("Exception thrown in controller");
    }

    public Result subProjectInsideFail() {
        inside.Foo.fail();
        return ok("should not reach this line");
    }

    public Result subProjectOutsideFail() {
        outside.Bar.fail();
        return ok("should not reach this line");
    }
}
