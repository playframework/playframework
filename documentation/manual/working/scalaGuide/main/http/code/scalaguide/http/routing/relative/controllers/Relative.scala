/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.http.routing.relative
//#relative-controller
package controllers

import javax.inject._

import play.api.mvc._

@Singleton
class Relative @Inject() (cc: ControllerComponents) extends AbstractController(cc) {
  def helloview: Action[AnyContent] = Action { implicit request => Ok(views.html.hello("Bob")) }

  def hello(name: String): Action[AnyContent] = Action {
    Ok(s"Hello $name!")
  }
}
