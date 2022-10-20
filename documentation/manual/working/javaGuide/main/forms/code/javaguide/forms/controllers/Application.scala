/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms.controllers

import javax.inject.Inject

import play.api.mvc._

class Application @Inject() (components: ControllerComponents) extends AbstractController(components) {
  def submit = Action(Ok)
}
