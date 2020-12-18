/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import org.specs2.mock.Mockito
import java.util.Optional

import akka.util.ByteString
import play.api.test._
import play.api.mvc._
import play.mvc.Http
import play.mvc.Http.RequestBody
import play.mvc.Http.RequestImpl

import scala.collection.JavaConverters._

class JavaRequestsSpec extends PlaySpecification with Mockito {
  "JavaHelpers" should {
    "create a request with an id" in {
      val request                   = FakeRequest().withHeaders("Content-type" -> "application/json")
      val javaRequest: Http.Request = new RequestImpl(request.withBody(null))

      javaRequest.id() must not beNull
    }

    "create a request with case insensitive headers" in {
      val request                   = FakeRequest().withHeaders("Content-type" -> "application/json")
      val javaRequest: Http.Request = new RequestImpl(request.withBody(null))

      val ct: String = javaRequest.getHeaders.get("Content-Type").get()
      val headers    = javaRequest.getHeaders
      ct must beEqualTo("application/json")

      headers.getAll("content-type").asScala must_== List(ct)
      headers.getAll("Content-Type").asScala must_== List(ct)
      headers.get("content-type").get must_== ct
    }

    "create a request with a helper that can do cookies" in {
      import scala.collection.JavaConverters._

      val cookie1                      = Cookie("name1", "value1")
      val requestHeader: RequestHeader = FakeRequest().withCookies(cookie1)
      val javaRequest: Http.Request    = new RequestImpl(requestHeader.withBody(null))

      val iterator: Iterator[Http.Cookie] = javaRequest.cookies().asScala.toIterator
      val cookieList                      = iterator.toList

      (cookieList.size must be).equalTo(1)
      (cookieList.head.name must be).equalTo("name1")
      (cookieList.head.value must be).equalTo("value1")
    }

    "create a request with a helper that can do cookies" in {
      import scala.collection.JavaConverters._

      val cookie1 = Cookie("name1", "value1")

      val requestHeader: Request[Http.RequestBody] =
        Request[Http.RequestBody](FakeRequest().withCookies(cookie1), new RequestBody(null))
      val javaRequest = new RequestImpl(requestHeader)

      val iterator: Iterator[Http.Cookie] = javaRequest.cookies().asScala.toIterator
      val cookieList                      = iterator.toList

      (cookieList.size must be).equalTo(1)
      (cookieList.head.name must be).equalTo("name1")
      (cookieList.head.value must be).equalTo("value1")
    }

    "create a request without a body" in {
      // No Content-Length and no Transfer-Encoding header will be set
      val requestHeader: Request[Http.RequestBody] = Request[Http.RequestBody](FakeRequest(), new RequestBody(null))
      val javaRequest                              = new RequestImpl(requestHeader)

      // hasBody does check for null and knows it means there is no body
      requestHeader.hasBody must beFalse
      javaRequest.hasBody must beFalse
    }

    "create a request with an empty body" in {
      // No Content-Length and no Transfer-Encoding header will be set
      val requestHeader: Request[Http.RequestBody] =
        Request[Http.RequestBody](FakeRequest(), new RequestBody(Optional.empty()))
      val javaRequest = new RequestImpl(requestHeader)

      // hasBody knows that a RequestBody containing an empty Optional means there is no body (empty)
      requestHeader.hasBody must beFalse
      javaRequest.hasBody must beFalse
    }

    "create a request with a body" in {
      // No Content-Length and no Transfer-Encoding header will be set
      val requestHeader: Request[Http.RequestBody] = Request[Http.RequestBody](FakeRequest(), new RequestBody("foo"))
      val javaRequest                              = new RequestImpl(requestHeader)

      requestHeader.hasBody must beTrue
      javaRequest.hasBody must beTrue
    }

    "create a request with an empty string body" in {
      // No Content-Length and no Transfer-Encoding header will be set
      val requestHeader: Request[Http.RequestBody] = Request[Http.RequestBody](FakeRequest(), new RequestBody(""))
      val javaRequest                              = new RequestImpl(requestHeader)

      // Because no headers exist, hasBody can only check if the RequestBody does contain an empty body it knows about
      // (null or Optional.empty). Therefore, technically, something is set, even though this something might represent
      // "empty"/"no body" (like empty string here) - but how should hasBody know? This something could be a custom type
      // coming from a custom body parser defined entirely by the user... Sure we could check for the most common types
      // if they represent an empty body (empty Strings, empty Json, empty ByteString, etc.) but that would not be consistent
      // (custom types that represent nothing would still return true)
      requestHeader.hasBody must beTrue
      javaRequest.hasBody must beTrue
    }

    "create a request with an empty byte string body" in {
      // No Content-Length and no Transfer-Encoding header will be set
      val requestHeader: Request[Http.RequestBody] =
        Request[Http.RequestBody](FakeRequest(), new RequestBody(ByteString.empty))
      val javaRequest = new RequestImpl(requestHeader)

      // Same behaviour like the "empty string body" test above: hasBody can't figure out if this value represents empty
      requestHeader.hasBody must beTrue
      javaRequest.hasBody must beTrue
    }
  }
}
