/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

//#ws-standalone
import akka.actor.ActorSystem
import akka.stream.{Materializer, SystemMaterializer}
import play.api.libs.ws._
import play.api.libs.ws.ahc.{AhcWSClient, StandaloneAhcWSClient}
import play.shaded.ahc.org.asynchttpclient.{AsyncHttpClient, AsyncHttpClientConfig, DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig}

import scala.concurrent.Future

object Main {
  import scala.concurrent.ExecutionContext.Implicits._

  def main(args: Array[String]): Unit = {
    implicit val system       = ActorSystem()

    val asyncHttpClientConfig = new DefaultAsyncHttpClientConfig.Builder()
      .setMaxRequestRetry(0)
      .setShutdownQuietPeriod(0)
      .setShutdownTimeout(0).build
    val asyncHttpClient = new DefaultAsyncHttpClient(asyncHttpClientConfig)

    implicit val materializer = SystemMaterializer(system).materializer
    val wsClient: WSClient    = new AhcWSClient(new StandaloneAhcWSClient(asyncHttpClient))

    call(wsClient)
      .andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }
  }

  def call(wsClient: WSClient): Future[Unit] = {
    wsClient.url("http://www.google.com").get().map { response =>
      val statusText: String = response.statusText
      println(s"Got a response $statusText")
    }
  }
}
//#ws-standalone
