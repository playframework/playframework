/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.tests.guice
package controllers

// #controller
import play.api.mvc._
import javax.inject.Inject

class Application @Inject() (component: Component) extends Controller {
  def index() = Action {
    Ok(component.hello)
  }
}
// #controller
