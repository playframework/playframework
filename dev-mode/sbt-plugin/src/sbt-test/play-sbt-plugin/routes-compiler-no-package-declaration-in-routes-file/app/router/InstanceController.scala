/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package router

import play.api.mvc._
import javax.inject.Inject

class InstanceController @Inject()(c: ControllerComponents) extends AbstractController(c) {
  var invoked = 0

  def index = Action {
    invoked += 1
    Ok(invoked.toString)
  }
}
