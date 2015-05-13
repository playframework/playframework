/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.bindings

import org.specs2.mutable.Specification
import play.api.libs.ws._
import play.api.{ Application, Configuration, GlobalSettings }
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test._
import play.it._
import scala.concurrent._

object NettyGlobalSettingsSpec extends GlobalSettingsSpec with NettyIntegrationSpecification

trait GlobalSettingsSpec extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {

  sequential

  def withServer[T](applicationGlobal: Option[String])(block: (Application, String) => T) = {
    implicit val port = testServerPort
    val additionalSettings = applicationGlobal.fold(Map.empty[String, String]) { s: String =>
      Map("application.global" -> s)
    }
    val app = FakeApplication(
      additionalConfiguration = additionalSettings,
      withRoutes = {
        case _ => Action { request =>
          Ok(request.headers.get("X-Foo").toString)
        }
      }
    )
    running(TestServer(port, app)) {
      val response = await(wsUrl("/").get())
      block(app, response.body)
    }
  }

  "GlobalSettings filters" should {
    "not have X-Foo header when no Global is configured" in withServer(None) { (app: Application, body: String) =>
      body must_== "None"
    }
    "have X-Foo header when Scala Global with filters is configured" in withServer(Some("play.it.bindings.FooFilteringScalaGlobal")) { (app: Application, body: String) =>
      body must_== "Some(filter-constructor-called-by-scala-global)"
    }
    "have X-Foo header when Java Global with filters is configured" in withServer(Some("play.it.bindings.FooFilteringJavaGlobal")) { (app: Application, body: String) =>
      body must_== "Some(filter-default-constructor)"
    }
  }

}

/** Inserts an X-Foo header with a custom value. */
class FooFilter(headerValue: String) extends Filter {
  def this() = this("filter-default-constructor")
  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    val fooBarHeaders = rh.copy(headers = rh.headers.add("X-Foo" -> headerValue))
    f(fooBarHeaders)
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
