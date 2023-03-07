/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.netty

import java.net.InetSocketAddress
import java.net.SocketAddress
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine

import io.netty.channel._
import io.netty.handler.codec.http.DefaultHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.ssl.SslHandler
import play.api.http.HttpConfiguration
import play.api.libs.crypto.CookieSignerProvider
import play.api.mvc.DefaultCookieHeaderEncoding
import play.api.mvc.DefaultFlashCookieBaker
import play.api.mvc.DefaultSessionCookieBaker
import play.core.server.common.ForwardedHeaderHandler
import play.core.server.common.ServerResultUtils

object NettyHelpers {
  val conversion: NettyModelConversion = {
    val httpConfig = HttpConfiguration()
    val serverResultUtils = new ServerResultUtils(
      new DefaultSessionCookieBaker(
        httpConfig.session,
        httpConfig.secret,
        new CookieSignerProvider(httpConfig.secret).get
      ),
      new DefaultFlashCookieBaker(httpConfig.flash, httpConfig.secret, new CookieSignerProvider(httpConfig.secret).get),
      new DefaultCookieHeaderEncoding(httpConfig.cookies)
    )
    new NettyModelConversion(
      serverResultUtils,
      new ForwardedHeaderHandler(ForwardedHeaderHandler.ForwardedHeaderHandlerConfig(None)),
      None
    )
  }

  val localhost: InetSocketAddress = new InetSocketAddress("127.0.0.1", 9999)
  val sslEngine: SSLEngine         = SSLContext.getDefault.createSSLEngine()

  def nettyChannel(remoteAddress: SocketAddress, ssl: Boolean): Channel = {
    val ra = remoteAddress
    val c = new AbstractChannel(null) {
      // Methods used in testing
      override def remoteAddress: SocketAddress = ra
      // Stubs
      override def doDisconnect(): Unit = ???
      override def newUnsafe(): AbstractChannel#AbstractUnsafe = new AbstractUnsafe {
        override def connect(remoteAddress: SocketAddress, localAddress: SocketAddress, promise: ChannelPromise): Unit =
          ???
      }
      override def isCompatible(loop: EventLoop): Boolean    = ???
      override def localAddress0(): SocketAddress            = ???
      override def doWrite(in: ChannelOutboundBuffer): Unit  = ???
      override def remoteAddress0(): SocketAddress           = ???
      override def doClose(): Unit                           = ???
      override def doBind(localAddress: SocketAddress): Unit = ???
      override def doBeginRead(): Unit                       = ???
      override def config(): ChannelConfig                   = ???
      override def metadata(): ChannelMetadata               = ???
      override def isActive: Boolean                         = ???
      override def isOpen: Boolean                           = ???
    }
    if (ssl) {
      c.pipeline().addLast("ssl", new SslHandler(sslEngine))
    }
    c
  }

  def nettyRequest(method: String = "GET", target: String = "/", headers: List[(String, String)] = Nil): HttpRequest = {
    val r = new DefaultHttpRequest(HttpVersion.valueOf("HTTP/1.1"), HttpMethod.valueOf(method), target)
    for ((name, value) <- headers) {
      r.headers().add(name, value)
    }
    r
  }
}
