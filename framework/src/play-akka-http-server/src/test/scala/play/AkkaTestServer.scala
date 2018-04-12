/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play

import akka.http.scaladsl.model._
import play.core.server._
import play.api.routing.sird._
import play.api.mvc._
import play.core.server.akkahttp.AkkaHttpHandler

import scala.concurrent.Future

object AkkaTestServer extends App {

  lazy val Action = new ActionBuilder.IgnoringBody()(_root_.controllers.Execution.trampoline)

  val port: Int = 9000
  val server = AkkaHttpServer.fromRouter(ServerConfig(
    port = Some(port),
    address = "127.0.0.1"
  )) {
    case GET(p"/") => Action { implicit req =>
      Results.Ok(s"Hello world")
    }
    case GET(p"/akkaHttpApi") => new AkkaHttpHandler {
      override def apply(request: HttpRequest): Future[HttpResponse] =
        Future.successful(
          HttpResponse(StatusCodes.OK, entity = HttpEntity("Responded using Akka HTTP HttpResponse API"))
        )
    }
  }
  println("Server (Akka HTTP) started: http://127.0.0.1:9000/ ")

  // server.stop()
}
