package play.it.http

import org.specs2.mutable._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.ws.Response
import play.api.libs.iteratee._

import play.core.Router.Routes

object ScalaResultsHandlingSpec extends Specification {

  "scala body handling" should {

    def makeRequest[T](result: Result)(block: Response => T) = withServer(result) { implicit port =>
      val response = await(wsUrl("/").get())
      block(response)
    }

    def withServer[T](result: Result)(block: Port => T) = {
      val port = testServerPort
      running(TestServer(port, new FakeApplication() {
        override lazy val routes = Some(new Routes {
          def prefix = "/"
          def setPrefix(prefix: String) {}
          def documentation = Nil
          def routes = {
            case _ => Action(result)
          }
        })
      })) {
        block(port)
      }
    }

    "buffer results with no content length" in makeRequest(Results.Ok("Hello world")) { response =>
      response.header(CONTENT_LENGTH) must beSome("11")
      response.body must_== "Hello world"
    }

    "send results with a content length as is" in makeRequest(Results.Ok("Hello world")
      .withHeaders(CONTENT_LENGTH -> "5")) { response =>
      response.header(CONTENT_LENGTH) must beSome("5")
      response.body must_== "Hello"
    }

    "chunk results for chunked streaming strategy" in makeRequest(
      Results.Ok.stream(Enumerator("a", "b", "c"))
    ) { response =>
      response.header(TRANSFER_ENCODING) must beSome("chunked")
      response.header(CONTENT_LENGTH) must beNone
      response.body must_== "abc"
    }

    "close the connection for feed results" in makeRequest(
      Results.Ok.feed(Enumerator("a", "b", "c"))
    ) { response =>
      response.header(TRANSFER_ENCODING) must beNone
      response.header(CONTENT_LENGTH) must beNone
      response.body must_== "abc"
    }

    "close the connection when the connection close header is present" in withServer(
      Results.Ok
    ) { port =>
      BasicHttpClient.makeRequests(port, true,
        BasicRequest("GET", "/", "HTTP/1.1", Map("Connection" -> "close"), "")
      )(0).status must_== 200
    }

    "close the connection when the connection when protocol is HTTP 1.0" in withServer(
      Results.Ok
    ) { port =>
      BasicHttpClient.makeRequests(port, true,
        BasicRequest("GET", "/", "HTTP/1.0", Map(), "")
      )(0).status must_== 200
    }

    "honour the keep alive header for HTTP 1.0" in withServer(
      Results.Ok
    ) { port =>
      val responses = BasicHttpClient.makeRequests(port, false,
        BasicRequest("GET", "/", "HTTP/1.0", Map("Connection" -> "keep-alive"), ""),
        BasicRequest("GET", "/", "HTTP/1.0", Map(), "")
      )
      responses(0).status must_== 200
      responses(1).status must_== 200
    }

    "keep alive HTTP 1.1 connections" in withServer(
      Results.Ok
    ) { port =>
      val responses = BasicHttpClient.makeRequests(port, false,
        BasicRequest("GET", "/", "HTTP/1.1", Map(), ""),
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
      )
      responses(0).status must_== 200
      responses(1).status must_== 200
    }

    "close chunked connections when requested" in withServer(
      Results.Ok.stream(Enumerator("a", "b", "c"))
    ) { port =>
      // will timeout if not closed
      BasicHttpClient.makeRequests(port, true,
        BasicRequest("GET", "/", "HTTP/1.1", Map("Connection" -> "close"), "")
      )(0).status must_== 200
    }

    "keep chunked connections alive by default" in withServer(
      Results.Ok.stream(Enumerator("a", "b", "c"))
    ) { port =>
      val responses = BasicHttpClient.makeRequests(port, false,
        BasicRequest("GET", "/", "HTTP/1.1", Map(), ""),
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
      )
      responses(0).status must_== 200
      responses(1).status must_== 200
    }
  }

}
