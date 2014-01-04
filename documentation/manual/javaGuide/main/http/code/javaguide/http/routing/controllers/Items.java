/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http.routing.controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class Items extends Controller {
    public static Result show(Long id) {
        return ok("showing item " + id);
    }
}
