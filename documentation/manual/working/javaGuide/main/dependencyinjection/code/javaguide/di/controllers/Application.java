/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.controllers;

import play.mvc.*;

public class Application extends BaseController {
    public Result index() {
        return ok();
    }
}
