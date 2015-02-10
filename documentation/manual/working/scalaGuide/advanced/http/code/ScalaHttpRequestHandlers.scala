/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.advanced.httprequesthandlers

package simple {

//#simple
import javax.inject.Inject
import play.api.http._
import play.api.mvc._
import play.api.routing.Router

class SimpleHttpRequestHandler @Inject() (router: Router) extends HttpRequestHandler {
  def handlerForRequest(request: RequestHeader) = {
    router.routes.lift(request) match {
      case Some(handler) => (request, handler)
      case None => (request, Action(Results.NotFound))
    }
  }
}
//#simple
}

package virtualhost {

import play.api.routing.Router
object bar {
  type Routes = Router
}
object foo {
  type Routes = Router
}

//#virtualhost
import javax.inject.Inject
import play.api.http._
import play.api.mvc.RequestHeader

class VirtualHostRequestHandler @Inject() (errorHandler: HttpErrorHandler,
    configuration: HttpConfiguration, filters: HttpFilters,
    fooRouter: foo.Routes, barRouter: bar.Routes
  ) extends DefaultHttpRequestHandler(
    fooRouter, errorHandler, configuration, filters
  ) {

  override def routeRequest(request: RequestHeader) = {
    request.host match {
      case "foo.example.com" => fooRouter.routes.lift(request)
      case "bar.example.com" => barRouter.routes.lift(request)
      case _ => super.routeRequest(request)
    }
  }
}
//#virtualhost

}
