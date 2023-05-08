/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.https

import scala.reflect.ClassTag

import com.typesafe.config.ConfigFactory
import jakarta.inject.Inject
import play.api._
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.mvc.request.RemoteConnection
import play.api.mvc.Handler.Stage
import play.api.mvc.Results._
import play.api.routing.HandlerDef
import play.api.routing.Router
import play.api.test._
import play.api.test.WithApplication
import play.api.Configuration
import play.api.Environment

private[https] class TestFilters @Inject() (redirectPlainFilter: RedirectHttpsFilter) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = Seq(redirectPlainFilter)
}

class RedirectHttpsFilterSpec extends PlaySpecification {
  "RedirectHttpsConfigurationProvider" should {
    "throw configuration error on invalid redirect status code" in {
      val configuration  = Configuration.from(Map("play.filters.https.redirectStatusCode" -> "200"))
      val environment    = Environment.simple()
      val configProvider = new RedirectHttpsConfigurationProvider(configuration, environment)

      {
        configProvider.get
      } must throwA[com.typesafe.config.ConfigException.Missing]
    }
  }

  "RedirectHttpsFilter" should {
    "redirect when not on https including the path and url query parameters" in new WithApplication(
      buildApp(mode = Mode.Prod)
    ) with Injecting {
      override def running() = {
        val req    = request("/please/dont?remove=this&foo=bar")
        val result = route(app, req).get

        status(result) must_== PERMANENT_REDIRECT
        header(LOCATION, result) must beSome("https://playframework.com/please/dont?remove=this&foo=bar")
      }
    }

    "redirect with custom redirect status code if configured" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectStatusCode = 301
        """.stripMargin,
        mode = Mode.Prod
      )
    ) with Injecting {
      override def running() = {
        val req    = request("/please/dont?remove=this&foo=bar")
        val result = route(app, req).get

        status(result) must_== 301
      }
    }

    "not redirect when on http in test" in new WithApplication(buildApp(mode = Mode.Test)) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
        val result = route(app, request().withConnection(secure)).get

        header(STRICT_TRANSPORT_SECURITY, result) must beNone
        status(result) must_== OK
      }
    }

    "redirect when on http in test and redirectEnabled = true" in new WithApplication(
      buildApp("play.filters.https.redirectEnabled = true", mode = Mode.Test)
    ) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
        val result = route(app, request().withConnection(secure)).get

        header(STRICT_TRANSPORT_SECURITY, result) must beNone
        status(result) must_== PERMANENT_REDIRECT
      }
    }

    "not redirect when on https but send HSTS header" in new WithApplication(buildApp(mode = Mode.Prod)) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = true, clientCertificateChain = None)
        val result = route(app, request().withConnection(secure)).get

        header(STRICT_TRANSPORT_SECURITY, result) must beSome("max-age=31536000; includeSubDomains")
        status(result) must_== OK
      }
    }

    "not redirect when X-Forwarded-Proto header is 'https' (and not on https) but send HSTS header" in new WithApplication(
      buildApp(
        """
          |play.filters.https.xForwardedProtoEnabled = true
      """.stripMargin,
        mode = Mode.Prod
      )
    ) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
        val result = route(app, request().withConnection(secure).withHeaders("X-Forwarded-Proto" -> "https")).get

        header(STRICT_TRANSPORT_SECURITY, result) must beSome("max-age=31536000; includeSubDomains")
        status(result) must_== OK
      }
    }

    "redirect to custom HTTPS port if configured" in new WithApplication(
      buildApp("play.filters.https.port = 9443", mode = Mode.Prod)
    ) {
      override def running() = {
        val result = route(app, request("/please/dont?remove=this&foo=bar")).get

        header(LOCATION, result) must beSome("https://playframework.com:9443/please/dont?remove=this&foo=bar")
      }
    }

    "not contain default HSTS header if secure in test" in new WithApplication(buildApp(mode = Mode.Test)) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = true, clientCertificateChain = None)
        val result = route(app, request().withConnection(secure)).get

        header(STRICT_TRANSPORT_SECURITY, result) must beNone
      }
    }

    "contain default HSTS header if secure in production" in new WithApplication(buildApp(mode = Mode.Prod)) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = true, clientCertificateChain = None)
        val result = route(app, request().withConnection(secure)).get

        header(STRICT_TRANSPORT_SECURITY, result) must beSome("max-age=31536000; includeSubDomains")
      }
    }

    "contain custom HSTS header if configured explicitly in prod" in new WithApplication(
      buildApp(
        """
          |play.filters.https.strictTransportSecurity="max-age=12345; includeSubDomains"
        """.stripMargin,
        mode = Mode.Prod
      )
    ) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = true, clientCertificateChain = None)
        val result = route(app, request().withConnection(secure)).get

        header(STRICT_TRANSPORT_SECURITY, result) must beSome("max-age=12345; includeSubDomains")
      }
    }

    "not redirect when xForwardedProtoEnabled is set but no header present" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.xForwardedProtoEnabled = true
        """.stripMargin,
        mode = Mode.Test
      )
    ) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
        val result = route(app, request().withConnection(secure)).get

        header(STRICT_TRANSPORT_SECURITY, result) must beNone
        status(result) must_== OK
      }
    }
    "redirect when xForwardedProtoEnabled is not set and no header present" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.xForwardedProtoEnabled = false
        """.stripMargin,
        mode = Mode.Test
      )
    ) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
        val result = route(app, request().withConnection(secure)).get

        header(STRICT_TRANSPORT_SECURITY, result) must beNone
        status(result) must_== PERMANENT_REDIRECT
      }
    }
    "redirect when xForwardedProtoEnabled is set and header is present" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.xForwardedProtoEnabled = true
        """.stripMargin,
        mode = Mode.Test
      )
    ) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
        val result = route(app, request().withConnection(secure).withHeaders("X-Forwarded-Proto" -> "http")).get

        header(STRICT_TRANSPORT_SECURITY, result) must beNone
        status(result) must_== PERMANENT_REDIRECT
      }
    }

    "send HSTS header when request itself is not secure but X-Forwarded-Proto header is 'https'" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.xForwardedProtoEnabled = true
        """.stripMargin,
        mode = Mode.Test
      )
    ) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
        val result = route(app, request().withConnection(secure).withHeaders("X-Forwarded-Proto" -> "https")).get

        header(STRICT_TRANSPORT_SECURITY, result) must beSome("max-age=31536000; includeSubDomains")
        status(result) must_== OK
      }
    }

    "not redirect when path included in redirectExcludePath" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.xForwardedProtoEnabled = true
          |play.filters.https.excludePaths = ["/skip"]
        """.stripMargin,
        mode = Mode.Test
      )
    ) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
        val result = route(app, request("/skip").withConnection(secure).withHeaders("X-Forwarded-Proto" -> "http")).get

        header(STRICT_TRANSPORT_SECURITY, result) must beNone
        status(result) must_== OK
      }
    }

    "not redirect when path included in redirectExcludePath and request has query params" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.xForwardedProtoEnabled = true
          |play.filters.https.excludePaths = ["/skip"]
        """.stripMargin,
        mode = Mode.Test
      )
    ) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
        val result = route(
          app,
          request("/skip", Some("foo=bar")).withConnection(secure).withHeaders("X-Forwarded-Proto" -> "http")
        ).get

        header(STRICT_TRANSPORT_SECURITY, result) must beNone
        status(result) must_== OK
      }
    }

    "not redirect when route has whitelisted modifier" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.routeModifiers.whiteList = [ "nohttps" ]
        """.stripMargin,
        mode = Mode.Test
      )
    ) {
      override def running() = {
        val insecure =
          RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
        val result = route(app, request("/modifiers").withConnection(insecure)).get

        header(STRICT_TRANSPORT_SECURITY, result) must beNone
        status(result) must_== OK
      }
    }

    "redirect when route does not have whitelisted modifier" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.routeModifiers.whiteList = [ "other" ]
        """.stripMargin,
        mode = Mode.Test
      )
    ) {
      override def running() = {
        val insecure =
          RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
        val result = route(app, request("/modifiers").withConnection(insecure)).get

        header(STRICT_TRANSPORT_SECURITY, result) must beNone
        status(result) must_== PERMANENT_REDIRECT
      }
    }

    "redirect when route has blacklisted modifier" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.routeModifiers.whiteList = []
          |play.filters.https.routeModifiers.blackList = [ "api" ]
        """.stripMargin,
        mode = Mode.Test
      )
    ) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
        val result = route(app, request("/modifiers").withConnection(secure)).get

        header(STRICT_TRANSPORT_SECURITY, result) must beNone
        status(result) must_== PERMANENT_REDIRECT
      }
    }

    "not redirect when route does not have blacklisted modifier" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.routeModifiers.whiteList = []
          |play.filters.https.routeModifiers.blackList = [ "other" ]
        """.stripMargin,
        mode = Mode.Test
      )
    ) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
        val result = route(app, request("/modifiers").withConnection(secure)).get

        header(STRICT_TRANSPORT_SECURITY, result) must beNone
        status(result) must_== OK
      }
    }

    "redirect when black and white lists are empty" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.routeModifiers.whiteList = []
          |play.filters.https.routeModifiers.blackList = []
        """.stripMargin,
        mode = Mode.Test
      )
    ) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
        val result = route(app, request("/modifiers").withConnection(secure)).get

        header(STRICT_TRANSPORT_SECURITY, result) must beNone
        status(result) must_== PERMANENT_REDIRECT
      }
    }
  }

  private def request(path: String = "/", queryParams: Option[String] = None) = {
    FakeRequest(method = "GET", path = path + queryParams.map("?" + _).getOrElse(""))
      .withHeaders(HOST -> "playframework.com")
  }

  def inject[T: ClassTag](implicit app: Application) = app.injector.instanceOf[T]

  private def buildApp(config: String = "", mode: Mode = Mode.Test) =
    GuiceApplicationBuilder(Environment.simple(mode = mode))
      .configure(Configuration(ConfigFactory.parseString(config)))
      .load(
        new play.api.inject.BuiltinModule,
        new play.api.mvc.CookiesModule,
        new play.api.i18n.I18nModule,
        new play.filters.https.RedirectHttpsModule
      )
      .appRoutes(implicit app => {
        case ("GET", "/") =>
          val action = inject[DefaultActionBuilder]
          action(Ok(""))
        case ("GET", "/skip") =>
          val action = inject[DefaultActionBuilder]
          action(Ok(""))
        case ("GET", "/modifiers") =>
          val env    = inject[Environment]
          val action = inject[DefaultActionBuilder]
          new Stage {
            override def apply(requestHeader: RequestHeader): (RequestHeader, Handler) = {
              (
                requestHeader.addAttr(
                  Router.Attrs.HandlerDef,
                  HandlerDef(
                    env.classLoader,
                    "routes",
                    "FooController",
                    "foo",
                    Seq.empty,
                    "POST",
                    "/modifiers",
                    "comments",
                    Seq("NOHTTPS", "api")
                  )
                ),
                action(Ok(""))
              )
            }
          }
      })
      .overrides(
        bind[HttpFilters].to[TestFilters]
      )
      .build()
}
