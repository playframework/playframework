/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package controllers

import play.api.mvc._

class InstanceController extends Controller {
  var invoked = 0

  def index = Action {
    invoked += 1
    Ok(invoked.toString)
  }
}
