package scalaguide.http.scalaresults {

  import play.api.mvc._
  import play.api.test._
  import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner
  import play.api.http.HeaderNames
  import scala.concurrent.Future
  import org.specs2.execute.AsResult

  @RunWith(classOf[JUnitRunner])
  class ScalaResultsSpec extends PlaySpecification with Controller {

    "A scala result" should {
      "default result Content-Type" in {
        //#content-type_text
        val textResult = Ok("Hello World!")
        //#content-type_text
        testContentType(textResult, "text/plain")
      }

      "default xml result Content-Type" in {
        //#content-type_xml
        val xmlResult = Ok(<message>Hello World!</message>)
        //#content-type_xml
        testContentType(xmlResult, "application/xml")
      }

      "set result Content-Type as html" in {
        //#content-type_html
        val htmlResult = Ok(<h1>Hello World!</h1>).as("text/html")
        //#content-type_html
        testContentType(htmlResult, "text/html")

        //#content-type_defined_html
        val htmlResult2 = Ok(<h1>Hello World!</h1>).as(HTML)
        //#content-type_defined_html
        testContentType(htmlResult2, "text/html")

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
      }

      "Changing the charset for text based HTTP responses" in {
        val index = scalaguide.http.scalaresults.full.Application.index
        assertAction(index)(res => testContentType(await(res), "charset=iso-8859-1"))
      }

       "HTML method works" in {
        val result = scalaguide.http.scalaresults.full.CodeShow.HTML(Codec.javaSupported("iso-8859-1"))
        result must contain("iso-8859-1")
      }
    }

    def testContentType(results: SimpleResult, contentType: String) = {
      testHeader(results, HeaderNames.CONTENT_TYPE, contentType)
    }

    def testHeader(results: SimpleResult, key: String, value: String) = {
      results.header.headers.get(key).get must contain(value)
    }

    def testAction[A](action: Action[A], expectedResponse: Int = OK, request: Request[A] = FakeRequest()) = {
      assertAction(action, expectedResponse, request) { result => success }
    }

    def assertAction[A, T: AsResult](action: Action[A], expectedResponse: Int = OK, request: Request[A] = FakeRequest())(assertions: Future[SimpleResult] => T) = {
      running(FakeApplication()) {
        val result = action(request)
        status(result) must_== expectedResponse
        assertions(result)
      }
    }
  }

  package scalaguide.http.scalaresults.full {
    //#full-application-set-myCustomCharset
    object Application extends Controller {

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
