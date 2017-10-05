/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
//#relative-controller
//###replace: package controllers;
package javaguide.http.routing.reverse.controllers;

import play.*
import play.mvc.*;

public class Relative extends Controller {

    public Result helloview() {
        return ok(views.html.hello.render("Bob", request()));
    }

    public Result hello(String name) {
        return ok("Hello " + name + "!");
    }

}
