/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers.module;

import play.mvc.*;

public class ModuleController extends Controller {
  public Result index(Http.Request req) {
    return ok(req.uri());
  }
}
