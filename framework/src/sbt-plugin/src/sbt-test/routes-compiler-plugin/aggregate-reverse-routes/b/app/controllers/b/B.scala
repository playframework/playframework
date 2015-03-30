/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package controllers.b

import play.api.mvc._

class B extends Controller {

  def index = Action {
    controllers.a.routes.A.index
    controllers.b.routes.B.index
    controllers.c.routes.C.index
    Ok
  }

}