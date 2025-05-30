/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

//#api-sird-router
package api

import jakarta.inject.Inject
import play.api.mvc._
import play.api.routing.sird._
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter

class ApiRouter @Inject() (controller: ApiController) extends SimpleRouter {
  override def routes: Routes = {
    case GET(p"/") => controller.index
  }
}
//#api-sird-router

//#spa-sird-router
class SpaRouter @Inject() (controller: SinglePageApplicationController) extends SimpleRouter {
  override def routes: Routes = {
    case GET(p"/api") => controller.api
  }
}
//#spa-sird-router

//#composed-sird-router
class AppRouter @Inject() (spaRouter: SpaRouter, apiRouter: ApiRouter) extends SimpleRouter {
  // Composes both routers with spaRouter having precedence.
  override def routes: Routes = spaRouter.routes.orElse(apiRouter.routes)
}
//#composed-sird-router

class ApiController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {
  def index = TODO
}

class SinglePageApplicationController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {
  def api = TODO
}
