/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.http.routing.controllers;

import java.util.Optional;

import play.mvc.BaseController;
import play.mvc.Result;

public class Api extends BaseController {
    public Result list(String version) {
        return ok("version " + version);
    }

    public Result listOpt(Optional<String> version) {
        return ok("version " + version.orElse("unknown"));
    }

    public Result newThing() {
        return ok();
    }
}
