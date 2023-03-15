/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package router

import javax.inject.Inject

import play.api.mvc._
import play.api.routing.Router
import play.api.routing.SimpleRouter
import play.api.routing.sird._

class Routes @Inject()(controller: controllers.HomeController) extends SimpleRouter {
  override def routes: Router.Routes = {
    case GET(p"/1" ? q"a=${a}b=${b}") => controller.index
    case GET(p"/2" ? q"a=") => controller.index
    case GET(p"/3" ? q"a") => controller.index
    case GET(p"/4" ? q"a=${a}b") => controller.index
  }
}
