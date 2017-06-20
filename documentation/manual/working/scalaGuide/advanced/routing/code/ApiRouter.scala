/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
//#inject-sird-router
package api

import javax.inject.Inject

import play.api.mvc._
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

class ApiRouter @Inject()(controller: ApiController)
  extends SimpleRouter
{
  override def routes: Routes = {
    case GET(p"/") => controller.index
  }
}
//#inject-sird-router

class ApiController @Inject()(cc:ControllerComponents) extends AbstractController(cc) {
  def index() = TODO
}