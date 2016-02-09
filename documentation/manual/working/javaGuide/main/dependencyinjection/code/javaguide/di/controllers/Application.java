/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.di.controllers;

import play.mvc.*;

public class Application extends Controller {
    public Result index() {
        return ok();
    }
}
