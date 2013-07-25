package play.it.http

import play.api.mvc.{Results, EssentialAction}
import play.api.test._
import play.api.test.TestServer
import play.api.libs.concurrent.Promise
import play.api.libs.iteratee._
import java.util.concurrent.TimeUnit
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

object HttpPipeliningSpec extends PlaySpecification {

  "Play's http pipelining support" should {

    def withServer[T](action: EssentialAction)(block: Port => T) = {
      val port = testServerPort
      running(TestServer(port, FakeApplication(
        withRoutes = {
          case _ => action
        }
      ))) {
        block(port)
      }
    }

    "wait for the first response to return before returning the second" in withServer(EssentialAction { req =>
      req.path match {
        case "/long" => Iteratee.flatten(Promise.timeout(Done(Results.Ok("long")), 100, TimeUnit.MILLISECONDS))
        case "/short" => Done(Results.Ok("short"))
        case _ => Done(Results.NotFound)
      }
    }) { port =>
      val responses = BasicHttpClient.pipelineRequests(port,
        BasicRequest("GET", "/long", "HTTP/1.1", Map(), ""),
        BasicRequest("GET", "/short", "HTTP/1.1", Map(), "")
      )
      responses(0).status must_== 200
      responses(0).body must beLeft("long")
      responses(1).status must_== 200
      responses(1).body must beLeft("short")
    }

    "wait for the first response body to return before returning the second" in withServer(EssentialAction { req =>
      req.path match {
        case "/long" => Done(
          Results.Ok.chunked(Enumerator.unfoldM[Int, String](0) { chunk =>
            if (chunk < 3) {
              Promise.timeout(Some((chunk + 1, chunk.toString)), 50, TimeUnit.MILLISECONDS)
            } else {
              Future.successful(None)
            }
          })
        )
        case "/short" => Done(Results.Ok("short"))
        case _ => Done(Results.NotFound)
      }
    }) { port =>
      val responses = BasicHttpClient.pipelineRequests(port,
        BasicRequest("GET", "/long", "HTTP/1.1", Map(), ""),
        BasicRequest("GET", "/short", "HTTP/1.1", Map(), "")
      )
      responses(0).status must_== 200
      responses(0).body must beRight
      responses(0).body.right.get._1 must containAllOf(Seq("0", "1", "2")).inOrder
      responses(1).status must_== 200
      responses(1).body must beLeft("short")
    }

  }
}
