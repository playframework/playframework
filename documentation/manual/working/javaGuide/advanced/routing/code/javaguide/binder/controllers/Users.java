/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.binder.controllers;

import play.mvc.BaseController;
import play.mvc.Result;

public class Users extends BaseController {

    public Result list() {
        return ok("List Users");
    }

    public Result get(Long id) {
        return ok("Get user by id");
    }
}
