/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test

import play.api.mvc.Result
import play.api.test._
import models.UserId
import scala.jdk.CollectionConverters._
import scala.concurrent.Future

object RouterSpec extends PlaySpecification {

  "reverse routes containing boolean parameters" in {
    "in the query string" in {
      controllers.routes.Application.takeBool(true).url must equalTo("/take-bool?b=true")
      controllers.routes.Application.takeBool(false).url must equalTo("/take-bool?b=false")
    }
    "in the  path" in {
      controllers.routes.Application.takeBool2(true).url must equalTo("/take-bool-2/true")
      controllers.routes.Application.takeBool2(false).url must equalTo("/take-bool-2/false")
    }
  }

  "reverse routes containing custom parameters" in {
    "the query string" in {
      controllers.routes.Application.queryUser(UserId("foo")).url must equalTo("/query-user?userId=foo")
      controllers.routes.Application.queryUser(UserId("foo/bar")).url must equalTo("/query-user?userId=foo%2Fbar")
      controllers.routes.Application.queryUser(UserId("foo?bar")).url must equalTo("/query-user?userId=foo%3Fbar")
      controllers.routes.Application.queryUser(UserId("foo%bar")).url must equalTo("/query-user?userId=foo%25bar")
      controllers.routes.Application.queryUser(UserId("foo&bar")).url must equalTo("/query-user?userId=foo%26bar")
    }
    "the path" in {
      controllers.routes.Application.user(UserId("foo")).url must equalTo("/users/foo")
      controllers.routes.Application.user(UserId("foo/bar")).url must equalTo("/users/foo%2Fbar")
      controllers.routes.Application.user(UserId("foo?bar")).url must equalTo("/users/foo%3Fbar")
      controllers.routes.Application.user(UserId("foo%bar")).url must equalTo("/users/foo%25bar")
      // & is not special for path segments
      controllers.routes.Application.user(UserId("foo&bar")).url must equalTo("/users/foo&bar")
    }
  }

  "bind boolean parameters" in {
    "from the query string" in new WithApplication() {
      val result = route(implicitApp, FakeRequest(GET, "/take-bool?b=true")).get
      contentAsString(result) must equalTo("/take-bool?b=true true")
      val result2 = route(implicitApp, FakeRequest(GET, "/take-bool?b=false")).get
      contentAsString(result2) must equalTo("/take-bool?b=false false")
      // Bind boolean values from 1 and 0 integers too
      contentAsString(route(implicitApp, FakeRequest(GET, "/take-bool?b=1")).get) must equalTo("/take-bool?b=1 true")
      contentAsString(route(implicitApp, FakeRequest(GET, "/take-bool?b=0")).get) must equalTo("/take-bool?b=0 false")
    }
    "from the path" in new WithApplication() {
      val result = route(implicitApp, FakeRequest(GET, "/take-bool-2/true")).get
      contentAsString(result) must equalTo("/take-bool-2/true true")
      val result2 = route(implicitApp, FakeRequest(GET, "/take-bool-2/false")).get
      contentAsString(result2) must equalTo("/take-bool-2/false false")
      // Bind boolean values from 1 and 0 integers too
      contentAsString(route(implicitApp, FakeRequest(GET, "/take-bool-2/1")).get) must equalTo("/take-bool-2/1 true")
      contentAsString(route(implicitApp, FakeRequest(GET, "/take-bool-2/0")).get) must equalTo("/take-bool-2/0 false")
    }
  }

