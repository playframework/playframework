/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.cors

import javax.inject.Inject

import play.api.http.HttpFilters
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.routing.Router

import scala.concurrent.Future

import play.api.Configuration
import play.api.mvc.{ Action, Result, Results }
import play.api.test.{ FakeRequest, FakeApplication, PlaySpecification }
import play.api.inject.bind

object CORSFilterSpec extends CORSCommonSpec {

  class Filters @Inject() (corsFilter: CORSFilter) extends HttpFilters {
    def filters = Seq(corsFilter)
  }

  def withApplication[T](conf: Map[String, _ <: Any] = Map.empty)(block: => T): T = {
    running(new GuiceApplicationBuilder()
      .configure(conf)
      .overrides(
        bind[Router].to(Router.from {
          case _ => Action(Results.Ok)
        }),
        bind[HttpFilters].to[Filters]
      ).build())(block)
  }

  "The CORSFilter" should {

    val restrictPaths = Map("play.filters.cors.pathPrefixes" -> Seq("/foo", "/bar"))

    "pass through a cors request that doesn't match the path prefixes" in withApplication(conf = restrictPaths) {
      val result = route(fakeRequest("GET", "/baz").withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== OK
      mustBeNoAccessControlResponseHeaders(result)
    }

    commonTests
  }
}

object CORSActionBuilderSpec extends CORSCommonSpec {

  def withApplication[T](conf: Map[String, _ <: Any] = Map.empty)(block: => T): T = {
    running(FakeApplication(
      withRoutes = {
        case _ => CORSActionBuilder(Configuration.reference ++ Configuration.from(conf))(Results.Ok)
      }
    ))(block)
  }

  def withApplicationWithPathConfiguredAction[T](configPath: String, conf: Map[String, _ <: Any] = Map.empty)(block: => T): T = {
    running(FakeApplication(
      additionalConfiguration = conf,
      withRoutes = {
        case _ => CORSActionBuilder(Configuration.reference ++ Configuration.from(conf), configPath)(Results.Ok)
      }
    ))(block)
  }

  "The CORSActionBuilder with" should {

    val restrictOriginsPathConf = Map("myaction.allowedOrigins" -> Seq("http://example.org", "http://localhost:9000"))

    "handle a cors request with a subpath of app configuration" in withApplicationWithPathConfiguredAction(configPath = "myaction", conf = restrictOriginsPathConf) {
      val result = route(fakeRequest().withHeaders(ORIGIN -> "http://localhost:9000")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beSome("true")
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://localhost:9000")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
      header(ACCESS_CONTROL_MAX_AGE, result) must beNone
      header(VARY, result) must beSome(ORIGIN)
    }

    commonTests
  }
}

trait CORSCommonSpec extends PlaySpecification {

  def withApplication[T](conf: Map[String, _ <: Any] = Map.empty)(block: => T): T

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

    "pass through requests without an origin header" in withApplication() {
      val result = route(fakeRequest()).get

      status(result) must_== OK
      mustBeNoAccessControlResponseHeaders(result)
    }

    "pass through same origin requests" in {
      "with a port number" in withApplication() {
        val result = route(FakeRequest().withHeaders(
          ORIGIN -> "http://www.example.com:9000",
          HOST -> "www.example.com:9000"
        )).get

        status(result) must_== OK
        mustBeNoAccessControlResponseHeaders(result)
      }
      "without a port number" in withApplication() {
        val result = route(FakeRequest().withHeaders(
          ORIGIN -> "http://www.example.com",
          HOST -> "www.example.com"
        )).get

        status(result) must_== OK
        mustBeNoAccessControlResponseHeaders(result)
      }
    }

    "not consider sub domains to be the same origin" in withApplication() {
      val result = route(fakeRequest().withHeaders(
        ORIGIN -> "http://www.example.com",
        HOST -> "example.com"
      )).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://www.example.com")
    }

    "not consider different ports to be the same origin" in withApplication() {
      val result = route(fakeRequest().withHeaders(
        ORIGIN -> "http://www.example.com:9000",
        HOST -> "www.example.com:9001"
      )).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://www.example.com:9000")
    }

    "forbid an empty origin header" in withApplication() {
      val result = route(fakeRequest().withHeaders(ORIGIN -> "")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "forbid an invalid origin header" in withApplication() {
      val result = route(fakeRequest().withHeaders(ORIGIN -> "localhost")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "forbid an unrecognized HTTP method" in withApplication() {
      val result = route(fakeRequest("FOO", "/").withHeaders(ORIGIN -> "localhost")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "forbid an empty Access-Control-Request-Method header in a preflight request" in withApplication() {
      val result = route(fakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "handle a simple cross-origin request with default config" in withApplication() {
      val result = route(fakeRequest("GET", "/").withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beSome("true")
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://localhost")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
      header(ACCESS_CONTROL_MAX_AGE, result) must beNone
      header(VARY, result) must beSome(ORIGIN)
    }

    "handle a basic preflight request with default config" in withApplication() {
      val result = route(fakeRequest("OPTIONS", "/").withHeaders(
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

    "handle a preflight request with request headers with default config" in withApplication() {
      val result = route(fakeRequest("OPTIONS", "/").withHeaders(
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

    "handle an actual cross-origin request with default config" in withApplication() {
      val result = route(fakeRequest("PUT", "/").withHeaders(ORIGIN -> "http://localhost")).get

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

    "handle a preflight request with credentials support off" in withApplication(conf = noCredentialsConf) {
      val result = route(fakeRequest("OPTIONS", "/").withHeaders(
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

    "handle a simple cross-origin request with credentials support off" in withApplication(conf = noCredentialsConf) {
      val result = route(fakeRequest("GET", "/").withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("*")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
      header(ACCESS_CONTROL_MAX_AGE, result) must beNone
    }

    val noPreflightCache = Map("play.filters.cors.preflightMaxAge" -> "0 seconds")

    "handle a preflight request with preflight caching off" in withApplication(conf = noPreflightCache) {
      val result = route(fakeRequest("OPTIONS", "/").withHeaders(
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

    "handle a preflight request with custom preflight cache max age" in withApplication(conf = customMaxAge) {
      val result = route(fakeRequest("OPTIONS", "/").withHeaders(
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

    "forbid a preflight request with a retricted request method" in withApplication(conf = restrictMethods) {
      val result = route(fakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "PUT")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    val restrictHeaders = Map("play.filters.cors.allowedHttpHeaders" -> Seq("X-Header1"))

    "forbid a preflight request with a retricted request header" in withApplication(conf = restrictHeaders) {
      val result = route(fakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "PUT",
        ACCESS_CONTROL_REQUEST_HEADERS -> "X-Header1, X-Header2")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    val exposeHeaders = Map("play.filters.cors.exposedHeaders" -> Seq("X-Header1", "X-Header2"))

    "handle a cors request with exposed headers configured" in withApplication(conf = exposeHeaders) {
      val result = route(fakeRequest().withHeaders(ORIGIN -> "http://localhost")).get

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

    "forbid a preflight request with a retricted origin" in withApplication(conf = restrictOrigins) {
      val result = route(fakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "PUT")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "forbid a cors request with a restricted origin" in withApplication(conf = restrictOrigins) {
      val result = route(fakeRequest().withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "handle a cors request with a whitelisted origin" in withApplication(conf = restrictOrigins) {
      val result = route(fakeRequest().withHeaders(ORIGIN -> "http://localhost:9000")).get

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
