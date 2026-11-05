/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

import models.UserId
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.mvc.Result
import play.api.test._

// Tests the `/java-request` routes, whose actions get the Java request passed
@RunWith(classOf[JUnitRunner])
object JavaRequestRouterSpec extends PlaySpecification {

  "reverse routes containing boolean parameters" in {
    "in the query string" in {
      controllers.routes.JavaRequestController.takeBool(true).url must equalTo("/java-request/take-bool?b=true")
      controllers.routes.JavaRequestController.takeBool(false).url must equalTo("/java-request/take-bool?b=false")
    }
    "in the path" in {
      controllers.routes.JavaRequestController.takeBool2(true).url must equalTo("/java-request/take-bool-2/true")
      controllers.routes.JavaRequestController.takeBool2(false).url must equalTo("/java-request/take-bool-2/false")
    }
  }

  "reverse routes containing custom parameters" in {
    "the query string" in {
      controllers.routes.JavaRequestController.queryUser(UserId("foo")).url must equalTo(
        "/java-request/query-user?userId=foo"
      )
      controllers.routes.JavaRequestController.queryUser(UserId("foo/bar")).url must equalTo(
        "/java-request/query-user?userId=foo%2Fbar"
      )
      controllers.routes.JavaRequestController.queryUser(UserId("foo?bar")).url must equalTo(
        "/java-request/query-user?userId=foo%3Fbar"
      )
      controllers.routes.JavaRequestController.queryUser(UserId("foo%bar")).url must equalTo(
        "/java-request/query-user?userId=foo%25bar"
      )
      controllers.routes.JavaRequestController.queryUser(UserId("foo&bar")).url must equalTo(
        "/java-request/query-user?userId=foo%26bar"
      )
    }
    "the path" in {
      controllers.routes.JavaRequestController.user(UserId("foo")).url must equalTo("/java-request/users/foo")
      controllers.routes.JavaRequestController.user(UserId("foo/bar")).url must equalTo("/java-request/users/foo%2Fbar")
      controllers.routes.JavaRequestController.user(UserId("foo?bar")).url must equalTo("/java-request/users/foo%3Fbar")
      controllers.routes.JavaRequestController.user(UserId("foo%bar")).url must equalTo("/java-request/users/foo%25bar")
      // & is not special for path segments
      controllers.routes.JavaRequestController.user(UserId("foo&bar")).url must equalTo("/java-request/users/foo&bar")
    }
  }

  "reverse routes containing only the request or path parameters" in {
    controllers.routes.JavaRequestController.index().url must equalTo("/java-request")
    controllers.routes.JavaRequestController.post().url must equalTo("/java-request/post")
    controllers.routes.JavaRequestController.withParam("foo").url must equalTo("/java-request/with/foo")
  }

  "pass the request to the action" in new WithApplication() {
    override def running() = {
      def check(path: String, expected: String) = {
        contentAsString(route(implicitApp, FakeRequest(GET, path)).get) must equalTo(expected)
      }

      check("/java-request", "/java-request")
      check("/java-request/with/foo", "/java-request/with/foo foo")
      check("/java-request/users/foo", "/java-request/users/foo foo")
      check("/java-request/query-user?userId=foo", "/java-request/query-user?userId=foo foo")
      check("/java-request/escapes/42", "/java-request/escapes/42 42")
      check("/java-request/ident/27", "/java-request/ident/27 27")
      check("/java-request/routes?abstract=foo", "/java-request/routes?abstract=foo foo")
      check("/java-request/intwarn/foo", "/java-request/intwarn/foo foo")
      check("/java-request/hello2", "/java-request/hello2 Hello world!")
    }
  }

