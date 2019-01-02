/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csp

import com.typesafe.config.{ ConfigFactory, ConfigRenderOptions }
import javax.inject.Inject
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Handler.Stage
import play.api.mvc.Results._
import play.api.mvc._
import play.api.mvc.request.RequestAttrKey
import play.api.routing.{ HandlerDef, Router }
import play.api.test.Helpers._
import play.api.test._
import play.api.{ Application, Configuration, Environment }

import scala.reflect.ClassTag

class Filters @Inject() (cspFilter: CSPFilter) extends HttpFilters {
  def filters = Seq(cspFilter)
}

class CSPFilterSpec extends PlaySpecification {

  sequential

  def inject[T: ClassTag](implicit app: Application) = app.injector.instanceOf[T]

  def configure(rawConfig: String) = {
    val typesafeConfig = ConfigFactory.parseString(rawConfig)
    Configuration(typesafeConfig)
  }

  def withApplication[T](result: Result, config: String)(block: Application => T): T = {
    val app = new GuiceApplicationBuilder()
      .configure(configure(config))
      .overrides(
        bind[Result].to(result),
        bind[HttpFilters].to[Filters]
      )
      .build
    running(app)(block(app))
  }

  def withApplicationRouter[T](result: Result, config: String, router: Application => PartialFunction[(String, String), Handler])(block: Application => T): T = {
    val app = new GuiceApplicationBuilder()
      .configure(configure(config))
      .overrides(
        bind[Result].to(result),
        bind[HttpFilters].to[Filters]
      ).appRoutes(app => router(app))
      .build
    running(app)(block(app))
  }

  val defaultHocon =
    """
      |play.filters.csp {
      |    nonce.header = true
      |    directives {
      |      object-src = null
      |      base-uri = null
      |    }
      |  }
    """.stripMargin

  val defaultConfig = ConfigFactory.parseString(defaultHocon)

  "filter" should {
    "allow bypassing the CSRF filter using a route modifier tag" in {
      withApplicationRouter(Ok("hello"), defaultHocon, implicit app => {
        case _ =>
          val env = inject[Environment]
          val Action = inject[DefaultActionBuilder]
          new Stage {
            override def apply(requestHeader: RequestHeader): (RequestHeader, Handler) = {
              (requestHeader.addAttr(Router.Attrs.HandlerDef, HandlerDef(
                env.classLoader,
                "routes",
                "FooController",
                "foo",
                Seq.empty,
                "POST",
                "/foo",
                "comments",
                Seq("nocsp", "api")
              )), Action { request =>
                request.body.asFormUrlEncoded
                  .flatMap(_.get("foo"))
                  .flatMap(_.headOption)
                  .map(Results.Ok(_))
                  .getOrElse(Results.NotFound)
              })
            }
          }
      }) { app =>
        val result = route(app, FakeRequest("POST", "/foo")).get

        header(CONTENT_SECURITY_POLICY, result) must beNone
      }
    }

    "do not bypass CSP Filter when not using the route modifier" in {
      withApplicationRouter(Ok("hello"), defaultHocon, implicit app => {
        case _ =>
          val env = inject[Environment]
          val Action = inject[DefaultActionBuilder]
          new Stage {
            override def apply(requestHeader: RequestHeader): (RequestHeader, Handler) = {
              (requestHeader.addAttr(Router.Attrs.HandlerDef, HandlerDef(
                env.classLoader,
                "routes",
                "FooController",
                "foo",
                Seq.empty,
                "POST",
                "/foo",
                "comments",
                Seq("api")
              )), Action { request =>
                request.body.asFormUrlEncoded
                  .flatMap(_.get("foo"))
                  .flatMap(_.headOption)
                  .map(Results.Ok(_))
                  .getOrElse(Results.NotFound)
              })
            }
          }
      }) { app =>
        val result = route(app, FakeRequest("POST", "/foo")).get

        header(CONTENT_SECURITY_POLICY, result) must beSome
      }
    }
  }

