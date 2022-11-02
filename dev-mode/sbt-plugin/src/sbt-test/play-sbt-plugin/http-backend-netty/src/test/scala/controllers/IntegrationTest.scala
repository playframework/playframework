/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSRequest
import play.api.test.PlaySpecification
import play.api.test._

import play.api.http.HttpProtocol
import ws.WebSocketClient

class IntegrationTest extends ForServer with PlaySpecification with ApplicationFactories {

  protected def applicationFactory: ApplicationFactory = withGuiceApp(GuiceApplicationBuilder())

  def wsUrl(path: String)(implicit running: RunningServer): WSRequest = {
    val ws  = running.app.injector.instanceOf[WSClient]
    val url = running.endpoints.httpEndpoint.get.pathUrl(path)
    ws.url(url).withVirtualHost("127.0.0.1")
  }

  def websocketUrl(path: String)(implicit running: RunningServer): String = {
    val scheme = running.endpoints.httpEndpoint.get.scheme
    val wsScheme = scheme match {
      case "http"  => "ws"
      case "https" => "wss"
    }
    running.endpoints.httpEndpoint.get.pathUrl(path).replace(s"$scheme://", s"$wsScheme://")
  }

  "Integration test" should {

    "use the controller successfully" >> { implicit rs: RunningServer =>
      val result = await(wsUrl("/").get())
      result.status must ===(200)
    }

    "use the user-configured HTTP backend during test" >> { implicit rs: RunningServer =>
      val result = await(wsUrl("/").get())
      // This assertion indirectly checks the HTTP backend used during tests is that configured
      // by the user on `build.sbt`.
      result.header("Server") must ===(Some("Netty Server"))
    }

    "use the user-configured HTTP transports during test" >> { implicit rs: RunningServer =>
      rs.endpoints.endpoints.filter(_.protocols.contains(HttpProtocol.HTTP_2_0)) must be(Nil)
    }

    "all close status codes should be pushed to app" >> { implicit rs: RunningServer =>

      var receivedCloseCode = ""

      // We open two websockets, one that gets closed with status code 2000
      // Another one which tells us the close status code of the mentioned connection so we can check it.

      new WebSocketClient(websocketUrl("/websocket-feedback")).addHandler(new WebSocketClient.WsHandler {
        override def handleStringMessage(message: String) = receivedCloseCode = message
      }).connect();

      val ws = new WebSocketClient(websocketUrl("/websocket"))
      ws.addHandler(new WebSocketClient.WsHandler {
        //
        // Immediately after opening the connection we close it again.
        //
        // According to netty, close status code 2000 is invalid:
        // https://github.com/netty/netty/blob/netty-4.1.84.Final/codec-http/src/main/java/io/netty/handler/codec/http/websocketx/WebSocketCloseStatus.java#L286-L291
        // That's kind of true, because it's reserved for the protocol itself, not for users: https://www.rfc-editor.org/rfc/rfc6455#section-7.4.2
        // However, the akka-http backend does not care and pushes all status code down to the application,
        // so the netty backend should do the same.
        override def onOpen() = ws.close(2000)
      }).connect();

      Thread.sleep(1000) // Give feedback-websocket time to send message to client

      receivedCloseCode mustEqual "2000"
    }

  }
}
