/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http

import play.it._
import play.api.mvc._
import play.api.test._
import play.api.test.TestServer
import play.api.libs.iteratee._

object NettyExpect100ContinueSpec extends Expect100ContinueSpec with NettyIntegrationSpecification
object AkkaHttpExpect100ContinueSpec extends Expect100ContinueSpec with AkkaHttpIntegrationSpecification

trait Expect100ContinueSpec extends PlaySpecification with ServerIntegrationSpecification {

  "Play" should {

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

    "honour 100 continue" in withServer(Action(Results.Ok)) { port =>
      val responses = BasicHttpClient.makeRequests(port)(
        BasicRequest("POST", "/", "HTTP/1.1", Map("Expect" -> "100-continue", "Content-Length" -> "10"), "abcdefghij")
      )
      responses.length must_== 2
      responses(0).status must_== 100
      responses(1).status must_== 200
    }

    "not read body when expecting 100 continue but action iteratee is done" in withServer(
      EssentialAction(_ => Done(Results.Ok))
    ) { port =>
        val responses = BasicHttpClient.makeRequests(port)(
          BasicRequest("POST", "/", "HTTP/1.1", Map("Expect" -> "100-continue", "Content-Length" -> "100000"), "foo")
        )
        responses.length must_== 1
        responses(0).status must_== 200
      }

    // This is necessary due to an ambiguity in the HTTP spec.  Clients are instructed not to wait indefinitely for
    // the 100 continue response, but rather to just send it anyway if no response is received.  If the body is
    // rejected then, there is no way for the server to know whether the next data is the body, sent by the client
    // because it decided to stop waiting, or if it's the next request.  The only reliable option for handling it is to
    // close the connection.
    //
    // See https://issues.jboss.org/browse/NETTY-390 for more details.
    "close the connection after rejecting a Expect: 100-continue body" in withServer(
      EssentialAction(_ => Done(Results.Ok))
    ) { port =>
        val responses = BasicHttpClient.makeRequests(port, checkClosed = true)(
          BasicRequest("POST", "/", "HTTP/1.1", Map("Expect" -> "100-continue", "Content-Length" -> "100000"), "foo")
        )
        responses.length must_== 1
        responses(0).status must_== 200
      }

    "leave the Netty pipeline in the right state after accepting a 100 continue request" in withServer(
      Action(Results.Ok)
    ) { port =>
        val responses = BasicHttpClient.makeRequests(port)(
          BasicRequest("POST", "/", "HTTP/1.1", Map("Expect" -> "100-continue", "Content-Length" -> "10"), "abcdefghij"),
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        )
        responses.length must_== 3
        responses(0).status must_== 100
        responses(1).status must_== 200
        responses(2).status must_== 200
      }
  }
}
