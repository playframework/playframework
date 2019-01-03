/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.routing

import play.api.libs.typedmap.TypedKey
import play.api.{ Configuration, Environment }
import play.api.mvc.{ Handler, RequestHeader }
import play.api.routing.Router.Routes
import play.core.j.JavaRouterAdapter
import play.utils.Reflect

/**
 * A router.
 */
trait Router {
  self =>
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
   * Get a new router that routes requests to `s"$prefix/$path"` in the same way this router routes requests to `path`.
   *
   * @return the prefixed router
   */
  def withPrefix(prefix: String): Router

  /**
   * An alternative syntax for `withPrefix`. For example:
   *
   * {{{
   *   val router = "/bar" /: barRouter
   * }}}
   */
  final def /:(prefix: String): Router = withPrefix(prefix)

  /**
   * A lifted version of the routes partial function.
   */
  final def handlerFor(request: RequestHeader): Option[Handler] = {
    routes.lift(request)
  }

  def asJava: play.routing.Router = new JavaRouterAdapter(this)

  /**
   * Compose two routers into one. The resulting router will contain
   * both the routes in `this` as well as `router`
   */
  final def orElse(other: Router): Router = new Router {
    def documentation: Seq[(String, String, String)] = self.documentation ++ other.documentation
    def withPrefix(prefix: String): Router = self.withPrefix(prefix).orElse(other.withPrefix(prefix))
    def routes: Routes = self.routes.orElse(other.routes)
  }

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

  object RequestImplicits {
    import play.api.mvc.RequestHeader

    implicit class WithHandlerDef(val request: RequestHeader) extends AnyVal {
      /**
       * The [[HandlerDef]] representing the routes file entry (if any) on this request.
       */
      def handlerDef: Option[HandlerDef] = request.attrs.get(Attrs.HandlerDef)

      /**
       * Check if the route for this request has the given modifier tag (case insensitive).
       *
       * This can be used by a filter to change behavior.
       */
      def hasRouteModifier(modifier: String): Boolean =
        handlerDef.exists(_.modifiers.exists(modifier.equalsIgnoreCase))
    }
  }

  /**
   * Request attributes used by the router.
   */
  object Attrs {
    /**
     * Key for the [[HandlerDef]] used to handle the request.
     */
    val HandlerDef: TypedKey[HandlerDef] = TypedKey("HandlerDef")
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

  /**
   * Concatenate another prefix with an existing prefix, collapsing extra slashes. If the existing prefix is empty or
   * "/" then the new prefix replaces the old one. Otherwise the new prefix is prepended to the old one with a slash in
   * between, ignoring a final slash in the new prefix or an initial slash in the existing prefix.
   */
  def concatPrefix(newPrefix: String, existingPrefix: String): String = {
    if (existingPrefix.isEmpty || existingPrefix == "/") {
      newPrefix
    } else {
      newPrefix.stripSuffix("/") + "/" + existingPrefix.stripPrefix("/")
    }
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
      val prefixTrailingSlash = if (prefix endsWith "/") prefix else prefix + "/"
      val prefixed: PartialFunction[RequestHeader, RequestHeader] = {
        case rh: RequestHeader if rh.path == prefix || rh.path.startsWith(prefixTrailingSlash) =>
          val newPath = "/" + rh.path.drop(prefixTrailingSlash.length)
          rh.withTarget(rh.target.withPath(newPath))
      }
      new Router {
        def routes = Function.unlift(prefixed.lift.andThen(_.flatMap(self.routes.lift)))
        def withPrefix(p: String) = self.withPrefix(Router.concatPrefix(p, prefix))
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
