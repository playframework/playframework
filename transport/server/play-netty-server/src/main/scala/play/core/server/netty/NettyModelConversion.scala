/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.netty

import java.net.InetSocketAddress
import java.net.URI
import java.security.cert.X509Certificate
import java.time.Instant
import javax.net.ssl.SSLPeerUnverifiedException

import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal
import scala.util.Failure
import scala.util.Try

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.handler.codec.http._
import io.netty.handler.ssl.SslHandler
import io.netty.util.ReferenceCountUtil
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString
import org.playframework.netty.http.DefaultStreamedHttpResponse
import org.playframework.netty.http.StreamedHttpRequest
import play.api.http.HeaderNames._
import play.api.http.HttpChunk
import play.api.http.HttpEntity
import play.api.http.HttpErrorHandler
import play.api.libs.typedmap.TypedMap
import play.api.mvc._
import play.api.mvc.request.PeerEndpoint
import play.api.mvc.request.RemoteInfo
import play.api.mvc.request.RequestAttrKey
import play.api.mvc.request.RequestTarget
import play.api.mvc.request.TransportConnection
import play.api.mvc.request.TransportTls
import play.api.Logger
import play.core.server.common.ClientCertificateHeaderHandler
import play.core.server.common.ForwardedHeaderHandler
import play.core.server.common.PathAndQueryParser
import play.core.server.common.ServerResultUtils
import play.core.system.RequestIdProvider