  "bind boolean parameters" in {
    "from the query string" in new WithApplication() {
      override def running() = {
        val result = route(implicitApp, FakeRequest(GET, "/java-request/take-bool?b=true")).get
        contentAsString(result) must equalTo("/java-request/take-bool?b=true true")
        val result2 = route(implicitApp, FakeRequest(GET, "/java-request/take-bool?b=false")).get
        contentAsString(result2) must equalTo("/java-request/take-bool?b=false false")
        // Bind boolean values from 1 and 0 integers too
        contentAsString(route(implicitApp, FakeRequest(GET, "/java-request/take-bool?b=1")).get) must equalTo(
          "/java-request/take-bool?b=1 true"
        )
        contentAsString(route(implicitApp, FakeRequest(GET, "/java-request/take-bool?b=0")).get) must equalTo(
          "/java-request/take-bool?b=0 false"
        )
      }
    }
    "from the path" in new WithApplication() {
      override def running() = {
        val result = route(implicitApp, FakeRequest(GET, "/java-request/take-bool-2/true")).get
        contentAsString(result) must equalTo("/java-request/take-bool-2/true true")
        val result2 = route(implicitApp, FakeRequest(GET, "/java-request/take-bool-2/false")).get
        contentAsString(result2) must equalTo("/java-request/take-bool-2/false false")
        // Bind boolean values from 1 and 0 integers too
        contentAsString(route(implicitApp, FakeRequest(GET, "/java-request/take-bool-2/1")).get) must equalTo(
          "/java-request/take-bool-2/1 true"
        )
        contentAsString(route(implicitApp, FakeRequest(GET, "/java-request/take-bool-2/0")).get) must equalTo(
          "/java-request/take-bool-2/0 false"
        )
      }
    }
  }

