/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.forms.controllers

import javax.inject.Inject

import play.api.mvc._

class Application @Inject()(components: ControllerComponents) extends AbstractController(components) {
  def submit = Action(Ok)
}
