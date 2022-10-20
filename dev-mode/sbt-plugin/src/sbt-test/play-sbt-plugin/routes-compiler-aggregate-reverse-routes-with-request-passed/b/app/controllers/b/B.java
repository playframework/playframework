/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers.b;

import play.mvc.*;

public class B extends Controller {

  public Result index(Http.Request req) {
    controllers.a.routes.A.index();
    controllers.b.routes.B.index();
    controllers.c.routes.C.index();
    return ok();
  }

}
