/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import org.specs2.mock.Mockito

import play.api.test._
import play.api.mvc._

import play.core.j.JavaHelpers
import play.mvc.Http
import play.mvc.Http.{ Context, RequestBody, RequestImpl }

class JavaRequestsSpec extends PlaySpecification with Mockito {

  "JavaHelpers" should {

    "create a request with case insensitive headers" in {
      val requestHeader: RequestHeader = FakeRequest().withHeaders("Content-type" -> "application/json")
      val javaRequest: Http.Request = new RequestImpl(requestHeader)

      val ct = javaRequest.getHeader("Content-Type")
      val headers = javaRequest.headers()
      ct must_== "application/json"
      headers.get("content-type") must_== Array(ct)
      headers.get("Content-Type") must_== Array(ct)
      javaRequest.getHeader("content-type") must_== ct
    }

    "create a request with a helper that can do cookies" in {
      import scala.collection.JavaConverters._

      val cookie1 = Cookie("name1", "value1")
      val requestHeader: RequestHeader = FakeRequest().withCookies(cookie1)
      val javaRequest: Http.Request = new RequestImpl(requestHeader)

      val iterator: Iterator[Http.Cookie] = javaRequest.cookies().asScala.toIterator
      val cookieList = iterator.toList

      cookieList.size must be equalTo 1
      cookieList.head.name must be equalTo "name1"
      cookieList.head.value must be equalTo "value1"
    }

    "create a context with a helper that can do cookies" in {
      import scala.collection.JavaConverters._

      val cookie1 = Cookie("name1", "value1")

      val requestHeader: Request[Http.RequestBody] = Request[Http.RequestBody](FakeRequest().withCookies(cookie1), new RequestBody(null))
      val javaContext: Context = JavaHelpers.createJavaContext(requestHeader, JavaHelpers.createContextComponents())
      val javaRequest = javaContext.request()

      val iterator: Iterator[Http.Cookie] = javaRequest.cookies().asScala.toIterator
      val cookieList = iterator.toList

      cookieList.size must be equalTo 1
      cookieList.head.name must be equalTo "name1"
      cookieList.head.value must be equalTo "value1"
    }

    "create a request without a body" in {
      val requestHeader: Request[Http.RequestBody] = Request[Http.RequestBody](FakeRequest(), new RequestBody(null))
      val javaContext: Context = JavaHelpers.createJavaContext(requestHeader, JavaHelpers.createContextComponents())
      val javaRequest = javaContext.request()

      requestHeader.hasBody must beFalse
      javaRequest.hasBody must beFalse
    }

    "create a request with a body" in {
      val requestHeader: Request[Http.RequestBody] = Request[Http.RequestBody](FakeRequest(), new RequestBody("foo"))
      val javaContext: Context = JavaHelpers.createJavaContext(requestHeader, JavaHelpers.createContextComponents())
      val javaRequest = javaContext.request()

      requestHeader.hasBody must beTrue
      javaRequest.hasBody must beTrue
    }

  }

}
