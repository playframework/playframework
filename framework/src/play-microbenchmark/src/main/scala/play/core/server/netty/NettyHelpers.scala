package play.core.server.netty

import java.net.InetSocketAddress

import io.netty.handler.codec.http.{ DefaultHttpRequest, HttpMethod, HttpRequest, HttpVersion }
import play.core.server.common.ForwardedHeaderHandler

object NettyHelpers {

  val conversion: NettyModelConversion = new NettyModelConversion(
    new ForwardedHeaderHandler(ForwardedHeaderHandler.ForwardedHeaderHandlerConfig(None))
  )

  val localhost: InetSocketAddress = new InetSocketAddress("127.0.0.1", 9999)

  def nettyRequest(
    method: String = "GET",
    target: String = "/",
    headers: List[(String, String)] = Nil): HttpRequest = {
    val r = new DefaultHttpRequest(HttpVersion.valueOf("HTTP/1.1"), HttpMethod.valueOf(method), target)
    for ((name, value) <- headers) {
      r.headers().add(name, value)
    }
    r
  }
}
