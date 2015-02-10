/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import play.api.mvc.RequestHeader
import play.twirl.api.JavaScript
import play.api.routing._

@deprecated("Use play.api.routing.Router instead", "2.4.0")
object Routes {

  @deprecated("Use play.api.routing.Router.Tags.RouteVerb instead", "2.4.0")
  val ROUTE_VERB = Router.Tags.RouteVerb
  @deprecated("Use play.api.routing.Router.Tags.RouteVerb instead", "2.4.0")
  val ROUTE_PATTERN = Router.Tags.RoutePattern
  @deprecated("Use play.api.routing.Router.Tags.RouteVerb instead", "2.4.0")
  val ROUTE_CONTROLLER = Router.Tags.RouteController
  @deprecated("Use play.api.routing.Router.Tags.RouteVerb instead", "2.4.0")
  val ROUTE_ACTION_METHOD = Router.Tags.RouteActionMethod
  @deprecated("Use play.api.routing.Router.Tags.RouteVerb instead", "2.4.0")
  val ROUTE_COMMENTS = Router.Tags.RouteComments

  @deprecated("Use play.api.routing.JavaScriptReverseRouter instead", "2.4.0")
  def javascriptRouter(name: String = "Router", ajaxMethod: Option[String] = Some("jQuery.ajax"))(routes: JavaScriptReverseRoute*)(implicit request: RequestHeader): JavaScript = {
    JavaScriptReverseRouter(name, ajaxMethod)(routes: _*)
  }

  @deprecated("Use play.api.routing.JavaScriptReverseRouter instead", "2.4.0")
  def javascriptRouter(name: String, ajaxMethod: Option[String], host: String, routes: JavaScriptReverseRoute*): JavaScript = {
    JavaScriptReverseRouter(name, ajaxMethod, host, routes: _*)
  }

}
