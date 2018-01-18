/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
//#controller
//###replace: package controllers;
package javaguide.http.routing.reverse.controllers;

import play.mvc.*;

public class Application extends BaseController {

    public Result hello(String name) {
        return ok("Hello " + name + "!");
    }

}
//#controller
