/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
//#controller
//###replace: package controllers;
package javaguide.http.routing.reverse.controllers;

import play.*;
import play.mvc.*;

public class Application extends Controller {

    public Result hello(String name) {
        return ok("Hello " + name + "!");
    }

}
//#controller
