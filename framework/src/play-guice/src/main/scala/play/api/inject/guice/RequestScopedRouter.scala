package play.api.inject.guice

import com.google.inject.{ AbstractModule, Injector, Module }
import play.api.mvc.RequestHeader
import play.api.routing.Router
import play.api.{ Configuration, Environment }

import scala.reflect.ClassTag

/**
 * A router that creates a new instance of a router from the injector on every request, also the request-scoped
 * dependencies defined in the provided modules.
 *
 * To use, extend this class:
 *
 * {{{
 *   class MyRequestScopedRouter @Inject()(injector: Injector)
 *     extends RequestScopedRouter[router.Routes](injector, modules = Seq(new MyRequestModule))
 * }}}
 *
 * Then add the router to your configuration, e.g. "play.http.router = com.example.MyRequestScopedRouter"
 */
class RequestScopedRouter[T <: Router](
    injector: Injector, prefix: String = "/", modules: Seq[Module] = Seq.empty
)(implicit routerClassTag: ClassTag[T]) extends Router with RequestScopedRouterProvider {

  def this(injector: Injector, routerClass: Class[T], prefix: String, modules: Array[Module]) {
    this(injector, prefix, modules)(ClassTag(routerClass))
  }

  /**
   * Provide a router that is scoped to this request.
   */
  def routerForRequest(request: RequestHeader): Router = {
    val requestScopedModules = new RequestScopedModule(request) +: modules
    injector
      .createChildInjector(requestScopedModules: _*)
      .getInstance(routerClassTag.runtimeClass)
      .asInstanceOf[T]
      .withPrefix(prefix)
  }

  def routes: Router.Routes = Function.unlift { request =>
    val requestScopedRouter = routerForRequest(request)
    requestScopedRouter.routes.lift(request)
  }

  def withPrefix(prefix: String): RequestScopedRouter[T] = new RequestScopedRouter(injector, prefix, modules)

  /**
   * Since we don't have a request yet, we can't get the documentation, so this just returns an empty Seq. To get the
   * route documentation, call routerForRequest(request).documentation
   *
   * @return A list of method, path pattern and controller/method invocations for each route.
   */
  def documentation: Seq[(String, String, String)] = Seq.empty
}

/**
 * A provider that returns a router scoped to a specific request. Useful when you want to access the router's
 * documentation (which is not available from RequestScopedRouter).
 */
trait RequestScopedRouterProvider {
  /**
   * Provide a router that is scoped to this request.
   */
  def routerForRequest(request: RequestHeader): Router
}

private class RequestScopedModule(request: RequestHeader) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[RequestHeader]).toInstance(request)
    bind(classOf[play.mvc.Http.RequestHeader]).toInstance(new play.core.j.RequestHeaderImpl(request))
  }
}
