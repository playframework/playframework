/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.cors

import scala.concurrent.Future

import play.api.Configuration
import play.api.mvc.{ Action, Result, Results }
import play.api.test.{ WithApplication, FakeRequest, FakeApplication, PlaySpecification }

object CORSFilterSpec extends CORSCommonSpec {

  def withApplication[T](conf: Map[String, _ <: Any] = Map.empty)(block: => T): T = {
    running(FakeApplication(
      additionalConfiguration = conf,
      withRoutes = {
        case _ => CORSFilter.apply(Action(Results.Ok))
      }
    ))(block)
  }

  "The CORSFilter" should {

    val restrictPaths = Map("cors.path.prefixes" -> Seq("/foo", "/bar"))

    "pass through a cors request that doesn't match the path prefixes" in withApplication(conf = restrictPaths) {
      val result = route(FakeRequest("GET", "/baz").withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== OK
      mustBeNoAccessControlResponseHeaders(result)
    }

    commonTests
  }
}

object CORSActionBuilderSpec extends CORSCommonSpec {

  def withApplication[T](conf: Map[String, _ <: Any] = Map.empty)(block: => T): T = {
    running(FakeApplication(
      additionalConfiguration = conf,
      withRoutes = {
        case _ => CORSActionBuilder(Results.Ok)
      }
    ))(block)
  }

  def withApplicationWithLocallyConfiguredAction[T](conf: Map[String, _ <: Any] = Map.empty)(block: => T): T = {
    running(FakeApplication(
      withRoutes = {
        case _ => CORSActionBuilder(CORSConfig.fromConfiguration(Configuration.from(conf)))(Results.Ok)
      }
    ))(block)
  }

  def withApplicationWithPathConfiguredAction[T](configPath: String, conf: Map[String, _ <: Any] = Map.empty)(block: => T): T = {
    running(FakeApplication(
      additionalConfiguration = conf,
      withRoutes = {
        case _ => CORSActionBuilder(configPath)(Results.Ok)
      }
    ))(block)
  }

  "The CORSActionBuilder with" should {

    val restrictOriginsLocalConf = Map("cors.allowed.origins" -> Seq("http://example.org", "http://localhost:9000"))

    "handle a cors request with a local configuration" in withApplicationWithLocallyConfiguredAction(conf = restrictOriginsLocalConf) {
      val result = route(FakeRequest().withHeaders(ORIGIN -> "http://localhost:9000")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beSome("true")
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://localhost:9000")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
      header(ACCESS_CONTROL_MAX_AGE, result) must beNone
      header(VARY, result) must beSome(ORIGIN)
    }

    val restrictOriginsPathConf = Map("myaction.cors.allowed.origins" -> Seq("http://example.org", "http://localhost:9000"))

    "handle a cors request with a subpath of app configuration" in withApplicationWithPathConfiguredAction(configPath = "myaction", conf = restrictOriginsPathConf) {
      val result = route(FakeRequest().withHeaders(ORIGIN -> "http://localhost:9000")).get

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

  def commonTests = {

    "pass through requests without an origin header" in withApplication() {
      val result = route(FakeRequest()).get

      status(result) must_== OK
      mustBeNoAccessControlResponseHeaders(result)
    }

    "pass through same origin requests" in withApplication() {
      val result = route(FakeRequest().withHeaders(
        ORIGIN -> "http://localhost:9000",
        HOST -> "localhost:9000")).get

      status(result) must_== OK
      mustBeNoAccessControlResponseHeaders(result)
    }

    "forbid an empty origin header" in withApplication() {
      val result = route(FakeRequest().withHeaders(ORIGIN -> "")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "forbid an invalid origin header" in withApplication() {
      val result = route(FakeRequest().withHeaders(ORIGIN -> "localhost")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "forbid an unrecognized HTTP method" in withApplication() {
      val result = route(FakeRequest("FOO", "/").withHeaders(ORIGIN -> "localhost")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "forbid an empty Access-Control-Request-Method header in a preflight request" in withApplication() {
      val result = route(FakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "handle a simple cross-origin request with default config" in withApplication() {
      val result = route(FakeRequest("GET", "/").withHeaders(ORIGIN -> "http://localhost")).get

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
      val result = route(FakeRequest("OPTIONS", "/").withHeaders(
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
      val result = route(FakeRequest("OPTIONS", "/").withHeaders(
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
      val result = route(FakeRequest("PUT", "/").withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beSome("true")
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://localhost")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
      header(ACCESS_CONTROL_MAX_AGE, result) must beNone
      header(VARY, result) must beSome(ORIGIN)
    }

    val noCredentialsConf = Map("cors.support.credentials" -> "false")

    "handle a preflight request with credentials support off" in withApplication(conf = noCredentialsConf) {
      val result = route(FakeRequest("OPTIONS", "/").withHeaders(
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
      val result = route(FakeRequest("GET", "/").withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("*")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beNone
      header(ACCESS_CONTROL_MAX_AGE, result) must beNone
    }

    val noPreflightCache = Map("cors.preflight.maxage" -> "0")

    "handle a preflight request with preflight caching off" in withApplication(conf = noPreflightCache) {
      val result = route(FakeRequest("OPTIONS", "/").withHeaders(
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

    val customMaxAge = Map("cors.preflight.maxage" -> "1800")

    "handle a preflight request with custom preflight cache max age" in withApplication(conf = customMaxAge) {
      val result = route(FakeRequest("OPTIONS", "/").withHeaders(
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

    val restrictMethods = Map("cors.allowed.http.methods" -> Seq("GET", "HEAD", "POST"))

    "forbid a preflight request with a retricted request method" in withApplication(conf = restrictMethods) {
      val result = route(FakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "PUT")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    val restrictHeaders = Map("cors.allowed.http.headers" -> Seq("X-Header1"))

    "forbid a preflight request with a retricted request header" in withApplication(conf = restrictHeaders) {
      val result = route(FakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "PUT",
        ACCESS_CONTROL_REQUEST_HEADERS -> "X-Header1, X-Header2")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    val exposeHeaders = Map("cors.exposed.headers" -> Seq("X-Header1", "X-Header2"))

    "handle a cors request with exposed headers configured" in withApplication(conf = exposeHeaders) {
      val result = route(FakeRequest().withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_CREDENTIALS, result) must beSome("true")
      header(ACCESS_CONTROL_ALLOW_HEADERS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_METHODS, result) must beNone
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome("http://localhost")
      header(ACCESS_CONTROL_EXPOSE_HEADERS, result) must beSome("X-Header1,X-Header2")
      header(ACCESS_CONTROL_MAX_AGE, result) must beNone
      header(VARY, result) must beSome(ORIGIN)
    }

    val restrictOrigins = Map("cors.allowed.origins" -> Seq("http://example.org", "http://localhost:9000"))

    "forbid a preflight request with a retricted origin" in withApplication(conf = restrictOrigins) {
      val result = route(FakeRequest("OPTIONS", "/").withHeaders(
        ORIGIN -> "http://localhost",
        ACCESS_CONTROL_REQUEST_METHOD -> "PUT")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "forbid a cors request with a restricted origin" in withApplication(conf = restrictOrigins) {
      val result = route(FakeRequest().withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== FORBIDDEN
      mustBeNoAccessControlResponseHeaders(result)
    }

    "handle a cors request with a whitelisted origin" in withApplication(conf = restrictOrigins) {
      val result = route(FakeRequest().withHeaders(ORIGIN -> "http://localhost:9000")).get

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
