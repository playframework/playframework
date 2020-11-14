/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import org.specs2.mutable.Specification
import play.api.http.HttpConfiguration
import play.api.libs.typedmap.TypedEntry
import play.api.libs.typedmap.TypedKey
import play.api.libs.typedmap.TypedMap
import play.api.mvc.request.DefaultRequestFactory
import play.api.mvc.request.RemoteConnection
import play.api.mvc.request.RequestTarget
import play.mvc.Http.RequestBody

class RequestSpec extends Specification {
  "request" should {
    "have typed attributes" in {
      "can set and get a single attribute" in {
        val x = TypedKey[Int]("x")
        dummyRequest().withAttrs(TypedMap(x -> 3)).attrs(x) must_== 3
      }
      "can set two attributes and get one back" in {
        val x = TypedKey[Int]("x")
        val y = TypedKey[String]("y")
        dummyRequest().withAttrs(TypedMap(x -> 3, y -> "hello")).attrs(y) must_== "hello"
      }
      "getting a set attribute should be Some" in {
        val x = TypedKey[Int]("x")
        dummyRequest().withAttrs(TypedMap(x -> 5)).attrs.get(x) must beSome(5)
      }
      "getting a nonexistent attribute should be None" in {
        val x = TypedKey[Int]("x")
        dummyRequest().attrs.get(x) must beNone
      }
      "can add single attribute" in {
        val x = TypedKey[Int]("x")
        dummyRequest().addAttr(x, 3).attrs(x) must_== 3
      }
      "keep current attributes when adding a new one" in {
        val x = TypedKey[Int]
        val y = TypedKey[String]
        dummyRequest().withAttrs(TypedMap(y -> "hello")).addAttr(x, 3).attrs(y) must_== "hello"
      }
      "overrides current attribute value" in {
        val x = TypedKey[Int]
        val y = TypedKey[String]
        val request = dummyRequest()
          .withAttrs(TypedMap(y -> "hello"))
          .addAttr(x, 3)
          .addAttr(y, "white")

        request.attrs(y) must_== "white"
        request.attrs(x) must_== 3
      }
      "can add multiple attributes" in {
        val x   = TypedKey[Int]("x")
        val y   = TypedKey[Int]("y")
        val req = dummyRequest().addAttrs(TypedEntry(x, 3), TypedEntry(y, 4))
        req.attrs(x) must_== 3
        req.attrs(y) must_== 4
      }
      "keep current attributes when adding multiple ones" in {
        val x = TypedKey[Int]
        val y = TypedKey[Int]
        val z = TypedKey[String]
        dummyRequest()
          .withAttrs(TypedMap(z -> "hello"))
          .addAttrs(TypedEntry(x, 3), TypedEntry(y, 4))
          .attrs(z) must_== "hello"
      }
      "overrides current attribute value when adding multiple attributes" in {
        val x = TypedKey[Int]
        val y = TypedKey[Int]
        val z = TypedKey[String]
        val requestHeader = dummyRequest()
          .withAttrs(TypedMap(z -> "hello"))
          .addAttrs(TypedEntry(x, 3), TypedEntry(y, 4), TypedEntry(z, "white"))

        requestHeader.attrs(z) must_== "white"
        requestHeader.attrs(x) must_== 3
        requestHeader.attrs(y) must_== 4
      }
      "can set two attributes and get both back" in {
        val x = TypedKey[Int]("x")
        val y = TypedKey[String]("y")
        val r = dummyRequest().withAttrs(TypedMap(x -> 3, y -> "hello"))
        r.attrs(x) must_== 3
        r.attrs(y) must_== "hello"
      }
      "can set two attributes and remove one of them" in {
        val x   = TypedKey[Int]("x")
        val y   = TypedKey[String]("y")
        val req = dummyRequest().withAttrs(TypedMap(x -> 3, y -> "hello")).removeAttr(x)
        req.attrs.get(x) must beNone
        req.attrs(y) must_== "hello"
      }
      "can set two attributes and remove both again" in {
        val x   = TypedKey[Int]("x")
        val y   = TypedKey[String]("y")
        val req = dummyRequest().withAttrs(TypedMap(x -> 3, y -> "hello")).removeAttr(x).removeAttr(y)
        req.attrs.get(x) must beNone
        req.attrs.get(y) must beNone
      }
    }
  }

  private def dummyRequest(requestMethod: String = "GET", requestUri: String = "/", headers: Headers = Headers()) = {
    new DefaultRequestFactory(HttpConfiguration()).createRequest(
      connection = RemoteConnection("", false, None),
      method = "GET",
      target = RequestTarget(requestUri, "", Map.empty),
      version = "",
      headers = headers,
      attrs = TypedMap.empty,
      new RequestBody(null)
    )
  }
}
