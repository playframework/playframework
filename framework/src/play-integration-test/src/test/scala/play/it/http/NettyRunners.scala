/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http

import akka.util.Timeout
import java.net.SocketAddress
import org.jboss.netty.channel._
import org.jboss.netty.channel.group.DefaultChannelGroup
import play.api.{ Play, Application, Mode }
import play.api.mvc._
import play.api.test._
import play.core.ApplicationProvider
import play.core.Router.Routes
import play.core.server.Server
import play.core.server.netty.PlayDefaultUpstreamHandler
import scala.util.Success

/**
 * Provides support for running PlayDefaultUpstreamHandler with Netty.
 */
trait NettyRunners extends PlayRunners {

  /** A Channel with minimal functionality implemented. */
  class StubChannel(pipeline: ChannelPipeline, remoteAddress: SocketAddress) extends Channel {
    private val closeFuture = new DefaultChannelFuture(this, false)
    final override def getCloseFuture: ChannelFuture = closeFuture

    def getId: Integer = ???
    def getFactory: ChannelFactory = ???
    def getParent: Channel = ???
    def getConfig: ChannelConfig = ???
    def getPipeline: ChannelPipeline = pipeline
    def isOpen: Boolean = ???
    def isBound: Boolean = ???
    def isConnected: Boolean = ???
    def getLocalAddress: SocketAddress = ???
    def getRemoteAddress: SocketAddress = remoteAddress
    def write(message: Any): ChannelFuture = ???
    def write(message: Any, remoteAddress: SocketAddress): ChannelFuture = ???
    def bind(localAddress: SocketAddress): ChannelFuture = ???
    def connect(remoteAddress: SocketAddress): ChannelFuture = ???
    def disconnect(): ChannelFuture = ???
    def unbind(): ChannelFuture = ???
    def close(): ChannelFuture = ???
    def getInterestOps: Int = ???
    def isReadable: Boolean = ???
    def isWritable: Boolean = ???
    def setInterestOps(interestOps: Int): ChannelFuture = ???
    def setReadable(readable: Boolean): ChannelFuture = ???
    def getAttachment: AnyRef = ???
    def setAttachment(attachment: Any) { ??? }
    def compareTo(o: Channel): Int = ???
  }

  /**
   * Test Netty handling against an application.
   */
  def withDownstreamHandler[T](
      downstreamHandler: ChannelDownstreamHandler,
      app: Application)(block: ChannelPipeline => T): T = {
    running(app) {
      // Create a minimal Server needed by PlayDefaultUpstreamHandler
      val appProvider = new ApplicationProvider {
        def get = Success(app)
            def path = app.path
      }
      val server = new Server {
        def mode: Mode.Mode = Mode.Test
            def applicationProvider: ApplicationProvider = appProvider
      }
      // Create a PlayDefaultUpstreamHandler and an interface to interact with it
      val duh = new PlayDefaultUpstreamHandler(server, new DefaultChannelGroup())
      val pipeline = Channels.pipeline()
      pipeline.addFirst("play-upstream-handler", duh)
      pipeline.addFirst("test-downstream-handler", downstreamHandler)
      try block(pipeline) finally Play.stop()
    }
  }
  
  /**
   * Test Netty handling against an EssentialAction.
   */
  def withDownstreamHandler[T](
      downstreamHandler: ChannelDownstreamHandler,
      action: EssentialAction)(block: ChannelPipeline => T): T = {
    val app = new FakeApplication() {
      override lazy val routes = Some(new Routes {
        def prefix = "/"
        def setPrefix(prefix: String) {}
        def documentation = Nil
        def routes = {
          case _ => action
        }
      })
    }
    withDownstreamHandler(downstreamHandler, app)(block)
  }

}