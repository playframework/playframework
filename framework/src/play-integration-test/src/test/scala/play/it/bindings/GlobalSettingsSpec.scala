/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.bindings

import java.lang.reflect.Method
import java.util.concurrent.CompletableFuture

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.routing.Router
import play.api.Application
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test._
import play.it._
import play.it.http.{ MockController, JAction }
import play.mvc.Http
import play.mvc.Http.Context

object NettyGlobalSettingsSpec extends GlobalSettingsSpec with NettyIntegrationSpecification

trait GlobalSettingsSpec extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {

  sequential

  def withServer[T](applicationGlobal: Option[String])(uri: String)(block: String => T) = {
    implicit val port = testServerPort
    val additionalSettings = applicationGlobal.fold(Map.empty[String, String]) { s: String =>
      Map("application.global" -> s"play.it.bindings.$s")
    } + ("play.http.requestHandler" -> "play.http.GlobalSettingsHttpRequestHandler")
    import play.api.inject._
    import play.api.routing.sird._
    lazy val app: Application = new GuiceApplicationBuilder()
      .configure(additionalSettings)
      .overrides(bind[Router].to(Router.from {
        case p"/scala" => Action { request =>
          Ok(request.headers.get("X-Foo").getOrElse("null"))
        }
        case p"/java" => JAction(app, JavaAction)
      })).build()
    running(TestServer(port, app)) {
      val response = await(wsUrl(uri).get())
      block(response.body)
    }
  }

  "GlobalSettings filters" should {
    "not have X-Foo header when no Global is configured" in withServer(None)("/scala") { body =>
      body must_== "null"
    }
    "have X-Foo header when Scala Global with filters is configured" in withServer(Some("FooFilteringScalaGlobal"))("/scala") { body =>
      body must_== "filter-constructor-called-by-scala-global"
    }
    "have X-Foo header when Java Global with filters is configured" in withServer(Some("FooFilteringJavaGlobal"))("/scala") { body =>
      body must_== "filter-default-constructor"
    }
    "allow intercepting by Java GlobalSettings.onRequest" in withServer(Some("OnRequestJavaGlobal"))("/java") { body =>
      body must_== "intercepted"
    }
  }

}

/** Inserts an X-Foo header with a custom value. */
class FooFilter(headerValue: String) extends EssentialFilter {
  def this() = this("filter-default-constructor")
  def apply(next: EssentialAction) = EssentialAction { request =>
    val fooBarHeaders = request.copy(headers = request.headers.add("X-Foo" -> headerValue))
    next(fooBarHeaders)
  }

}

/** Scala GlobalSettings object that uses a filter */
object FooFilteringScalaGlobal extends play.api.GlobalSettings {
  override def doFilter(next: EssentialAction): EssentialAction = {
    Filters(super.doFilter(next), new FooFilter("filter-constructor-called-by-scala-global"))
  }
}

/** Java GlobalSettings class that uses a filter */
class FooFilteringJavaGlobal extends play.GlobalSettings {
  override def filters[T]() = Array[Class[T]](classOf[FooFilter].asInstanceOf[Class[T]])
}

class OnRequestJavaGlobal extends play.GlobalSettings {
  override def onRequest(request: Http.Request, actionMethod: Method) = {
    new play.mvc.Action.Simple {
      def call(ctx: Context) = CompletableFuture.completedFuture(play.mvc.Results.ok("intercepted"))
    }
  }
}

object JavaAction extends MockController {
  def action = play.mvc.Results.ok(Option(request.getHeader("X-Foo")).getOrElse("null"))
}
