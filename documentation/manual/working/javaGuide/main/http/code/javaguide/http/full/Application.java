/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
//#full-controller
//###replace: package controllers;
package javaguide.http.full;

import play.mvc.*;

public class Application extends BaseController {

    public Result index() {
        return ok("It works!");
    }

}
//#full-controller
