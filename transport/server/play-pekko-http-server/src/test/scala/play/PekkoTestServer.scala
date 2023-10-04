/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play

import scala.concurrent.Future

import org.apache.pekko.http.scaladsl.model._
import play.api.mvc._
import play.api.mvc.pekkohttp.PekkoHttpHandler
import play.api.routing.sird._
import play.core.server._

object PekkoTestServer extends App {
  val port: Int = 9000

  private val serverConfig = ServerConfig(port = Some(port), address = "127.0.0.1")

  val server = PekkoHttpServer.fromRouterWithComponents(serverConfig) { c =>
    {
      case GET(p"/") =>
        c.defaultActionBuilder { implicit req => Results.Ok(s"Hello world") }
      case GET(p"/pekkoHttpApi") =>
        PekkoHttpHandler { request =>
          Future.successful(
            HttpResponse(StatusCodes.OK, entity = HttpEntity("Responded using Pekko HTTP HttpResponse API"))
          )
        }
    }
  }
  println("Server (Pekko HTTP) started: http://127.0.0.1:9000/ ")

  // server.stop()
}
