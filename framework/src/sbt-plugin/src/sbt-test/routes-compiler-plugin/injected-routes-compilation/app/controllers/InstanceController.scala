/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
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
