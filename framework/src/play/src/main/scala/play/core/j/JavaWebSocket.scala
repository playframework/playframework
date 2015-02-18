/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.j

import play.api.mvc._
import play.mvc.Http.{ Context => JContext }
import play.mvc.{ WebSocket => JWebSocket }
import play.libs.F.{ Promise => JPromise }
import scala.collection.JavaConverters._

import scala.concurrent.Future
import com.fasterxml.jackson.databind.JsonNode
import play.core.actors.WebSocketActor._
import play.api.libs.concurrent.Akka

import play.api.Play.current
import play.core.Execution.Implicits.internalContext
import scala.reflect.ClassTag

/**
 * handles a scala websocket in a Java Context
 */
object JavaWebSocket extends JavaHelpers {

  def webSocketWrapper[A](retrieveWebSocket: => Future[JWebSocket[A]])(implicit frameFormatter: WebSocket.FrameFormatter[A], mt: ClassTag[A]): WebSocket[A, A] = WebSocket[A, A] { request =>

    val javaContext = createJavaContext(request)

    val javaWebSocket = try {
      JContext.current.set(javaContext)
      retrieveWebSocket
    } finally {
      JContext.current.remove()
    }

    javaWebSocket.map { jws =>
      val reject = Option(jws.rejectWith())
      reject.map { result =>

        Left(createResult(javaContext, result))

      } getOrElse {

        Right((in, out) => {

          if (jws.isActor) {

            WebSocketsExtension(Akka.system).actor !
              WebSocketsActor.Connect(request.id, in, out, actorRef => jws.actorProps(actorRef))

          } else {

            import play.api.libs.iteratee._

            val (enumerator, channel) = Concurrent.broadcast[A]

            val socketOut = new play.mvc.WebSocket.Out[A] {
              def write(frame: A) {
                channel.push(frame)
              }
              def close() {
                channel.eofAndEnd()
              }
            }

            val socketIn = new play.mvc.WebSocket.In[A]

            enumerator |>> out

            jws.onReady(socketIn, socketOut)

            in |>> {
              Iteratee.foreach[A](msg => socketIn.callbacks.asScala.foreach(_.invoke(msg))).map { _ =>
                socketIn.closeCallbacks.asScala.foreach(_.invoke())
              }
            }
          }
        })
      }
    }
  }

  // -- Bytes

  def ofBytes(retrieveWebSocket: => JWebSocket[Array[Byte]]): WebSocket[Array[Byte], Array[Byte]] =
    webSocketWrapper[Array[Byte]](Future.successful(retrieveWebSocket))

  def promiseOfBytes(retrieveWebSocket: => JPromise[JWebSocket[Array[Byte]]]): WebSocket[Array[Byte], Array[Byte]] =
    webSocketWrapper[Array[Byte]](retrieveWebSocket.wrapped())

  // -- String

  def ofString(retrieveWebSocket: => JWebSocket[String]): WebSocket[String, String] =
    webSocketWrapper[String](Future.successful(retrieveWebSocket))

  def promiseOfString(retrieveWebSocket: => JPromise[JWebSocket[String]]): WebSocket[String, String] =
    webSocketWrapper[String](retrieveWebSocket.wrapped())

  // -- Json (JsonNode)

  implicit val jsonFrame = WebSocket.FrameFormatter.stringFrame.transform(
    play.libs.Json.stringify, play.libs.Json.parse
  )

  def ofJson(retrieveWebSocket: => JWebSocket[JsonNode]): WebSocket[JsonNode, JsonNode] =
    webSocketWrapper[JsonNode](Future.successful(retrieveWebSocket))

  def promiseOfJson(retrieveWebSocket: => JPromise[JWebSocket[JsonNode]]): WebSocket[JsonNode, JsonNode] =
    webSocketWrapper[JsonNode](retrieveWebSocket.wrapped())
}