  "bind int parameters from the query string as a list" in {

    "from a list of numbers" in new WithApplication() {
      override def running() = {
        val result = route(
          implicitApp,
          FakeRequest(
            GET,
            controllers.routes.JavaRequestController
              .takeList(List(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)).asJava)
              .url
          )
        ).get
        contentAsString(result) must equalTo("/java-request/take-list?x=1&x=2&x=3 1,2,3")
      }
    }
    "from a list of numbers and letters" in new WithApplication() {
      override def running() = {
        val result = route(implicitApp, FakeRequest(GET, "/java-request/take-list?x=1&x=a&x=2")).get
        status(result) must equalTo(BAD_REQUEST)
      }
    }
    "when there is no parameter at all" in new WithApplication() {
      override def running() = {
        val result = route(implicitApp, FakeRequest(GET, "/java-request/take-list")).get
        contentAsString(result) must equalTo("/java-request/take-list ")
      }
    }
    "using the Java API" in new WithApplication() {
      override def running() = {
        val result = route(implicitApp, FakeRequest(GET, "/java-request/take-java-list?x=1&x=2&x=3")).get
        contentAsString(result) must equalTo("/java-request/take-java-list?x=1&x=2&x=3 1,2,3")
      }
    }
  }

  "use a new instance for each instantiated controller" in new WithApplication() {
    override def running() = {
      route(implicitApp, FakeRequest(GET, "/java-request/instance")) must beSome[Future[Result]].like {
        case result => contentAsString(result) must_== "/java-request/instance 1"
      }
      route(implicitApp, FakeRequest(GET, "/java-request/instance")) must beSome[Future[Result]].like {
        case result => contentAsString(result) must_== "/java-request/instance 1"
      }
    }
  }

  "URL encoding and decoding works correctly" in new WithApplication() {
    override def running() = {
      def checkDecoding(
          dynamicEncoded: String,
          staticEncoded: String,
          queryEncoded: String,
          dynamicDecoded: String,
          staticDecoded: String,
          queryDecoded: String
      ) = {
        val path     = s"/java-request/urlcoding/$dynamicEncoded/$staticEncoded?q=$queryEncoded"
        val expected =
          s"/java-request/urlcoding/$dynamicEncoded/$staticDecoded?q=$queryEncoded dynamic=$dynamicDecoded static=$staticDecoded query=$queryDecoded"
        val result = route(implicitApp, FakeRequest(GET, path)).get
        val actual = contentAsString(result)
        actual must equalTo(expected)
      }

      def checkEncoding(
          dynamicDecoded: String,
          staticDecoded: String,
          queryDecoded: String,
          dynamicEncoded: String,
          staticEncoded: String,
          queryEncoded: String
      ) = {
        val expected = s"/java-request/urlcoding/$dynamicEncoded/$staticEncoded?q=$queryEncoded"
        val call     = controllers.routes.JavaRequestController.urlcoding(dynamicDecoded, staticDecoded, queryDecoded)
        call.url must equalTo(expected)
      }

      checkDecoding("a", "a", "a", "a", "a", "a")
      checkDecoding("%2B", "%2B", "%2B", "+", "%2B", "+")
      checkDecoding("+", "+", "+", "+", "+", " ")
      checkDecoding("%20", "%20", "%20", " ", "%20", " ")
      checkDecoding("&", "&", "-", "&", "&", "-")
      checkDecoding("=", "=", "-", "=", "=", "-")

      checkEncoding("+", "+", "+", "+", "+", "%2B")
      checkEncoding(" ", " ", " ", "%20", " ", "+")
      checkEncoding("&", "&", "&", "&", "&", "%26")
      checkEncoding("=", "=", "=", "=", "=", "%3D")

      // We use java.net.URLEncoder for query string encoding, which is not
      // RFC compliant, e.g. it percent-encodes "/" which is not a delimiter
      // for query strings, and it percent-encodes "~" which is an "unreserved" character
      // that should never be percent-encoded. The following tests, therefore
      // don't really capture our ideal desired behaviour for query string
      // encoding. However, the behaviour for dynamic and static paths is correct.
      checkEncoding("/", "/", "/", "%2F", "/", "%2F")
      checkEncoding("~", "~", "~", "~", "~", "%7E")

      checkDecoding("123", "456", "789", "123", "456", "789")
      checkEncoding("123", "456", "789", "123", "456", "789")
    }
  }

  "allow reverse routing of routes includes" in new WithApplication() {
    override def running() = {
      // Force the router to bootstrap the prefix
      implicitApp.injector.instanceOf[play.api.routing.Router]
      controllers.module.routes.ModuleController.javaRequest().url must_== "/module/java-request"
      contentAsString(route(implicitApp, FakeRequest(GET, "/module/java-request")).get) must_== "/module/java-request"
    }
  }

  "document the router" in new WithApplication() {
    override def running() = {
      val someRoute = implicitApp.injector
        .instanceOf[play.api.routing.Router]
        .documentation
        .find(r => r._1 == "GET" && r._2.startsWith("/java-request/with/"))
      someRoute must beSome[(String, String, String)]
      val route = someRoute.get
      route._2 must_== "/java-request/with/$param<[^/]+>"
      route._3 must_== "controllers.JavaRequestController.withParam(request:Request, param:String)"
    }
  }

  "reverse routes complex query params " in new WithApplication() {
    override def running() = {
      controllers.routes.JavaRequestController
        .takeList(List(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)).asJava)
        .url must_== "/java-request/take-list?x=1&x=2&x=3"
    }
  }

  "choose the first matching route for a call in reverse routes" in new WithApplication() {
    override def running() = {
      controllers.routes.JavaRequestController.hello().url must_== "/java-request/hello"
    }
  }

  // Regression test for #8827: the default value must be looked up for the parameter after the request
  "reverse routing a route with parameter that has default value and comes _after_ the request param" in new WithApplication() {
    override def running() = {
      controllers.routes.JavaRequestController.routedefault().url must_== "/java-request/routesdefault"
      controllers.routes.JavaRequestController.routedefault("y").url must_== "/java-request/routesdefault?type=y"
      contentAsString(route(implicitApp, FakeRequest(GET, "/java-request/routesdefault")).get) must_==
        "/java-request/routesdefault x"
    }
  }
}
