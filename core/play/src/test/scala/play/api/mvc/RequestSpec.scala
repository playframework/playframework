/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import org.specs2.mutable.Specification
import play.api.http.HttpConfiguration
import play.api.libs.typedmap.{ TypedKey, TypedMap }
import play.api.mvc.request.{ DefaultRequestFactory, RemoteConnection, RequestTarget }
import play.mvc.Http.RequestBody

class RequestSpec extends Specification {

  "request" should {
    "have typed attributes" in {
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
        val request = dummyRequest().withAttrs(TypedMap(y -> "hello"))
          .addAttr(x, 3)
          .addAttr(y, "white")

        request.attrs(y) must_== "white"
        request.attrs(x) must_== 3
      }
    }
  }

  private def dummyRequest(
    requestMethod: String = "GET",
    requestUri: String = "/",
    headers: Headers = Headers()) = {
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
