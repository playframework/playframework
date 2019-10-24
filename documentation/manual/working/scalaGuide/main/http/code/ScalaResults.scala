/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.http.scalaresults {

  import java.io.ByteArrayInputStream
  import java.io.File

  import akka.stream.scaladsl.Source
  import akka.util.ByteString
  import play.api.mvc.request
  import play.api.mvc._
  import play.api.test._
  import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner

  import scala.concurrent.Future
  import org.specs2.execute.AsResult
  import play.api.Application

  @RunWith(classOf[JUnitRunner])
  class ScalaResultsSpec extends AbstractController(Helpers.stubControllerComponents()) with PlaySpecification {

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
        val result = Ok("Hello World!").withHeaders(CACHE_CONTROL -> "max-age=3600", ETAG -> "xx")
        //#set-headers
        testHeader(result, CACHE_CONTROL, "max-age=3600")
        testHeader(result, ETAG, "xx")
      }

      "Setting and discarding cookies" in {
        //#set-cookies
        val result = Ok("Hello world")
          .withCookies(Cookie("theme", "blue"))
          .bakeCookies()
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
        val index = new scalaguide.http.scalaresults.full.Application(Helpers.stubControllerComponents()).index
        assertAction(index)((_, res) => testContentType(await(res), "charset=iso-8859-1"))
      }

      "HTML method works" in {
        val result = scalaguide.http.scalaresults.full.CodeShow.HTML(Codec.javaSupported("iso-8859-1"))
        result must contain("iso-8859-1")
      }

      "Partial Content based on Range header" in {

        "for Sources" in {
          val request = FakeRequest().withHeaders("Range" -> "bytes=0-5")

          //#range-result-source
          val header  = request.headers.get(RANGE)
          val content = "This is the full body!"
          val source  = Source.single(ByteString(content))

          val partialContent = RangeResult.ofSource(
            entityLength = content.length,
            source = source,
            rangeHeader = header,
            fileName = Some("file.txt"),
            contentType = Some(TEXT)
          )
          //#range-result-source

          testContentType(partialContent, "text/plain")
          partialContent.header.status must beEqualTo(PARTIAL_CONTENT)
        }

        "for InputStream" in {
          val request = FakeRequest().withHeaders("Range" -> "bytes=0-5")

          //#range-result-input-stream
          val input          = getInputStream()
          val partialContent = RangeResult.ofStream(input, request.headers.get(RANGE), "video.mp4", Some("video/mp4"))
          //#range-result-input-stream

          testContentType(partialContent, "video/mp4")
          partialContent.header.status must beEqualTo(PARTIAL_CONTENT)
        }

        "with an offset" in {

          class PartialContentController(val controllerComponents: ControllerComponents) extends BaseController {

            private def sourceFrom(content: String): Source[ByteString, _] =
              Source(content.getBytes.iterator.map(ByteString(_)).toIndexedSeq)

            def index = Action { request =>
              //#range-result-source-with-offset
              val header  = request.headers.get(RANGE)
              val content = "This is the full body!"
              val source  = sourceFrom(content)

              val partialContent = RangeResult.ofSource(
                entityLength = Some(content.length),
                getSource = offset => (offset, source.drop(offset)),
                rangeHeader = header,
                fileName = Some("file.txt"),
                contentType = Some(TEXT)
              )
              //#range-result-source-with-offset

              partialContent
            }
          }

          val action = new PartialContentController(Helpers.stubControllerComponents()).index
          assertAction(
            action,
            expectedResponse = PARTIAL_CONTENT,
            request = FakeRequest().withHeaders(RANGE -> "bytes=8-10")
          ) { (app, res) =>
            implicit val mat = app.materializer
            contentAsString(res) must beEqualTo("the")
          }
        }
      }
    }

    // It does not matter which file is returned since it won't be read
    private def getInputStream() = new ByteArrayInputStream("Content".getBytes)

    def testContentType(results: Result, contentType: String) = {
      results.body.contentType must beSome.which { _ must contain(contentType) }
    }

    def testHeader(results: Result, key: String, value: String) = {
      results
        .bakeCookies() // bake cookies with default configuration
        .header
        .headers
        .get(key)
        .get must contain(value)
    }

    def testAction[A](action: Action[A], expectedResponse: Int = OK, request: Request[A] = FakeRequest()) = {
      assertAction(action, expectedResponse, request) { (_, _) =>
        success
      }
    }

    def assertAction[A, T: AsResult](
        action: Action[A],
        expectedResponse: Int = OK,
        request: Request[A] = FakeRequest()
    )(assertions: (Application, Future[Result]) => T) = {
      running() { app =>
        val result = action(request)
        status(result) must_== expectedResponse
        assertions(app, result)
      }
    }
  }

  package scalaguide.http.scalaresults.full {

    import javax.inject.Inject

    //#full-application-set-myCustomCharset
    class Application @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

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
