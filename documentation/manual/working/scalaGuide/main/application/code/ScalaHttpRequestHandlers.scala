/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
        case None          => (request, action(Results.NotFound))
      }
    }
  }
//#simple
}

package virtualhost {
  import play.api.mvc.Handler
  import play.api.routing.Router
  import play.api.OptionalDevContext
  import play.core.WebCommands
  object bar {
    type Routes = Router
  }
  object foo {
    type Routes = Router
  }

//#virtualhost
  import javax.inject.Inject
  import javax.inject.Provider

  import play.api.http._
  import play.api.mvc.RequestHeader

  class VirtualHostRequestHandler @Inject() (
      webCommands: WebCommands,
      optionalDevContext: OptionalDevContext,
      errorHandler: HttpErrorHandler,
      configuration: HttpConfiguration,
      filters: HttpFilters,
      fooRouter: Provider[foo.Routes],
      barRouter: Provider[bar.Routes]
  ) extends DefaultHttpRequestHandler(
        webCommands,
        optionalDevContext,
        fooRouter,
        errorHandler,
        configuration,
        filters
      ) {
    override def routeRequest(request: RequestHeader): Option[Handler] = {
      request.host match {
        case "foo.example.com" => fooRouter.get.routes.lift(request)
        case "bar.example.com" => barRouter.get.routes.lift(request)
        case _                 => super.routeRequest(request)
      }
    }
  }
//#virtualhost
}
