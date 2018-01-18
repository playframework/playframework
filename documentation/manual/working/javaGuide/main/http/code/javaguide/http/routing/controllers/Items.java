/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.http.routing.controllers;

import play.mvc.BaseController;
import play.mvc.Result;

public class Items extends BaseController {
    public Result show(Long id) {
        return ok("showing item " + id);
    }
}
