/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.routing

import play.api.{ PlayConfig, Configuration, Environment }
import play.api.mvc.{ RequestHeader, Handler }
import play.utils.Reflect

/**
 * A router.
 */
trait Router {

  /**
   * The actual routes of the router.
   */
  def routes: PartialFunction[RequestHeader, Handler]

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
}

/**
 * Utilities for routing.
 */
object Router {

  /**
   * Try to load the configured router class.
   *
   * @return The router class if configured or if a default one in the root package was detected.
   */
  def load(env: Environment, configuration: Configuration): Option[Class[_ <: Router]] = {
    val className = PlayConfig(configuration).getOptionalDeprecated[String]("play.http.router", "application.router")

    try {
      Some(Reflect.getClass[Router](className.getOrElse("Routes"), env.classLoader))
    } catch {
      case e: ClassNotFoundException =>
        // Only throw an exception if a router was explicitly configured, but not found.
        // Otherwise, it just means this application has no router, and that's ok.
        className.map { routerName =>
          throw configuration.reportError("application.router", "Router not found: " + routerName)
        }
    }
  }

  /** Tags that are added to requests by the router. */
  object Tags {
    /** The verb that the router matched */
    val RouteVerb = "ROUTE_VERB"
    /** The pattern that the router used to match the path */
    val RoutePattern = "ROUTE_PATTERN"
    /** The controller that was routed to */
    val RouteController = "ROUTE_CONTROLLER"
    /** The method on the controller that was invoked */
    val RouteActionMethod = "ROUTE_ACTION_METHOD"
    /** The comments in the routes file that were above the route */
    val RouteComments = "ROUTE_COMMENTS"
  }

  /**
   * Create a new router from the given partial function
   *
   * @param routes The routes partial function
   * @return A router that uses that partial function
   */
  def from(routes: PartialFunction[RequestHeader, Handler]): Router = new SimpleRouter(routes, "/")

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
 * A simple router that is based on a PartialFunction of request headers to handlers.
 *
 * @param suppliedRoutes The partial function that does the routing.
 * @param prefix The prefix for the router.
 */
class SimpleRouter(suppliedRoutes: PartialFunction[RequestHeader, Handler],
    prefix: String) extends Router {
  def routes = {
    if (prefix == "/") {
      suppliedRoutes
    } else {
      val p = if (prefix.endsWith("/")) prefix else prefix + "/"
      val prefixed: PartialFunction[RequestHeader, RequestHeader] = {
        case rh: RequestHeader if rh.path.startsWith(p) => rh.copy(path = rh.path.drop(p.length - 1))
      }
      Function.unlift(prefixed.lift.andThen(_.flatMap(suppliedRoutes.lift)))
    }
  }
  def documentation = Nil
  def withPrefix(prefix: String) = new SimpleRouter(suppliedRoutes, prefix)
}