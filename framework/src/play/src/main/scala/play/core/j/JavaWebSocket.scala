package play.core.j

import play.api.mvc._
import play.mvc.{ Action => JAction, Result => JResult }
import play.mvc.Http.{ Context => JContext, Request => JRequest, RequestBody => JBody, Cookies => JCookies, Cookie => JCookie }
import scala.collection.JavaConverters._

import play.core.Execution.Implicits.internalContext

/**
 * handles a scala websocket in a Java Context
 */
object JavaWebSocket extends JavaHelpers {

  def webSocketWrapper[A](retrieveWebSocket: => play.mvc.WebSocket[A])(implicit frameFormatter: play.api.mvc.WebSocket.FrameFormatter[A]): WebSocket[A] = WebSocket[A] { request =>
    (in, out) =>

      import play.api.libs.iteratee._

      val javaWebSocket = try {
        JContext.current.set(createJavaContext(request))
        retrieveWebSocket
      } finally {
        JContext.current.remove()
      }

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

      in |>> {
        Iteratee.foreach[A](msg => socketIn.callbacks.asScala.foreach(_.invoke(msg))).map { _ =>
          socketIn.closeCallbacks.asScala.foreach(_.invoke())
        }
      }

      enumerator |>> out

      javaWebSocket.onReady(socketIn, socketOut)

  }

  // -- Bytes

  def ofBytes(retrieveWebSocket: => play.mvc.WebSocket[Array[Byte]]): Handler = webSocketWrapper[Array[Byte]](retrieveWebSocket)

  // -- String

  def ofString(retrieveWebSocket: => play.mvc.WebSocket[String]): Handler = webSocketWrapper[String](retrieveWebSocket)

  // -- Json (JsonNode)

  implicit val jsonFrame = play.api.mvc.WebSocket.FrameFormatter.stringFrame.transform(
    play.libs.Json.stringify, play.libs.Json.parse
  )

  def ofJson(retrieveWebSocket: => play.mvc.WebSocket[com.fasterxml.jackson.databind.JsonNode]): Handler = webSocketWrapper[com.fasterxml.jackson.databind.JsonNode](retrieveWebSocket)
}
