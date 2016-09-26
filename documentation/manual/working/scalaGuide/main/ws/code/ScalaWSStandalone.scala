/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
 
//#ws-standalone
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws._
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Future

object Main {
  import scala.concurrent.ExecutionContext.Implicits._

  def main(args: Array[String]): Unit = {
    println("Hello, world!")

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    val wsClient = AhcWSClient()

    call(wsClient).andThen {
      case _ =>
        system.terminate()
    }
  }

  def call(wsClient: WSClient): Future[Unit] = {
    wsClient.url("http://www.google.com").get().map { response =>
      println("Got a response!")
      println(response.allHeaders)
    }.andThen {
      case _ =>
        wsClient.close()
    }
  }
}
//#ws-standalone