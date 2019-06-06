/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.http.routing.relative
//#relative-controller
package controllers

import javax.inject._
import play.api.mvc._

@Singleton
class Relative @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def helloview() = Action { implicit request =>
    Ok(views.html.hello("Bob"))
  }

  def hello(name: String) = Action {
    Ok(s"Hello $name!")
  }
}
