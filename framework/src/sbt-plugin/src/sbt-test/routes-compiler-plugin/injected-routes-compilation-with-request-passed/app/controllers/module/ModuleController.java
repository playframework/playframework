/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers.module;

import play.mvc.*;

public class ModuleController extends Controller {
  public Result index(Http.Request req) {
    return ok(req.uri());
  }
}
