package play

import play.core.server._
import play.api.routing.sird._
import play.api.mvc._

import akka._
import akka.http._

object AkkaTestServer extends App {

  val port: Int = 9000
  val server = AkkaHttpServer.fromRouter(ServerConfig(
    port = Some(port),
    address = "127.0.0.1"
  )) {
    case GET(p"/") => Action { implicit req =>
      Results.Ok(s"Hello world")
    }
  }
  println("Server (Akka HTTP) started: http://127.0.0.1:9000/ ")

  // server.stop()
}
