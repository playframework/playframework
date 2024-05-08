/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import play.mvc.*;

public class HomeController extends Controller {

    public Result index(Http.Request req) {
        return ok("hello_world_" + req.session().get("foo").orElse("empty")) // This line tests session cookie parsing
            .addingToSession(req, "one", "two"); // This lines tests session cookie creation
    }

}
