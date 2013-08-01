package play.it.http

import play.api.mvc._
import play.api.test._
import play.api.test.TestServer
import play.api.libs.iteratee._
import scala.concurrent.ExecutionContext.Implicits._
import scala.util.Random

object RequestBodyHandlingSpec extends PlaySpecification {

  "Play request body handling" should {

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

    "handle large bodies" in withServer(EssentialAction { rh =>
      Iteratee.ignore[Array[Byte]].map(_ => Results.Ok)
    }) { port =>
      val body = new String(Random.alphanumeric.take(50 * 1024).toArray)
      val responses = BasicHttpClient.makeRequests(port, trickleFeed = Some(100L))(
        BasicRequest("POST", "/", "HTTP/1.1", Map("Content-Length" -> body.length.toString), body),
        // Second request ensures that Play switches back to its normal handler
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
      )
      responses.length must_== 2
      responses(0).status must_== 200
      responses(1).status must_== 200
    }

    "gracefully handle early body parser termination" in withServer(EssentialAction { rh =>
      Traversable.takeUpTo[Array[Byte]](20 * 1024) &>> Iteratee.ignore[Array[Byte]].map(_ => Results.Ok)
    }) { port =>
      val body = new String(Random.alphanumeric.take(50 * 1024).toArray)
      // Trickle feed is important, otherwise it won't switch to ignoring the body.
      val responses = BasicHttpClient.makeRequests(port, trickleFeed = Some(100L))(
        BasicRequest("POST", "/", "HTTP/1.1", Map("Content-Length" -> body.length.toString), body),
        // Second request ensures that Play switches back to its normal handler
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
      )
      responses.length must_== 2
      responses(0).status must_== 200
      responses(1).status must_== 200
    }
  }
}
