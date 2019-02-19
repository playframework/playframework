/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers.a;

import play.mvc.*;

public class A extends Controller {

  public Result index(Http.Request req) {
    controllers.a.routes.A.index();
    controllers.b.routes.B.index();
    controllers.c.routes.C.index();
    return ok();
  }

}
