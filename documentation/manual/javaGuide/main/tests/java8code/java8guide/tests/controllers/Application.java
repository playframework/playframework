/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.tests.controllers;

import play.mvc.*;

public class Application extends Controller {

    public static Result index(String name) {
        return ok(java8guide.tests.html.index.render(name));
    }
}
