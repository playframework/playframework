/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.routing

import org.specs2.mutable.Specification
import play.api.routing.Router
import play.core.test.FakeRequest

class RouterSpec extends Specification {

  "Router dynamic string builder" should {
    "handle empty parts" in {
      dynamicString("") must_== ""
    }
    "handle simple parts" in {
      dynamicString("xyz") must_== "xyz"
    }
    "handle parts containing backslashes" in {
      dynamicString("x/y") must_== "x%2Fy"
    }
    "handle parts containing spaces" in {
      dynamicString("x y") must_== "x%20y"
    }
    "handle parts containing pluses" in {
      dynamicString("x+y") must_== "x+y"
    }
    "handle parts with unicode characters" in {
      dynamicString("ℛat") must_== "%E2%84%9Bat"
    }
  }

  "Router queryString builder" should {
    "build a query string" in {
      queryString(List(Some("a"), Some("b"))) must_== "?a&b"
    }
    "ignore none values" in {
      queryString(List(Some("a"), None, Some("b"))) must_== "?a&b"
      queryString(List(None, Some("a"), None)) must_== "?a"
    }
    "ignore empty values" in {
      queryString(List(Some("a"), Some(""), Some("b"))) must_== "?a&b"
      queryString(List(Some(""), Some("a"), Some(""))) must_== "?a"
    }
    "produce nothing if no values" in {
      queryString(List(None, Some(""))) must_== ""
      queryString(List()) must_== ""
    }
  }

  "PathPattern" should {
    val pathPattern = PathPattern(Seq(StaticPart("/path/"), StaticPart("to/"), DynamicPart("foo", "[^/]+", true)))
    val pathString = "/path/to/some%20file"
    val pathNonEncodedString1 = "/path/to/bar:baz"
    val pathNonEncodedString2 = "/path/to/bar:%20baz"
    val pathStringInvalid = "/path/to/invalide%2"

    "Bind Path string as string" in {
      pathPattern(pathString).get("foo") must beEqualTo(Right("some file"))
    }
    "Bind Path with incorrectly encoded string as string" in {
      pathPattern(pathNonEncodedString1).get("foo") must beEqualTo(Right("bar:baz"))
    }
    "Bind Path with incorrectly encoded string as string" in {
      pathPattern(pathNonEncodedString2).get("foo") must beEqualTo(Right("bar: baz"))
    }
    "Fail on unparseable Path string" in {
      val Left(e) = pathPattern(pathStringInvalid).get("foo")
      e.getMessage must beEqualTo("Malformed escape pair at index 9: /invalide%2")
    }

    "multipart path is not decoded" in {
      val pathPattern = PathPattern(Seq(StaticPart("/path/"), StaticPart("to/"), DynamicPart("foo", ".+", false)))
      val pathString = "/path/to/this/is/some%20file/with/id"
      pathPattern(pathString).get("foo") must beEqualTo(Right("this/is/some%20file/with/id"))

    }
  }

  "SimpleRouter" should {

    import play.api.mvc.Handler
    import play.api.routing.sird._
    object Root extends Handler
    object Foo extends Handler

    val router = Router.from {
      case GET(p"/") => Root
      case GET(p"/foo") => Foo
    }

    "work" in {
      import play.api.http.HttpVerbs._
      router.handlerFor(FakeRequest(GET, "/")) must be some (Root)
      router.handlerFor(FakeRequest(GET, "/foo")) must be some (Foo)
    }

    "add prefix" in {
      import play.api.http.HttpVerbs._
      val apiRouter = router.withPrefix("/api")
      apiRouter.handlerFor(FakeRequest(GET, "/")) must beNone
      apiRouter.handlerFor(FakeRequest(GET, "/api/")) must be some (Root)
      apiRouter.handlerFor(FakeRequest(GET, "/api/foo")) must be some (Foo)
    }

    "add prefix multiple times" in {
      import play.api.http.HttpVerbs._
      val apiV1Router = "/api" /: "v1" /: router
      apiV1Router.handlerFor(FakeRequest(GET, "/")) must beNone
      apiV1Router.handlerFor(FakeRequest(GET, "/api/")) must beNone
      apiV1Router.handlerFor(FakeRequest(GET, "/api/v1/")) must be some (Root)
      apiV1Router.handlerFor(FakeRequest(GET, "/api/v1/foo")) must be some (Foo)
    }
  }
}
