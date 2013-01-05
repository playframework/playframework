package play.core.server.netty

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._

import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._

import scala.collection.JavaConverters._
import collection.immutable.TreeMap
import play.core.utils.CaseInsensitiveOrdered

private[netty] trait Helpers {

  def socketOut[A](ctx: ChannelHandlerContext)(frameFormatter: play.api.mvc.WebSocket.FrameFormatter[A]): Iteratee[A, Unit] = {
    val channel = ctx.getChannel()
    val nettyFrameFormatter = frameFormatter.asInstanceOf[play.core.server.websocket.FrameFormatter[A]]

    def step(future: Option[ChannelFuture])(input: Input[A]): Iteratee[A, Unit] =
      input match {
        case El(e) => Cont(step(Some(channel.write(nettyFrameFormatter.toFrame(e)))))
        case e @ EOF => future.map(_.addListener(ChannelFutureListener.CLOSE)).getOrElse(channel.close()); Done((), e)
        case Empty => Cont(step(future))
      }

    Enumeratee.breakE[A](_ => !channel.isConnected()).transform(Cont(step(None)))
  }

  def getHeaders(nettyRequest: HttpRequest): Headers = {

    //todo: wrap the underlying map in a structure more efficient than TreeMap
    val pairs = nettyRequest.getHeaderNames.asScala.map { key =>
      key -> nettyRequest.getHeaders(key).asScala
    }

    new Headers { val data = pairs.toSeq }

  }

  def getCookies(nettyRequest: HttpRequest): Cookies = {

    val cookies: Map[String, play.api.mvc.Cookie] = getHeaders(nettyRequest).get(play.api.http.HeaderNames.COOKIE).map { cookiesHeader =>
      new CookieDecoder().decode(cookiesHeader).asScala.map { c =>
        c.getName -> play.api.mvc.Cookie(
          c.getName, c.getValue, if (c.getMaxAge == Integer.MIN_VALUE) None else Some(c.getMaxAge),
          Option(c.getPath).getOrElse("/"), Option(c.getDomain), c.isSecure, c.isHttpOnly)
      }.toMap
    }.getOrElse(Map.empty)

    new Cookies {
      def get(name: String) = cookies.get(name)
      override def toString = cookies.toString
    }

  }

  /**
   * Copy headers to netty response
   * @param headers
   * @param nettyResponse
   */
  def setNettyHeaders(headers: Map[String, String], nettyResponse: DefaultHttpResponse) {
    headers.foreach {

      // Fix a bug for Set-Cookie header.
      // Multiple cookies could be merged in a single header
      // but it's not properly supported by some browsers
      case (name@play.api.http.HeaderNames.SET_COOKIE, value) => {

        import scala.collection.JavaConverters._
        import play.api.mvc._

        nettyResponse.setHeader(name, Cookies.decode(value).map {
          c => Cookies.encode(Seq(c))
        }.asJava)

      }

      case (name, value) => nettyResponse.setHeader(name, value)
    }
  }
}
