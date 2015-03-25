/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package controllers.c

import play.api.mvc._

class C extends Controller {

  def index = Action {
    controllers.a.routes.A.index()
    controllers.b.routes.B.index()
    controllers.c.routes.C.index()
    Ok
  }

}