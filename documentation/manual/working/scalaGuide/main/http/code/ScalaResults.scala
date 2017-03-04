/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */

import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import play.api.http.HeaderNames
import scala.concurrent.Future
import org.scalatestplus.play._

  
package scalaguide.http.scalaresults {


  class ScalaResultsSpec extends PlaySpec with Controller {

    "A scala result" should {
      "default result Content-Type" in {
        //#content-type_text
        val textResult = Ok("Hello World!")
        //#content-type_text
        testContentType(textResult, "text/plain; charset=utf-8")
      }

      "default xml result Content-Type" in {
        //#content-type_xml
        val xmlResult = Ok(<message>Hello World!</message>)
        //#content-type_xml
        testContentType(xmlResult, "application/xml; charset=utf-8")
      }

      "set result Content-Type as html" in {
        //#content-type_html
        val htmlResult = Ok(<h1>Hello World!</h1>).as("text/html")
        //#content-type_html
        testContentType(htmlResult, "text/html")

        //#content-type_defined_html
        val htmlResult2 = Ok(<h1>Hello World!</h1>).as(HTML)
        //#content-type_defined_html
        testContentType(htmlResult2, "text/html; charset=utf-8")

      }

      "Manipulating HTTP headers" in {
        //#set-headers
        val result = Ok("Hello World!").withHeaders(
          CACHE_CONTROL -> "max-age=3600",
          ETAG -> "xx")
        //#set-headers
        testHeader(result, CACHE_CONTROL, "max-age=3600")
        testHeader(result, ETAG, "xx")
      }

      "Setting and discarding cookies" in {
        //#set-cookies
        val result = Ok("Hello world").withCookies(
          Cookie("theme", "blue"))
        //#set-cookies
        testHeader(result, SET_COOKIE, "theme=blue")
        //#discarding-cookies
        val result2 = result.discardingCookies(DiscardingCookie("theme"))
        //#discarding-cookies
        testHeader(result2, SET_COOKIE, "theme=;")
        //#setting-discarding-cookies
        val result3 = result.withCookies(Cookie("theme", "blue")).discardingCookies(DiscardingCookie("skin"))
        //#setting-discarding-cookies
        testHeader(result3, SET_COOKIE, "skin=;")
        testHeader(result3, SET_COOKIE, "theme=blue;")
        
      }

      "Changing the charset for text based HTTP responses" in {
        val index = new scalaguide.http.scalaresults.full.Application().index
        val resultFut: Future[Result] = index.apply(FakeRequest())
        val bodyText: String = contentAsString(resultFut)
        bodyText mustBe "<h1>Hello World!</h1>"
        val result = scala.concurrent.Await.result(resultFut, scala.concurrent.duration.Duration.Inf)
        result.body.contentType mustBe Some("text/html; charset=iso-8859-1")
      }

      "HTML method works" in {
        val result = scalaguide.http.scalaresults.full.CodeShow.HTML(Codec.javaSupported("iso-8859-1"))
        result mustBe "text/html; charset=iso-8859-1"
      }
    }

    def testContentType(results: Result, contentType: String) = {
      results.body.contentType mustBe Some(contentType)
    }

    def testHeader(results: Result, key: String, value: String) = {
      results.header.headers.get(key).get == value
    }

  }

  package scalaguide.http.scalaresults.full {
    //#full-application-set-myCustomCharset
    class Application extends Controller {

      implicit val myCustomCharset = Codec.javaSupported("iso-8859-1")

      def index = Action {
        Ok(<h1>Hello World!</h1>).as(HTML)
      }

    }
    //#full-application-set-myCustomCharset
 

  object CodeShow {
    //#Source-Code-HTML
    def HTML(implicit codec: Codec) = {
      "text/html; charset=" + codec.charset
    }
    //#Source-Code-HTML
  }
   }
}
