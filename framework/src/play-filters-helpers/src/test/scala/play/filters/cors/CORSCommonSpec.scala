/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.cors

import play.api.Application
import play.api.mvc.Result
import play.api.test.{ FakeRequest, PlaySpecification }

import scala.concurrent.Future

trait CORSCommonSpec extends PlaySpecification {

  def withApplication[T](conf: Map[String, _ <: Any] = Map.empty)(block: Application => T): T

  def mustBeNoAccessControlResponseHeaders(result: Future[Result]) = {
    header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beNone
    header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
    header(ACCESS_CONTROL_ALLOW_METHODS, result) must beNone
    header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beNone
    header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
    header(ACCESS_CONTROL_MAX_AGE, result) must beNone
  }

  def fakeRequest(method: String = "GET", path: String = "/") = FakeRequest(method, path).withHeaders(
    HOST -> "www.example.com"
  )

  def commonTests = {

    "pass through requests without an origin header" in withApplication() { app =>
      val result = route(app, fakeRequest()).get

      status(result) must_== OK
      mustBeNoAccessControlResponseHeaders(result)
    }

    "pass through same origin requests" in {
      "with a port number" in withApplication() { app =>
        val result = route(app, FakeRequest().withHeaders(
          ORIGIN -> "http://www.example.com:9000",
          HOST -> "www.example.com:9000"
        )).get

        status(result) must_== OK
        header(VARY, result) must beSome(ORIGIN)
        mustBeNoAccessControlResponseHeaders(result)
      }
      "without a port number" in withApplication() { app =>
        val result = route(app, FakeRequest().withHeaders(
          ORIGIN -> "http://www.example.com",
          HOST -> "www.example.com"
        )).get

        status(result) must_== OK
        header(VARY, result) must beSome(ORIGIN)
        mustBeNoAccessControlResponseHeaders(result)
      }
    }

    val serveForbidden = Map(
      "play.filters.cors.allowedOrigins" -> Seq("http://example.org"),
      "play.filters.cors.serveForbiddenOrigins" -> "true")

    "pass through requests with serve forbidden origins on and an origin header that is" in {
      "invalid" in withApplication(conf = serveForbidden) { app =>
        val result = route(app, fakeRequest().withHeaders(
          ORIGIN -> "file://"
        )).get

        status(result) must_== OK
        header(VARY, result) must beSome(ORIGIN)
        mustBeNoAccessControlResponseHeaders(result)
      }
      "forbidden" in withApplication(conf = serveForbidden) { app =>
        val result = route(app, fakeRequest().withHeaders(
          ORIGIN -> "http://www.notinwhitelistorhost.com"
        )).get

        status(result) must_== OK
        header(VARY, result) must beSome(ORIGIN)
        mustBeNoAccessControlResponseHeaders(result)
      }
      "in the whitelist" in withApplication(conf = serveForbidden) { app =>
        val result = route(app, fakeRequest().withHeaders(
          ORIGIN -> "http://example.org"
        )).get

        status(result) must_== OK
        header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://example.org")
        header(VARY, result) must beSome(ORIGIN)
      }
    }

    "not consider sub domains to be the same origin" in withApplication() { app =>
      val result = route(app, fakeRequest().withHeaders(
        ORIGIN -> "http://www.example.com",
        HOST -> "example.com"
      )).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://www.example.com")
      header(VARY, result) must beSome(ORIGIN)
    }

    "not consider different ports to be the same origin" in withApplication() { app =>
      val result = route(app, fakeRequest().withHeaders(
        ORIGIN -> "http://www.example.com:9000",
        HOST -> "www.example.com:9001"
      )).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://www.example.com:9000")
      header(VARY, result) must beSome(ORIGIN)
    }

    "not consider different protocols to be the same origin" in withApplication() { app =>
      val result = route(app, fakeRequest().withHeaders(
        ORIGIN -> "https://www.example.com:9000",
        HOST -> "www.example.com:9000"
      )).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("https://www.example.com:9000")
      header(VARY, result) must beSome(ORIGIN)
    }

    "forbid an empty origin header" in withApplication() { app =>
      val result = route(app, fakeRequest().withHeaders(ORIGIN -> "")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "forbid an invalid origin header" in withApplication() { app =>
      val result = route(app, fakeRequest().withHeaders(ORIGIN -> "localhost")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "forbid an unrecognized HTTP method" in withApplication() { app =>
      val result = route(app, fakeRequest("FOO", "/").withHeaders(ORIGIN -> "localhost")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "forbid an empty Access-Control-Request-Method header in a preflight request" in withApplication() { app =>
      val result = route(app, fakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "handle a simple cross-origin request with default config" in withApplication() { app =>
      val result = route(app, fakeRequest("GET", "/").withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beSome("true")
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://localhost")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
      header(ACCESS_CONTROL_MAX_AGE, result) must beNone
      header(VARY, result) must beSome(ORIGIN)
    }

    "handle simple cross-origin request when the action throws an error" in withApplication() { app =>
      val result = route(app, fakeRequest("GET", "/error").withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== INTERNAL_SERVER_ERROR
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beSome("true")
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://localhost")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
      header(ACCESS_CONTROL_MAX_AGE, result) must beNone
      header(VARY, result) must beSome(ORIGIN)
    }

    "handle a basic preflight request with default config" in withApplication() { app =>
      val result = route(app, fakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "PUT")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beSome("true")
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beSome("PUT")
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://localhost")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
      header(ACCESS_CONTROL_MAX_AGE, result) must beSome("3600")
      header(VARY, result) must beSome(ORIGIN)
    }

    "handle a preflight request with request headers with default config" in withApplication() { app =>
      val result = route(app, fakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "PUT",
        ACCESS_CONTROL_REQUEST_HEADERS -> "X-Header1, X-Header2")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beSome("true")
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beSome("x-header1,x-header2")
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beSome("PUT")
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://localhost")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
      header(ACCESS_CONTROL_MAX_AGE, result) must beSome("3600")
      header(VARY, result) must beSome(ORIGIN)
    }

    "handle an actual cross-origin request with default config" in withApplication() { app =>
      val result = route(app, fakeRequest("PUT", "/").withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beSome("true")
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://localhost")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
      header(ACCESS_CONTROL_MAX_AGE, result) must beNone
      header(VARY, result) must beSome(ORIGIN)
    }

    val noCredentialsConf = Map("play.filters.cors.supportsCredentials" -> "false")

    "handle a preflight request with credentials support off" in withApplication(conf = noCredentialsConf) { app =>
      val result = route(app, fakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "PUT")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beSome("PUT")
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("*")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
      header(ACCESS_CONTROL_MAX_AGE, result) must beSome("3600")
    }

    "handle a simple cross-origin request with credentials support off" in withApplication(conf = noCredentialsConf) { app =>
      val result = route(app, fakeRequest("GET", "/").withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("*")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
      header(ACCESS_CONTROL_MAX_AGE, result) must beNone
    }

    val noPreflightCache = Map("play.filters.cors.preflightMaxAge" -> "0 seconds")

    "handle a preflight request with preflight caching off" in withApplication(conf = noPreflightCache) { app =>
      val result = route(app, fakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "PUT")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beSome("true")
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beSome("PUT")
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://localhost")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
      header(ACCESS_CONTROL_MAX_AGE, result) must beNone
      header(VARY, result) must beSome(ORIGIN)
    }

    val customMaxAge = Map("play.filters.cors.preflightMaxAge" -> "30 minutes")

    "handle a preflight request with custom preflight cache max age" in withApplication(conf = customMaxAge) { app =>
      val result = route(app, fakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "PUT")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beSome("true")
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beSome("PUT")
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://localhost")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
      header(ACCESS_CONTROL_MAX_AGE, result) must beSome("1800")
      header(VARY, result) must beSome(ORIGIN)
    }

    val restrictMethods = Map("play.filters.cors.allowedHttpMethods" -> Seq("GET", "HEAD", "POST"))

    "forbid a preflight request with a retricted request method" in withApplication(conf = restrictMethods) { app =>
      val result = route(app, fakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "PUT")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    val restrictHeaders = Map("play.filters.cors.allowedHttpHeaders" -> Seq("X-Header1"))

    "forbid a preflight request with a retricted request header" in withApplication(conf = restrictHeaders) { app =>
      val result = route(app, fakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "PUT",
        ACCESS_CONTROL_REQUEST_HEADERS -> "X-Header1, X-Header2")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    val exposeHeaders = Map("play.filters.cors.exposedHeaders" -> Seq("X-Header1", "X-Header2"))

    "handle a cors request with exposed headers configured" in withApplication(conf = exposeHeaders) { app =>
      val result = route(app, fakeRequest().withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beSome("true")
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://localhost")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beSome("X-Header1,X-Header2")
      header(ACCESS_CONTROL_MAX_AGE, result) must beNone
      header(VARY, result) must beSome(ORIGIN)
    }

    val restrictOrigins = Map("play.filters.cors.allowedOrigins" -> Seq("http://example.org", "http://localhost:9000"))

    "forbid a preflight request with a retricted origin" in withApplication(conf = restrictOrigins) { app =>
      val result = route(app, fakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "PUT")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "forbid a cors request with a restricted origin" in withApplication(conf = restrictOrigins) { app =>
      val result = route(app, fakeRequest().withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "handle a cors request with a whitelisted origin" in withApplication(conf = restrictOrigins) { app =>
      val result = route(app, fakeRequest().withHeaders(ORIGIN -> "http://localhost:9000")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beSome("true")
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://localhost:9000")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
      header(ACCESS_CONTROL_MAX_AGE, result) must beNone
      header(VARY, result) must beSome(ORIGIN)
    }

  }
}