private[server] class NettyModelConversion(
    resultUtils: ServerResultUtils,
    forwardedHeaderHandler: ForwardedHeaderHandler,
    clientCertificateHeaderHandler: ClientCertificateHeaderHandler,
    serverHeader: Option[String]
) {
  private val logger = Logger(classOf[NettyModelConversion])

  /**
   * Convert a Netty request to a Play RequestHeader.
   *
   * Will return a failure if there's a protocol error or some other error in the header.
   */
  def convertRequest(channel: Channel, request: HttpRequest): Try[RequestHeader] = {
    if (request.decoderResult.isFailure) {
      Failure(request.decoderResult.cause())
    } else {
      tryToCreateRequest(channel, request)
    }
  }

  /** Try to create the request. May fail if the path is invalid */
  private def tryToCreateRequest(channel: Channel, request: HttpRequest): Try[RequestHeader] = {
    Try {
      val target: RequestTarget = createRequestTarget(request)
      createRequestHeader(channel, request, target)
    }
  }

  /** Capture immutable direct transport metadata from a request's channel. */
  private def createTransport(channel: Channel): TransportConnection = {
    val socketAddress = channel.remoteAddress().asInstanceOf[InetSocketAddress]
    val tls           = Option(channel.pipeline().get(classOf[SslHandler])).map { handler =>
      val peerCertificates = try {
        handler.engine.getSession.getPeerCertificates.toSeq.collect { case x509: X509Certificate => x509 }
      } catch {
        case _: SSLPeerUnverifiedException => Seq.empty
      }
      TransportTls(peerCertificates)
    }
    TransportConnection(PeerEndpoint(socketAddress.getAddress, Some(socketAddress.getPort)), tls)
  }

  /** Create request target information from a Netty request. */
  private def createRequestTarget(request: HttpRequest): RequestTarget = {
    val (parsedPath, parsedQueryString) = PathAndQueryParser.parse(request.uri)

    new RequestTarget {
      override lazy val uri: URI                      = new URI(uriString)
      override def uriString: String                  = request.uri
      override val path: String                       = parsedPath
      override val queryString: String                = parsedQueryString.stripPrefix("?")
      override val queryMap: Map[String, Seq[String]] = {
        val decoder = new QueryStringDecoder(parsedQueryString)
        try {
          decoder.parameters().asScala.view.mapValues(_.asScala.toList).toMap
        } catch {
          case iae: IllegalArgumentException if Option(iae.getMessage).exists(_.startsWith("invalid hex byte")) =>
            throw iae
          case NonFatal(e) =>
            logger.warn("Failed to parse query string; returning empty map.", e)
            Map.empty
        }
      }
    }
  }

  /**
   * Create the request header. This header is not created with the application's
   * RequestFactory, simply because we don't yet have an application at this phase
   * of request processing. We'll pass it through the application's RequestFactory
   * later.
   */
  def createRequestHeader(channel: Channel, request: HttpRequest, target: RequestTarget): RequestHeader = {
    val transport     = createTransport(channel)
    val rawRemote     = RemoteInfo.fromPeer(transport.peer)
    val rawHeaders    = new NettyHeadersWrapper(request.headers)
    val directScheme  = RequestHeader.initialScheme(transport)
    val initialTarget = RequestHeader
      .initialRequestTarget(request.method.name(), target, request.protocolVersion.text(), rawHeaders)
      .fold(error => throw new IllegalArgumentException(error), identity)
    val forwarding = forwardedHeaderHandler.forwardedRequest(
      rawRemote,
      rawHeaders,
      directScheme,
      initialTarget.authority
    )
    val effectiveScheme = RequestHeader
      .effectiveScheme(initialTarget.scheme, directScheme, forwarding.scheme)
      .fold(error => throw new IllegalArgumentException(error), identity)
    val normalizedTarget   = RequestHeader.normalizeRequestTargetPath(target, initialTarget)
    val clientCertificates = clientCertificateHeaderHandler.clientCertificates(transport, rawHeaders)
    val attrs              = TypedMap(
      // Send an attribute so our tests can tell which kind of server we're using.
      // We only do this for the "non-default" engine, so we used to tag
      // pekko-http explicitly, so that benchmarking isn't affected by this.
      RequestAttrKey.Server -> "netty",
      // This is the earliest stage of a Play request at which we can set an id.
      RequestAttrKey.Id -> RequestIdProvider.freshId(),
    )
    new RequestHeaderImpl(
      forwarding.remote,
      request.method.name(),
      normalizedTarget,
      request.protocolVersion.text(),
      rawHeaders,
      attrs,
      transport,
      clientCertificates.clientCertificate,
      effectiveScheme,
      forwarding.authority,
      clientCertificates.xForwardedClientCertificates
    )
  }

  /**
   * Build a nonthrowing header for reporting a request-conversion error.
   *
   * Normal request conversion might have failed before all target metadata was available. This
   * recovery path therefore derives target metadata best-effort and still applies independently
   * valid trusted forwarding metadata. Forwarding validation is fail-closed; an unexpected failure
   * falls back to the directly observed request metadata so construction for HttpErrorHandler cannot
   * repeat the original failure.
   */
  def createErrorRequestHeader(
      channel: Channel,
      request: HttpRequest,
      target: RequestTarget,
      requestFailure: Throwable
  ): RequestHeader = {
    val transport          = createTransport(channel)
    val rawRemote          = RemoteInfo.fromPeer(transport.peer)
    val rawHeaders         = new NettyHeadersWrapper(request.headers)
    val clientCertificates =
      clientCertificateHeaderHandler.clientCertificatesForErrorRequest(transport, rawHeaders, requestFailure)
    val directScheme  = RequestHeader.initialScheme(transport)
    val initialTarget = RequestHeader
      .initialRequestTarget(request.method.name(), target, request.protocolVersion.text(), rawHeaders)
      .toOption
    val initialAuthority = initialTarget.flatMap(_.authority)
    val forwarding       = try {
      forwardedHeaderHandler.forwardedRequest(rawRemote, rawHeaders, directScheme, initialAuthority)
    } catch {
      case NonFatal(error) =>
        logger.warn("Failed to apply forwarded metadata to an error request; using direct metadata.", error)
        ForwardedHeaderHandler.ParsedForwarding(rawRemote, directScheme, initialAuthority)
    }
    val scheme = RequestHeader
      .effectiveScheme(initialTarget.flatMap(_.scheme), directScheme, forwarding.scheme)
      .getOrElse(forwarding.scheme)
    val attrs = TypedMap(
      RequestAttrKey.Server -> "netty",
      RequestAttrKey.Id     -> RequestIdProvider.freshId(),
    )

    new RequestHeaderImpl(
      forwarding.remote,
      request.method.name(),
      target,
      request.protocolVersion.text(),
      rawHeaders,
      attrs,
      transport,
      clientCertificates.clientCertificate,
      scheme,
      forwarding.authority,
      clientCertificates.xForwardedClientCertificates
    )
  }

  /** Create the source for the request body */
  def convertRequestBody(request: HttpRequest): Option[Source[ByteString, Any]] = {
    request match {
      case full: FullHttpRequest =>
        val content = httpContentToByteString(full)
        if (content.isEmpty) {
          None
        } else {
          Some(Source.single(content))
        }
      case streamed: StreamedHttpRequest =>
        Some(Source.fromPublisher(SynchronousMappedStreams.map(streamed, httpContentToByteString)))
    }
  }

  /** Convert an HttpContent object to a ByteString */
  private def httpContentToByteString(content: HttpContent): ByteString = {
    val builder = ByteString.newBuilder
    content.content().readBytes(builder.asOutputStream, content.content().readableBytes())
    val bytes = builder.result()
    ReferenceCountUtil.release(content)
    bytes
  }

  /** Create a Netty response from the result */
  def convertResult(
      result: Result,
      requestHeader: RequestHeader,
      httpVersion: HttpVersion,
      errorHandler: HttpErrorHandler
  )(implicit mat: Materializer): Future[HttpResponse] = {
    resultUtils.resultConversionWithErrorHandling(requestHeader, result, errorHandler) { result =>
      val responseStatus = result.header.reasonPhrase match {
        case Some(phrase) => new HttpResponseStatus(result.header.status, phrase)
        case None         => HttpResponseStatus.valueOf(result.header.status)
      }

      val connectionHeader = resultUtils.determineConnectionHeader(requestHeader, result)
      val skipEntity       = requestHeader.method == HttpMethod.HEAD.name()

      val response: HttpResponse = result.body match {
        case any if skipEntity =>
          resultUtils.cancelEntity(any)
          new HeadHttpResponse(httpVersion, responseStatus)

        case HttpEntity.Strict(data, _) =>
          new DefaultFullHttpResponse(httpVersion, responseStatus, byteStringToByteBuf(data))

        case HttpEntity.Streamed(stream, _, _) =>
          createStreamedResponse(stream, httpVersion, responseStatus)

        case HttpEntity.Chunked(chunks, _) =>
          createChunkedResponse(chunks, httpVersion, responseStatus)
      }

      // Set response headers
      val headers = resultUtils.splitSetCookieHeaders(result.header.headers)

      headers.foreach {
        case (name, value) => response.headers().add(name, value)
      }

      // Content type and length
      if (skipEntity) {
        if (HttpUtil.isContentLengthSet(response)) {
          val manualContentLength = response.headers.get(CONTENT_LENGTH)
          logger.warn(
            s"Ignoring manual Content-Length ($manualContentLength) since it is not rendered for HEAD responses."
          )
          response.headers.remove(CONTENT_LENGTH)
        }
        // HttpStreamsServerHandler is not HEAD-aware. Without Content-Length or Transfer-Encoding it assumes a 200
        // response can only be delimited by closing the connection. Mark it as chunked inside the Netty pipeline so
        // keep-alive is preserved; PlayHttpResponseEncoder strips the marker before bytes are written.
        HttpUtil.setTransferEncodingChunked(response, true)
      } else if (resultUtils.mayHaveEntity(result.header.status)) {
        result.body.contentLength.foreach { contentLength =>
          if (HttpUtil.isContentLengthSet(response)) {
            val manualContentLength = response.headers.get(CONTENT_LENGTH)
            if (manualContentLength == contentLength.toString) {
              logger.info(s"Manual Content-Length header, ignoring manual header.")
            } else {
              logger.warn(
                s"Content-Length header was set manually in the header ($manualContentLength) but is not the same as actual content length ($contentLength)."
              )
            }
          }
          HttpUtil.setContentLength(response, contentLength)
        }
      } else if (HttpUtil.isContentLengthSet(response)) {
        val manualContentLength = response.headers.get(CONTENT_LENGTH)
        logger.warn(
          s"Ignoring manual Content-Length ($manualContentLength) since it is not allowed for ${result.header.status} responses."
        )
        response.headers.remove(CONTENT_LENGTH)
      }
      result.body.contentType.foreach { contentType =>
        if (response.headers().contains(CONTENT_TYPE)) {
          logger.warn(
            s"Content-Type set both in header (${response.headers().get(CONTENT_TYPE)}) and attached to entity ($contentType), ignoring content type from entity. To remove this warning, use Result.as(...) to set the content type, rather than setting the header manually."
          )
        } else {
          response.headers().add(CONTENT_TYPE, contentType)
        }
      }

      connectionHeader.header.foreach { headerValue => response.headers().set(CONNECTION, headerValue) }

      // Netty doesn't add the required Date header for us, so make sure there is one here
      if (!response.headers().contains(DATE)) {
        response.headers().add(DATE, dateHeader)
      }

      if (!response.headers().contains(SERVER)) {
        serverHeader.foreach(response.headers().add(SERVER, _))
      }

      Future.successful(response)
    } {
      // Fallback response
      val response =
        new DefaultFullHttpResponse(httpVersion, HttpResponseStatus.INTERNAL_SERVER_ERROR, Unpooled.EMPTY_BUFFER)
      HttpUtil.setContentLength(response, 0)
      response.headers().add(DATE, dateHeader)
      serverHeader.foreach(response.headers().add(SERVER, _))
      response.headers().add(CONNECTION, "close")
      response
    }
  }

  /** Create a Netty streamed response. */
  private def createStreamedResponse(
      stream: Source[ByteString, ?],
      httpVersion: HttpVersion,
      responseStatus: HttpResponseStatus
  )(implicit mat: Materializer) = {
    val publisher = SynchronousMappedStreams.map(stream.runWith(Sink.asPublisher(false)), byteStringToHttpContent)
    new DefaultStreamedHttpResponse(httpVersion, responseStatus, publisher)
  }

  /** Create a Netty chunked response. */
  private def createChunkedResponse(
      chunks: Source[HttpChunk, ?],
      httpVersion: HttpVersion,
      responseStatus: HttpResponseStatus
  )(implicit mat: Materializer) = {
    val publisher = chunks.runWith(Sink.asPublisher(false))

    val httpContentPublisher = SynchronousMappedStreams.map[HttpChunk, HttpContent](
      publisher,
      {
        case HttpChunk.Chunk(bytes) =>
          new DefaultHttpContent(byteStringToByteBuf(bytes))
        case HttpChunk.LastChunk(trailers) =>
          val lastChunk = new DefaultLastHttpContent()
          trailers.headers.foreach {
            case (name, value) =>
              lastChunk.trailingHeaders().add(name, value)
          }
          lastChunk
      }
    )

    val response = new DefaultStreamedHttpResponse(httpVersion, responseStatus, httpContentPublisher)
    HttpUtil.setTransferEncodingChunked(response, true)
    response
  }

  /** Convert a ByteString to a Netty ByteBuf. */
  private def byteStringToByteBuf(bytes: ByteString): ByteBuf = {
    if (bytes.isEmpty) {
      Unpooled.EMPTY_BUFFER
    } else {
      Unpooled.wrappedBuffer(bytes.asByteBuffer)
    }
  }

  private def byteStringToHttpContent(bytes: ByteString): HttpContent = {
    new DefaultHttpContent(byteStringToByteBuf(bytes))
  }

  // cache the date header of the last response so we only need to compute it every second
  private var cachedDateHeader: (Long, String) = (Long.MinValue, null)
  private def dateHeader: String               = {
    val currentTimeMillis  = System.currentTimeMillis()
    val currentTimeSeconds = currentTimeMillis / 1000
    cachedDateHeader match {
      case (cachedSeconds, dateHeaderString) if cachedSeconds == currentTimeSeconds =>
        dateHeaderString
      case _ =>
        val dateHeaderString = ResponseHeader.httpDateFormat.format(Instant.ofEpochMilli(currentTimeMillis))
        cachedDateHeader = currentTimeSeconds -> dateHeaderString
        dateHeaderString
    }
  }
}