  "reportOnly" should {
    "set only the report only header when defined" in withApplication(Ok("hello"), ConfigFactory.parseString(defaultHocon +
      """
        |play.filters.csp.reportOnly=true
        |""".stripMargin).withFallback(defaultConfig)
      .root().render(ConfigRenderOptions.concise())) { app =>
      val result = route(app, FakeRequest()).get

      header(CONTENT_SECURITY_POLICY_REPORT_ONLY, result) must beSome
      header(CONTENT_SECURITY_POLICY, result) must beNone
    }
  }

  "nonce" should {

    "work with no nonce" in withApplication(Ok("hello"), ConfigFactory.parseString(defaultHocon +
      """
        |play.filters.csp.nonce.enabled=false
        |play.filters.csp.directives.script-src="%CSP_NONCE_PATTERN%"
        |""".stripMargin)
      .root().render(ConfigRenderOptions.concise())) { app =>
      val result = route(app, FakeRequest()).get

      // https://csp-evaluator.withgoogle.com/ is great here
      val expected = "script-src %CSP_NONCE_PATTERN%"

      header(CONTENT_SECURITY_POLICY, result) must beSome(expected)
    }

    "work with CSP nonce" in withApplication(Ok("hello"), ConfigFactory.parseString(defaultHocon +
      """
        |play.filters.csp.directives.script-src="%CSP_NONCE_PATTERN%"
        |""".stripMargin)
      .root().render(ConfigRenderOptions.concise())) { app =>
      val result = route(app, FakeRequest()).get

      val cspNonce = header(X_CONTENT_SECURITY_POLICY_NONCE_HEADER, result).get
      val expected = s"script-src 'nonce-$cspNonce'"

      header(CONTENT_SECURITY_POLICY, result) must beSome(expected)
    }

    "work with CSP nonce but no nonce header" in withApplication(Ok("hello"), ConfigFactory.parseString(defaultHocon +
      """
        |play.filters.csp.nonce.header=false
        |""".stripMargin)
      .root().render(ConfigRenderOptions.concise())) { app =>
      val result = route(app, FakeRequest()).get

      header(X_CONTENT_SECURITY_POLICY_NONCE_HEADER, result) must beNone
    }

    "set a CSP nonce attribute and get it directly" in withApplicationRouter(Ok("hello"), defaultHocon, implicit app => {
      case _ =>
        val Action = inject[DefaultActionBuilder]
        Action { request =>
          Ok(request.attrs.get(RequestAttrKey.CSPNonce).getOrElse("undefined"))
        }
    }) { app =>
      val result = route(app, FakeRequest()).get
      val cspNonce = header(X_CONTENT_SECURITY_POLICY_NONCE_HEADER, result).get
      val bodyText: String = contentAsString(result)
      bodyText must_== cspNonce
    }

    "render CSP nonce attribute in template using CSPNonce" in withApplicationRouter(Ok("hello"), defaultHocon, implicit app => {
      case _ =>
        val Action = inject[DefaultActionBuilder]
        Action { implicit request =>
          Ok(views.html.helper.CSPNonce.get.getOrElse("undefined"))
        }
    }) { app =>
      val result = route(app, FakeRequest(GET, "/template")).get
      val cspNonce = header(X_CONTENT_SECURITY_POLICY_NONCE_HEADER, result).get
      val bodyText: String = contentAsString(result)
      bodyText must_== cspNonce
    }
  }

  "hash" should {

    "work with hash defined" in withApplication(Ok("hello"), ConfigFactory.parseString(
      """
        |play.filters.csp {
        |   hashes = [
        |      {
        |        algorithm = "sha256"
        |        hash = "helloworld"
        |        pattern = "%CSP_HELLOWORLD_HASH%"
        |      },
        |    ]
        |    directives.script-src="%CSP_HELLOWORLD_HASH%"
        |}
        |""".stripMargin).withFallback(defaultConfig)
      .root().render(ConfigRenderOptions.concise())) { app =>
      val result = route(app, FakeRequest()).get
      val expected = "script-src 'sha256-helloworld'"

      header(CONTENT_SECURITY_POLICY, result) must beSome(expected)
    }

  }

}
