/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

//#full-controller
//###replace: package controllers;
package javaguide.http.full;

import play.*;
import play.mvc.*;

public class Application extends Controller {

    public Result index() {
        return ok("It works!");
    }

}
//#full-controller
