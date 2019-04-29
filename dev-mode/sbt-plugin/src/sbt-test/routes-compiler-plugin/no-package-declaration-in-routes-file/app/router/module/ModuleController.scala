/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package router.module

import play.api.mvc._
import javax.inject.Inject

class ModuleController @Inject()(c: ControllerComponents) extends AbstractController(c) {
  def index = Action {
    Ok
  }
}
