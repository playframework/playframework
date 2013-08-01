package play.it.http

import play.api.test._
import play.api.libs.ws.Response
import play.api.test.{FakeApplication, TestServer}
import play.mvc.Results
import play.mvc.Results.Chunks

object JavaResultsHandlingSpec extends PlaySpecification {

  "java body handling" should {
    def makeRequest[T](controller: MockController)(block: Response => T) = {
      implicit val port = testServerPort
      running(TestServer(port, FakeApplication(
        withRoutes = {
          case _ => JAction(controller)
        }
      ))) {
        val response = await(wsUrl("/").get())
        block(response)
      }
    }

    "buffer results with no content length" in makeRequest(new MockController {
      def action = Results.ok("Hello world")
    }) { response =>
      response.header(CONTENT_LENGTH) must beSome("11")
      response.body must_== "Hello world"
    }

    "send results as is with a content length" in makeRequest(new MockController {
      def action = {
        response.setHeader(CONTENT_LENGTH, "5")
        Results.ok("Hello world")
      }
    }) { response =>
      response.header(CONTENT_LENGTH) must beSome("5")
      response.body must_== "Hello"
    }

    "chunk results that are streamed" in makeRequest(new MockController {
      def action = {
        Results.ok(new Results.StringChunks() {
          def onReady(out: Chunks.Out[String]) {
            out.write("a")
            out.write("b")
            out.write("c")
            out.close()
          }
        })
      }
    }) { response =>
      response.header(TRANSFER_ENCODING) must beSome("chunked")
      response.header(CONTENT_LENGTH) must beNone
      response.body must_== "abc"
    }
  }
}
