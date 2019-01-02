/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play

import akka.http.scaladsl.model._
import play.core.server._
import play.api.routing.sird._
import play.api.mvc._
import play.api.mvc.akkahttp.AkkaHttpHandler

import scala.concurrent.Future

object AkkaTestServer extends App {

  val port: Int = 9000

  private val serverConfig = ServerConfig(port = Some(port), address = "127.0.0.1")

  val server = AkkaHttpServer.fromRouterWithComponents(serverConfig) { c =>
    {
      case GET(p"/") => c.defaultActionBuilder{ implicit req =>
        Results.Ok(s"Hello world")
      }
      case GET(p"/akkaHttpApi") => AkkaHttpHandler { request =>
        Future.successful(HttpResponse(StatusCodes.OK, entity = HttpEntity("Responded using Akka HTTP HttpResponse API")))
      }
    }
  }
  println("Server (Akka HTTP) started: http://127.0.0.1:9000/ ")

  // server.stop()
}
