package play.it.http

import org.specs2.mutable._
import play.api.mvc.{Result, Results, Action}
import play.api.test.Helpers._
import play.api.test._
import play.api.libs.ws.Response
import play.api.libs.iteratee.Enumerator

object ScalaResultsHandlingSpec extends Specification {


  "scala body handling" should {

    def makeRequest[T](result: Result)(block: Response => T) = {
      implicit val port = testServerPort
      running(TestServer(port, FakeApplication(
        withRoutes = {
          case _ => Action(result)
        }
      ))) {
        val response = await(wsUrl("/").get())
        block(response)
      }
    }

    "buffer results with no content length" in makeRequest(Results.Ok("Hello world")) { response =>
      response.header(CONTENT_LENGTH) must beSome("11")
      response.body must_== "Hello world"
    }

    "send results as is with a content length" in makeRequest(Results.Ok("Hello world")
      .withHeaders(CONTENT_LENGTH -> "5")) { response =>
      response.header(CONTENT_LENGTH) must beSome("5")
      response.body must_== "Hello"
    }

    "chunk results that are streamed" in makeRequest(
      Results.Ok.stream(Enumerator("a", "b", "c") >>> Enumerator.eof)
    ) { response =>
      response.header(TRANSFER_ENCODING) must beSome("chunked")
      response.header(CONTENT_LENGTH) must beNone
      response.body must_== "abc"
    }

    /* Skipped until feeding bug is fixed
    "close the connection for streamed results that are not chunked" in makeRequest(
      Results.Ok.feed(Enumerator("a", "b", "c"))
    ) { response =>
      response.header(TRANSFER_ENCODING) must beNone
      response.header(CONTENT_LENGTH) must beNone
      response.body must_== "abc"
    }
    */
  }

}
