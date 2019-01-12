/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.advanced.httprequesthandlers

package simple {

//#simple
import javax.inject.Inject
import play.api.http._
import play.api.mvc._
import play.api.routing.Router

class SimpleHttpRequestHandler @Inject() (router: Router, action: DefaultActionBuilder) extends HttpRequestHandler {
  def handlerForRequest(request: RequestHeader) = {
    router.routes.lift(request) match {
      case Some(handler) => (request, handler)
      case None => (request, action(Results.NotFound))
    }
  }
}
//#simple
}

package virtualhost {

import play.api.OptionalDevContext
import play.api.mvc.Handler
import play.api.routing.Router
import play.core.WebCommands
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

class VirtualHostRequestHandler @Inject() (
    webCommands: WebCommands,
    optionalDevContext: OptionalDevContext,
    errorHandler: HttpErrorHandler,
    configuration: HttpConfiguration, filters: HttpFilters,
    fooRouter: foo.Routes, barRouter: bar.Routes
  ) extends DefaultHttpRequestHandler(
    webCommands, optionalDevContext, fooRouter, errorHandler, configuration, filters
  ) {

  override def routeRequest(request: RequestHeader): Option[Handler] = {
    request.host match {
      case "foo.example.com" => fooRouter.routes.lift(request)
      case "bar.example.com" => barRouter.routes.lift(request)
      case _ => super.routeRequest(request)
    }
  }
}
//#virtualhost

}
