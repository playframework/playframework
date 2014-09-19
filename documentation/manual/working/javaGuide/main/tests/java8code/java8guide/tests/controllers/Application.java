/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.tests.controllers;

import play.mvc.*;

public class Application extends Controller {

    public Result index() {
        return ok(java8guide.tests.html.index.render("Welcome to Play!"));
    }
}
