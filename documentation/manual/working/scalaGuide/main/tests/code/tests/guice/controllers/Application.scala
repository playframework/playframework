/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.tests.guice
package controllers

// #controller
import play.api.mvc._
import javax.inject.Inject

class Application @Inject()(component: Component, cc: ControllerComponents) extends AbstractController(cc) {
  def index() = Action {
    Ok(component.hello)
  }
}
// #controller
