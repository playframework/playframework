package play.core.j

import play.api.mvc._
import play.mvc.{ Action => JAction, Result => JResult }
import play.mvc.Http.{ Context => JContext, Request => JRequest, RequestBody => JBody, Cookies => JCookies, Cookie => JCookie }
import scala.collection.JavaConverters._

/**
 * handles a scala websocket in a Java Context
 */
object JavaWebSocket extends JavaHelpers {

  /**
   * @param websocket that's executed in Java Context
   */
  def ofString(retrieveWebSocket: => play.mvc.WebSocket[String]) = WebSocket[String] { request =>
    (in, out) =>

      import play.api.libs.iteratee._

      val javaWebSocket = try {
        JContext.current.set(createJavaContext(request))
        retrieveWebSocket
      } finally {
        JContext.current.remove()
      }

      val enumerator = new CallbackEnumerator[String]

      val socketOut = new play.mvc.WebSocket.Out[String](enumerator)
      val socketIn = new play.mvc.WebSocket.In[String]

      in |>> Iteratee.mapChunk_((msg: String) => socketIn.callbacks.asScala.foreach(_.invoke(msg)))

      enumerator |>> out

      javaWebSocket.onReady(socketIn, socketOut)

  }

}
