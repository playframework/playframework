/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import java.net.{ HttpURLConnection, URL }

import akka.stream.scaladsl._
import org.specs2.mutable.Specification
import play.api.Mode
import play.api.mvc.WebSocket
import play.api.routing.sird._

class AkkaHttpServerTest extends Specification {

  "AkkaHttpServer" should {
    "Return 426 when receiving a (non-upgrade) request on a websocket action" in {
      val server = AkkaHttpServer.fromRouterWithComponents(
        ServerConfig(mode = Mode.Test)
      ) { components =>
          {
            case GET(p"/hello") => WebSocket.accept[String, String] { request =>
              val in = Sink.foreach[String](println)
              val out = Source.single("Hello!").concat(Source.maybe)
              Flow.fromSinkAndSource(in, out)
            }
          }
        }

      val connection = new URL("http", "localhost", server.httpPort.get, "/hello").openConnection().asInstanceOf[HttpURLConnection]
      val status = connection.getResponseCode

      status mustEqual 426

    }
  }

}
