package play.api.mvc

import org.openjdk.jmh.annotations.{ Level, Setup }
import play.core.server.netty.NettyHelpers

object MvcHelpers {
  def requestHeader(headerList: List[(String, String)]): RequestHeader = {
    val rawRequest = NettyHelpers.nettyRequest(headers = headerList)
    NettyHelpers.conversion.convertRequest(1L, NettyHelpers.localhost, None, rawRequest).get
  }
}