  "bind int parameters from the query string as a list" in {

    "from a list of numbers" in new WithApplication() {
      val result = route(
        implicitApp,
        FakeRequest(
          GET,
          controllers.routes.Application
            .takeList(List(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)).asJava)
            .url
        )
      ).get
      contentAsString(result) must equalTo("/take-list?x=1&x=2&x=3 1,2,3")
    }
    "from a list of numbers and letters" in new WithApplication() {
      val result = route(implicitApp, FakeRequest(GET, "/take-list?x=1&x=a&x=2")).get
      status(result) must equalTo(BAD_REQUEST)
    }
    "when there is no parameter at all" in new WithApplication() {
      val result = route(implicitApp, FakeRequest(GET, "/take-list")).get
      contentAsString(result) must equalTo("/take-list ")
    }
    "using the Java API" in new WithApplication() {
      val result = route(implicitApp, FakeRequest(GET, "/take-java-list?x=1&x=2&x=3")).get
      contentAsString(result) must equalTo("/take-java-list?x=1&x=2&x=3 1,2,3")
    }
  }

  "use a new instance for each instantiated controller" in new WithApplication() {
    route(implicitApp, FakeRequest(GET, "/instance")) must beSome[Future[Result]].like {
      case result => contentAsString(result) must_== "/instance 1"
    }
    route(implicitApp, FakeRequest(GET, "/instance")) must beSome[Future[Result]].like {
      case result => contentAsString(result) must_== "/instance 1"
    }
  }

  "URL encoding and decoding works correctly" in new WithApplication() {
    def checkDecoding(
        dynamicEncoded: String,
        staticEncoded: String,
        queryEncoded: String,
        dynamicDecoded: String,
        staticDecoded: String,
        queryDecoded: String
    ) = {
      val path = s"/urlcoding/$dynamicEncoded/$staticEncoded?q=$queryEncoded"
      val expected =
        s"/urlcoding/$dynamicEncoded/$staticDecoded?q=$queryEncoded dynamic=$dynamicDecoded static=$staticDecoded query=$queryDecoded"
      val result = route(implicitApp, FakeRequest(GET, path)).get
      val actual       = contentAsString(result)
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
      val expected = s"/urlcoding/$dynamicEncoded/$staticEncoded?q=$queryEncoded"
      val call     = controllers.routes.Application.urlcoding(dynamicDecoded, staticDecoded, queryDecoded)
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

  "allow reverse routing of routes includes" in new WithApplication() {
    // Force the router to bootstrap the prefix
    implicitApp.injector.instanceOf[play.api.routing.Router]
    controllers.module.routes.ModuleController.index().url must_== "/module/index"
  }

  "document the router" in new WithApplication() {
    // The purpose of this test is to alert anyone that changes the format of the router documentation that
    // it is being used by Swagger. So if you do change it, please let Tony Tam know at tony at wordnik dot com.
    val someRoute = implicitApp.injector
      .instanceOf[play.api.routing.Router]
      .documentation
      .find(r => r._1 == "GET" && r._2.startsWith("/with/"))
    someRoute must beSome[(String, String, String)]
    val route = someRoute.get
    route._2 must_== "/with/$param<[^/]+>"
    route._3 must startWith("controllers.Application.withParam")
  }

  "reverse routes complex query params " in new WithApplication() {
    controllers.routes.Application
      .takeList(List(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)).asJava)
      .url must_== "/take-list?x=1&x=2&x=3"
  }

  "choose the first matching route for a call in reverse routes" in new WithApplication() {
    controllers.routes.Application.hello().url must_== "/hello"
  }

  "reverse routing a route with parameter that has default value and comes _after_ the request param" in new WithApplication() {
    controllers.routes.Application.routedefault().url must_== "/routesdefault"
  }

  "The assets reverse route support" should {
    "fingerprint assets" in new WithApplication() {
      controllers.routes.Assets.versioned("css/main.css").url must_== "/public/css/abcd1234-main.css"
    }
    "selected the minified version" in new WithApplication() {
      controllers.routes.Assets.versioned("css/minmain.css").url must_== "/public/css/abcd1234-minmain-min.css"
    }
    "work for non fingerprinted assets" in new WithApplication() {
      controllers.routes.Assets.versioned("css/nonfingerprinted.css").url must_== "/public/css/nonfingerprinted.css"
    }
    "selected the minified non fingerprinted version" in new WithApplication() {
      controllers.routes.Assets
        .versioned("css/nonfingerprinted-minmain.css")
        .url must_== "/public/css/nonfingerprinted-minmain-min.css"
    }
  }
}
