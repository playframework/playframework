/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.routing

import play.api.libs.typedmap.TypedKey
import play.api.{ Configuration, Environment }
import play.api.mvc.{ Handler, RequestHeader }
import play.core.j.JavaRouterAdapter
import play.core.routing.HandlerDef
import play.utils.Reflect

/**
 * A router.
 */
trait Router {

  /**
   * The actual routes of the router.
   */
  def routes: Router.Routes

  /**
   * Documentation for the router.
   *
   * @return A list of method, path pattern and controller/method invocations for each route.
   */
  def documentation: Seq[(String, String, String)]

  /**
   * Prefix this router with the given prefix.
   *
   * Should return a new router that uses the prefix, but legacy implementations may just update their existing prefix.
   */
  def withPrefix(prefix: String): Router

  /**
   * A lifted version of the routes partial function.
   */
  def handlerFor(request: RequestHeader): Option[Handler] = {
    routes.lift(request)
  }

  def asJava: play.routing.Router = new JavaRouterAdapter(this)
}

/**
 * Utilities for routing.
 */
object Router {

  /**
   * The type of the routes partial function
   */
  type Routes = PartialFunction[RequestHeader, Handler]

  /**
   * Try to load the configured router class.
   *
   * @return The router class if configured or if a default one in the root package was detected.
   */
  def load(env: Environment, configuration: Configuration): Option[Class[_ <: Router]] = {
    val className = configuration.getDeprecated[Option[String]]("play.http.router", "application.router")

    try {
      Some(Reflect.getClass[Router](className.getOrElse("router.Routes"), env.classLoader))
    } catch {
      case e: ClassNotFoundException =>
        // Only throw an exception if a router was explicitly configured, but not found.
        // Otherwise, it just means this application has no router, and that's ok.
        className.map { routerName =>
          throw configuration.reportError("application.router", "Router not found: " + routerName)
        }
    }
  }

  /**
   * The [[RequestHeader]] attribute key used to access the routing
   * [[HandlerDef]] used to handle the request.
   */
  val HandlerDefAttr = TypedKey[HandlerDef]("HandlerDef")

  /** Tags that are added to requests by the router. */
  @deprecated("Use HandlerDefAttr instead", "2.6.0")
  object Tags {
    /** The verb that the router matched */
    @deprecated("Use HandlerDefAttr instead", "2.6.0")
    val RouteVerb = "ROUTE_VERB"
    /** The pattern that the router used to match the path */
    @deprecated("Use HandlerDefAttr instead", "2.6.0")
    val RoutePattern = "ROUTE_PATTERN"
    /** The controller that was routed to */
    @deprecated("Use HandlerDefAttr instead", "2.6.0")
    val RouteController = "ROUTE_CONTROLLER"
    /** The method on the controller that was invoked */
    @deprecated("Use HandlerDefAttr instead", "2.6.0")
    val RouteActionMethod = "ROUTE_ACTION_METHOD"
    /** The comments in the routes file that were above the route */
    @deprecated("Use HandlerDefAttr instead", "2.6.0")
    val RouteComments = "ROUTE_COMMENTS"
  }

  /**
   * Create a new router from the given partial function
   *
   * @param routes The routes partial function
   * @return A router that uses that partial function
   */
  def from(routes: Router.Routes): Router = SimpleRouter(routes)

  /**
   * An empty router.
   *
   * Never returns an handler from the routes function.
   */
  val empty: Router = new Router {
    def documentation = Nil
    def withPrefix(prefix: String) = this
    def routes = PartialFunction.empty
  }
}

/**
 * A simple router that implements the withPrefix and documentation methods for you.
 */
trait SimpleRouter extends Router { self =>
  def documentation: Seq[(String, String, String)] = Seq.empty
  def withPrefix(prefix: String): Router = {
    if (prefix == "/") {
      self
    } else {
      new Router {
        def routes = {
          val p = if (prefix.endsWith("/")) prefix else prefix + "/"
          val prefixed: PartialFunction[RequestHeader, RequestHeader] = {
            case rh: RequestHeader if rh.path.startsWith(p) => rh.copy(path = rh.path.drop(p.length - 1))
          }
          Function.unlift(prefixed.lift.andThen(_.flatMap(self.routes.lift)))
        }
        def withPrefix(prefix: String) = self.withPrefix(prefix)
        def documentation = self.documentation
      }
    }
  }
}

class SimpleRouterImpl(routesProvider: => Router.Routes) extends SimpleRouter {
  def routes = routesProvider
}

object SimpleRouter {
  /**
   * Create a new simple router from the given routes
   */
  def apply(routes: Router.Routes): Router = new SimpleRouterImpl(routes)
}
