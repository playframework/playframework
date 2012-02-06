package play.core.j

import play.api.mvc._
import play.mvc.{ Action => JAction, Result => JResult }
import play.mvc.Http.{ Context => JContext, Request => JRequest, RequestBody => JBody, Cookies => JCookies, Cookie => JCookie }
import scala.collection.JavaConverters._

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

      val enumerator = Enumerator.imperative[A]()

      val socketOut = new play.mvc.WebSocket.Out[A] {

        def write(frame: A) {
          enumerator.push(frame)
        }

        def close() {
          enumerator.close()
        }

      }
      val socketIn = new play.mvc.WebSocket.In[A]

      in |>> {
        Iteratee.foreach[A](msg => socketIn.callbacks.asScala.foreach(_.invoke(msg))).mapDone { _ =>
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

  def ofJson(retrieveWebSocket: => play.mvc.WebSocket[org.codehaus.jackson.JsonNode]): Handler = webSocketWrapper[org.codehaus.jackson.JsonNode](retrieveWebSocket)
}
