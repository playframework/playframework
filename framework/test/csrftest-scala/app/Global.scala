
import play.api.mvc._
import play.api._

import play.api.filters._
import play.api.csrf._

object Global extends WithFilters(CSRFFilter()) with GlobalSettings /*{
  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    Some(Action{Results.BadRequest.withSession("test" -> "bordel")})
  }
}*/