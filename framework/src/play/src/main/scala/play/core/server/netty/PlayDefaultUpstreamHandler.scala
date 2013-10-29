/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.netty

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.ssl._

import org.jboss.netty.channel.group._
import play.core._
import server.Server
import play.api._
import play.api.mvc._
import play.api.http.HeaderNames.{ X_FORWARDED_FOR, X_FORWARDED_PROTO }
import play.api.libs.iteratee._
import scala.collection.JavaConverters._
import scala.util.control.Exception
import scala.concurrent.Future
import java.net.URI
import java.io.IOException
import play.core.system.{ HandlerExecutorContext, HandlerExecutor }

private[play] class PlayDefaultUpstreamHandler(server: Server, allChannels: DefaultChannelGroup,
  handlerExecutors: Seq[HandlerExecutor[NettyBackendRequest]])
    extends SimpleChannelUpstreamHandler {

  private val requestIDs = new java.util.concurrent.atomic.AtomicLong(0)

  /**
   * We don't know what the consequence of changing logging exceptions from trace to error will be.  We hope that it
   * won't have any impact, but in case it turns out that there are many exceptions that are normal occurrences, we
   * want to give people the opportunity to turn it off.
   */
  val nettyExceptionLogger = Logger("play.nettyException")

  override def exceptionCaught(ctx: ChannelHandlerContext, event: ExceptionEvent) {
    event.getCause match {
      // IO exceptions happen all the time, it usually just means that the client has closed the connection before fully
      // sending/receiving the response.
      case e: IOException => nettyExceptionLogger.trace("Benign IO exception caught in Netty", e)
      case e => nettyExceptionLogger.error("Exception caught in Netty", e)
    }
    event.getChannel.close()
  }

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    Option(ctx.getPipeline.get(classOf[SslHandler])).map { sslHandler =>
      sslHandler.handshake()
    }
  }

  override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    val cleanup = ctx.getAttachment
    if (cleanup != null) cleanup.asInstanceOf[() => Unit]()
    ctx.setAttachment(null)
  }

  override def channelOpen(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    allChannels.add(e.getChannel)
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    e.getMessage match {

      case nettyHttpRequest: HttpRequest =>

        Play.logger.trace("Http request received by netty: " + nettyHttpRequest)
        val nettyUri = new QueryStringDecoder(nettyHttpRequest.getUri)
        val rHeaders = getHeaders(nettyHttpRequest)

        def rRemoteAddress = e.getRemoteAddress match {
          case ra: java.net.InetSocketAddress =>
            val remoteAddress = ra.getAddress.getHostAddress
            forwardedHeader(remoteAddress, X_FORWARDED_FOR).getOrElse(remoteAddress)
        }

        def rSecure = e.getRemoteAddress match {
          case ra: java.net.InetSocketAddress =>
            val remoteAddress = ra.getAddress.getHostAddress
            val fh = forwardedHeader(remoteAddress, X_FORWARDED_PROTO)
            fh.map(_ == "https").getOrElse(ctx.getPipeline.get(classOf[SslHandler]) != null)
        }

        /**
         * Gets the value of a header, if the remote address is localhost or
         * if the trustxforwarded configuration property is true
         */
        def forwardedHeader(remoteAddress: String, headerName: String) = for {
          headerValue <- rHeaders.get(headerName)
          app <- server.applicationProvider.get.toOption
          trustxforwarded <- app.configuration.getBoolean("trustxforwarded").orElse(Some(false))
          if remoteAddress == "127.0.0.1" || trustxforwarded
        } yield headerValue

        def createRequestHeader(parameters: Map[String, Seq[String]] = Map.empty[String, Seq[String]]) = {
          //mapping netty request to Play's
          val untaggedRequestHeader = new RequestHeader {
            val id = requestIDs.incrementAndGet
            val tags = Map.empty[String, String]
            def uri = nettyHttpRequest.getUri
            def path = new URI(nettyUri.getPath).getRawPath //wrapping into URI to handle absoluteURI
            def method = nettyHttpRequest.getMethod.getName
            def version = nettyHttpRequest.getProtocolVersion.getText
            def queryString = parameters
            def headers = rHeaders
            lazy val remoteAddress = rRemoteAddress
            lazy val secure = rSecure
            def username = None
          }
          untaggedRequestHeader
        }

        def tryToCreateRequest = {
          val parameters = Map.empty[String, Seq[String]] ++ nettyUri.getParameters.asScala.mapValues(_.asScala)
          createRequestHeader(parameters)
        }

        val (requestHeader, handlerLookup: Either[Future[SimpleResult], (Handler, Application)]) = Exception
          .allCatch[RequestHeader].either(tryToCreateRequest)
          .fold(
            e => {
              val rh = createRequestHeader()
              val r = server.applicationProvider.get.map(_.global).getOrElse(DefaultGlobal).onBadRequest(rh, e.getMessage)
              (rh, Left(r))
            },
            rh => server.getHandlerFor(rh) match {
              case directResult @ Left(_) => (rh, directResult)
              case Right((taggedRequestHeader, handler, application)) => (taggedRequestHeader, Right((handler, application)))
            }
          )

        // Call onRequestCompletion after all request processing is done. Protected with an AtomicBoolean to ensure can't be executed more than once.
        val alreadyClean = new java.util.concurrent.atomic.AtomicBoolean(false)
        def cleanup() {
          if (!alreadyClean.getAndSet(true)) {
            play.api.Play.maybeApplication.foreach(_.global.onRequestCompletion(requestHeader))
          }
        }

        // attach the cleanup function to the channel context for after cleaning
        ctx.setAttachment(cleanup _)

        val backendRequest = NettyBackendRequest(ctx, e)

        import play.api.libs.iteratee.Execution.Implicits.trampoline

        val done = handlerLookup match {
          //execute normal action
          case Right((handler, app)) =>
            val context = new NettyHandlerExecutorContext(Some(app), handlerExecutors)
            context(requestHeader, backendRequest, handler).orElse {
              context.handleError(requestHeader, backendRequest,
                new RuntimeException("Unknown handler returned by router: " + handler))
            }
          case Left(resultFuture) =>
            Play.logger.trace("No handler, got direct result: " + e)
            new NettyHandlerExecutorContext(None, handlerExecutors).sendResult(requestHeader, backendRequest, resultFuture)
        }

        done.map(_.map { _ =>
          cleanup()
          ctx.setAttachment(null)
        })

      case unexpected => Play.logger.error("Oops, unexpected message received in NettyServer (please report this problem): " + unexpected)
    }
  }

  def getHeaders(nettyRequest: HttpRequest): Headers = {
    val pairs = nettyRequest.getHeaders.asScala.groupBy(_.getKey).mapValues(_.map(_.getValue))
    new Headers { val data = pairs.toSeq }
  }
}

private case class NettyHandlerExecutorContext(application: Option[Application],
  handlerExecutors: Seq[HandlerExecutor[NettyBackendRequest]])
    extends HandlerExecutorContext[NettyBackendRequest] {

  import play.api.libs.iteratee.Execution.Implicits.trampoline

  def sendResult(request: RequestHeader, backend: NettyBackendRequest, result: Future[SimpleResult]) =
    NettyEssentialActionExecutor(this, request, backend,
      EssentialAction(_ => Iteratee.flatten(result.map(Done(_)))))
}
