/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.advanced.httprequesthandlers

package simple {

//#simple
import javax.inject.Inject
import play.api.http._
import play.api.mvc._
import play.core.Router

class SimpleHttpRequestHandler @Inject() (routes: Router.Routes) extends HttpRequestHandler {
  def handlerForRequest(request: RequestHeader) = {
    routes.routes.lift(request) match {
      case Some(handler) => (request, handler)
      case None => (request, Action(Results.NotFound))
    }
  }
}
//#simple
}

package virtualhost {

import play.core.Router
object bar {
  type Routes = Router.Routes
}
object foo {
  type Routes = Router.Routes
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
