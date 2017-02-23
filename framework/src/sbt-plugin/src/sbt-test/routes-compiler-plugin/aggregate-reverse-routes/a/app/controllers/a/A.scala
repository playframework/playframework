/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers.a

import play.api.mvc._

class A extends Controller {

  def index = Action {
    controllers.a.routes.A.index
    controllers.b.routes.B.index
    controllers.c.routes.C.index
    Ok
  }

}
