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
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.mvc.request.RemoteConnection
import play.api.mvc.request.RequestAttrKey
import play.api.mvc.request.RequestTarget
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

    "redirect an absolute request target using only its path and query" in new WithApplication(
      buildApp(mode = Mode.Prod)
    ) {
      override def running() = {
        val result = route(
          app,
          absoluteRequest("http://playframework.com:9000/please/dont?version=GTI|V8", "/please/dont")
        ).get

        header(LOCATION, result) must beSome("https://playframework.com/please/dont?version=GTI|V8")
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

    "handle an HTTPS X-Forwarded-Proto value case-insensitively when enabled" in new WithApplication(
      buildApp(
        """
          |play.filters.https.xForwardedProtoEnabled = true
      """.stripMargin,
        mode = Mode.Prod
      )
    ) {
      override def running() = {
        val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
        val result = route(app, request().withConnection(secure).withHeaders("X-Forwarded-Proto" -> "HTTPS")).get

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

    "redirect a bracketed IPv6 host to a custom HTTPS port" in new WithApplication(
      buildApp("play.filters.https.port = 9443", mode = Mode.Prod)
    ) {
      override def running() = {
        val result = route(app, request("/please", host = "[2001:db8::1]:9000")).get

        header(LOCATION, result) must beSome("https://[2001:db8::1]:9443/please")
      }
    }

    "redirect using a trusted forwarded host" in new WithServer(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.http.forwarded.version = "rfc7239"
          |play.http.forwarded.trustForwardedHost = true
        """.stripMargin,
        mode = Mode.Test,
        withWs = true
      ),
      testServerPort
    ) {
      override def running() = {
        val ws       = inject[WSClient]
        val response = await(
          ws.url(s"http://localhost:$port/please?foo=bar")
            .addHttpHeaders("Forwarded" -> "for=192.0.2.43;host=\"public.example:8080\"")
            .withFollowRedirects(false)
            .get()
        )

        response.status must_== PERMANENT_REDIRECT
        response.header(LOCATION) must beSome("https://public.example/please?foo=bar")
      }
    }

    "prefer a trusted effective host over an absolute request target" in new WithApplication(
      buildApp(mode = Mode.Prod)
    ) {
      override def running() = {
        val result = route(
          app,
          absoluteRequest("http://internal.example/please?foo=bar", "/please")
            .addAttr(RequestAttrKey.EffectiveHost, "public.example:8080")
        ).get

        header(LOCATION, result) must beSome("https://public.example/please?foo=bar")
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
    "ignore X-Forwarded-Proto when xForwardedProtoEnabled is false" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.xForwardedProtoEnabled = false
        """.stripMargin,
        mode = Mode.Test
      )
    ) {
      override def running() = {
        val insecure =
          RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
        val result = route(app, request().withConnection(insecure).withHeaders("X-Forwarded-Proto" -> "https")).get

        header(STRICT_TRANSPORT_SECURITY, result) must beNone
        header(LOCATION, result) must beSome("https://playframework.com/")
        status(result) must_== PERMANENT_REDIRECT
      }
    }
    "handle an HTTP X-Forwarded-Proto value case-insensitively when enabled" in new WithApplication(
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
        val result = route(app, request().withConnection(secure).withHeaders("X-Forwarded-Proto" -> "HTTP")).get

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

  private def request(
      path: String = "/",
      queryParams: Option[String] = None,
      host: String = "playframework.com"
  ) = {
    FakeRequest(method = "GET", path = path + queryParams.map("?" + _).getOrElse(""))
      .withHeaders(HOST -> host)
  }

  private def absoluteRequest(uri: String, path: String) = {
    request(uri).withTarget(RequestTarget(uri, path, Map.empty))
  }

  def inject[T: ClassTag](implicit app: Application) = app.injector.instanceOf[T]

  private def buildApp(config: String = "", mode: Mode = Mode.Test, withWs: Boolean = false) = {
    val modules: Seq[play.api.inject.guice.GuiceableModule] =
      Seq[play.api.inject.guice.GuiceableModule](
        new play.api.inject.BuiltinModule,
        new play.api.mvc.CookiesModule,
        new play.api.i18n.I18nModule,
        new play.filters.https.RedirectHttpsModule
      ) ++ (if (withWs) Seq[play.api.inject.guice.GuiceableModule](new play.api.libs.ws.ahc.AhcWSModule) else Seq.empty)

    GuiceApplicationBuilder(Environment.simple(mode = mode))
      .configure(Configuration(ConfigFactory.parseString(config)))
      .load(modules*)
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
}
