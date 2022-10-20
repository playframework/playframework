/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.tests.guice
package controllers

// #controller
import play.api.mvc._
import javax.inject.Inject

class Application @Inject() (component: Component, cc: ControllerComponents) extends AbstractController(cc) {
  def index = Action {
    Ok(component.hello)
  }
}
// #controller
