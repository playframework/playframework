/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.routing

import java.util.concurrent.CompletionStage

import scala.annotation.varargs
import scala.collection.mutable.ArrayBuffer
import scala.runtime.AbstractPartialFunction

import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.routing.HandlerDef
import play.api.routing.Router
import play.core.routing.JavaRouteActions._
import play.mvc.{ Result => JResult }
import play.mvc.{ WebSocket => JWebSocket }
import play.mvc.Http.{ Request => JRequest }

/**
 * The base class of the routers that the routes compiler generates as Java source code.
 *
 * The constructor of a generated router registers the routes and includes of the routes file in their order. This
 * class matches, binds and invokes them like a router generated as Scala source code does.
 *
 * @param errorHandler The error handler that renders the errors of requests that fail to bind.
 * @param prefix The prefix of this router.
 * @param routerPackage The package of the generated router.
 */
abstract class JavaGeneratedRouter(
    override val errorHandler: HttpErrorHandler,
    val prefix: String,
    routerPackage: String
) extends GeneratedRouter {
  import JavaGeneratedRouter._

  private val rules = ArrayBuffer.empty[Rule]

  final override val routes: Router.Routes = new AbstractPartialFunction[RequestHeader, Handler] {
    override def isDefinedAt(request: RequestHeader): Boolean = rules.exists(_.isDefinedAt(request))

    override def applyOrElse[A1 <: RequestHeader, B1 >: Handler](request: A1, default: A1 => B1): B1 =
      rules.iterator
        .map(_.handlerFor(request))
        .collectFirst { case Some(handler) => handler }
        .getOrElse(default(request))
  }

  final override def documentation: Seq[(String, String, String)] = rules.toSeq.flatMap(_.documentation)

  /**
   * The separator between the prefix of this router and the paths of its routes.
   */
  protected final def defaultPrefix: String = if (prefix.endsWith("/")) "" else "/"

  /**
   * Appends the given path to the prefix of this router, for the prefix of an included router.
   */
  protected final def prefixed(path: String): String = prefix + defaultPrefix + path

  protected final def staticPart(value: String): PathPart = StaticPart(value)

  protected final def dynamicPart(name: String, constraint: String, encodeable: Boolean): PathPart =
    DynamicPart(name, constraint, encodeable)

  /**
   * Starts a route of this router. The route is registered when its builder gets the controller method call.
   *
   * @param verb The HTTP method of the route.
   * @param path The path of the route in the routes file, without the prefix of this router.
   * @param parts The parts of the path, without the prefix of this router.
   */
  @varargs
  protected final def route(verb: String, path: String, parts: PathPart*): RouteBuilder =
    new RouteBuilder(verb, path, parts.toList)

  /**
   * Includes the given router, which already has the prefix of the include.
   */
  protected final def include(router: Router): Unit = rules += new IncludeRule(Include(router))

  protected final def pathParam[T](name: String, binder: PathBindable[T]): ParamBinding =
    new ParamBinding(_.fromPath(name, None)(binder))

  protected final def pathParam[T](name: String, binder: PathBindable[T], default: T): ParamBinding =
    new ParamBinding(_.fromPath(name, Some(default))(binder))

  protected final def queryParam[T](name: String, binder: QueryStringBindable[T]): ParamBinding =
    new ParamBinding(_.fromQuery(name, None)(binder))

  protected final def queryParam[T](name: String, binder: QueryStringBindable[T], default: T): ParamBinding =
    new ParamBinding(_.fromQuery(name, Some(default))(binder))

  /**
   * Builds a route of this router.
   *
   * The overloads of `to` accept the controller method call, so that the Java compiler checks the return type of
   * the controller method.
   */
  final class RouteBuilder private[JavaGeneratedRouter] (verb: String, path: String, parts: List[PathPart]) {
    private var controller     = ""
    private var method         = ""
    private var parameterTypes = Seq.empty[Class[?]]
    private var call           = ""
    private var comments       = ""
    private var modifiers      = Seq.empty[String]
    private var params         = List.empty[ParamBinding]

    /**
     * The controller method of the route, with the types of all its parameters.
     */
    @varargs
    def handler(controller: String, method: String, parameterTypes: Class[?]*): RouteBuilder = {
      this.controller = controller
      this.method = method
      this.parameterTypes = parameterTypes.toList
      this
    }

    /**
     * The controller method call of the route as the documentation of the router shows it.
     */
    def documentation(call: String): RouteBuilder = {
      this.call = call
      this
    }

    def comments(comments: String): RouteBuilder = {
      this.comments = comments
      this
    }

    @varargs
    def modifiers(modifiers: String*): RouteBuilder = {
      this.modifiers = modifiers.toList
      this
    }

    /**
     * The parameters of the route that are bound from the request, in the order of the controller method.
     */
    @varargs
    def params(params: ParamBinding*): RouteBuilder = {
      this.params = params.toList
      this
    }

    def to(action: ResultAction): Unit = {
      val invoker = invokerFor(HandlerInvokerFactory.wrapJava)
      register(args => invoker.call(action.call(args)))
    }

    def to(action: AsyncResultAction): Unit = {
      val invoker = invokerFor(HandlerInvokerFactory.wrapJavaPromise)
      register(args => invoker.call(action.call(args)))
    }

    def to(action: RequestResultAction): Unit = {
      val invoker = invokerFor(HandlerInvokerFactory.wrapJavaRequest)
      register(args => invoker.call((request: JRequest) => action.call(request, args)))
    }

    def to(action: AsyncRequestResultAction): Unit = {
      val invoker = invokerFor(HandlerInvokerFactory.wrapJavaPromiseRequest)
      register(args => invoker.call((request: JRequest) => action.call(request, args)))
    }

    def to(action: WebSocketAction): Unit = {
      val invoker = invokerFor(HandlerInvokerFactory.javaWebSocket)
      register(args => invoker.call(action.call(args)))
    }

    def to(action: HandlerAction): Unit = {
      val invoker = invokerFor(HandlerInvokerFactory.passThrough[Handler])
      register(args => invoker.call(action.call(args)))
    }

    private def invokerFor[T](factory: HandlerInvokerFactory[T]): HandlerInvoker[T] =
      createInvoker[T](
        fakeValue[T],
        HandlerDef(
          JavaGeneratedRouter.this.getClass.getClassLoader,
          routerPackage,
          controller,
          method,
          parameterTypes,
          verb,
          prefix + path,
          comments,
          modifiers
        )
      )(factory)

    private def register(handler: Array[AnyRef] => Handler): Unit = {
      val pathPattern = PathPattern(
        StaticPart(prefix) :: (if (parts.isEmpty) Nil else StaticPart(defaultPrefix) :: parts)
      )
      val docPath = if (parts.isEmpty) prefix else prefix + defaultPrefix + path
      rules += new RouteRule(Route(verb, pathPattern), params, handler, (verb, docPath, call))
    }
  }

  private sealed trait Rule {
    def isDefinedAt(request: RequestHeader): Boolean
    def handlerFor(request: RequestHeader): Option[Handler]
    def documentation: Seq[(String, String, String)]
  }

  private final class RouteRule(
      extractor: Route.ParamsExtractor,
      params: List[ParamBinding],
      handler: Array[AnyRef] => Handler,
      doc: (String, String, String)
  ) extends Rule {
    def isDefinedAt(request: RequestHeader): Boolean = extractor.unapply(request).isDefined

    def handlerFor(request: RequestHeader): Option[Handler] =
      extractor.unapply(request).map { routeParams =>
        call(params.map(_.bind(routeParams)))(values => handler(values.map(_.asInstanceOf[AnyRef]).toArray))
      }

    def documentation: Seq[(String, String, String)] = Seq(doc)
  }

  private final class IncludeRule(include: Include) extends Rule {
    def isDefinedAt(request: RequestHeader): Boolean        = include.unapply(request).isDefined
    def handlerFor(request: RequestHeader): Option[Handler] = include.unapply(request)
    def documentation: Seq[(String, String, String)]        = include.router.documentation
  }
}

object JavaGeneratedRouter {

  /**
   * Binds a parameter of a route from the request.
   */
  final class ParamBinding private[routing] (private[routing] val bind: RouteParams => Param[?])
}
