package router

import javax.inject.Inject

import play.api.mvc._
import play.api.routing.Router
import play.api.routing.SimpleRouter
import play.api.routing.sird._

class Routes @Inject()(controller: controllers.HomeController) extends SimpleRouter {
  override def routes: Router.Routes = {
    case GET(p"/") => controller.index
  }
}
