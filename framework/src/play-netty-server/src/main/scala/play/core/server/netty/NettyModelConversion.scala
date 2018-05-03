/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.netty

import java.net.{ InetAddress, InetSocketAddress, URI }
import java.security.cert.X509Certificate
import java.time.Instant
import javax.net.ssl.SSLPeerUnverifiedException

import akka.stream.Materializer
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.ByteString
import com.typesafe.netty.http.{ DefaultStreamedHttpResponse, StreamedHttpRequest }
import io.netty.buffer.{ ByteBuf, Unpooled }
import io.netty.channel.Channel
import io.netty.handler.codec.http._
import io.netty.handler.ssl.SslHandler
import io.netty.util.ReferenceCountUtil
import play.api.Logger
import play.api.http.HeaderNames._
import play.api.http.{ HttpChunk, HttpEntity, HttpErrorHandler }
import play.api.libs.typedmap.TypedMap
import play.api.mvc._
import play.api.mvc.request.{ RemoteConnection, RequestAttrKey, RequestTarget }
import play.core.server.common.{ ForwardedHeaderHandler, ServerResultUtils }

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{ Failure, Try }

private[server] class NettyModelConversion(
    resultUtils: ServerResultUtils,
    forwardedHeaderHandler: ForwardedHeaderHandler,
    serverHeader: Option[String]) {

  private val logger = Logger(classOf[NettyModelConversion])

  private def parsePathAndQuery(uri: String): (String, String) = {
    // https://tools.ietf.org/html/rfc3986#section-3.3
    val withoutHost = uri.dropWhile(_ != '/')
    // The path is terminated by the first question mark ("?")
    // or number sign ("#") character, or by the end of the URI.
    val queryEndPos = Some(withoutHost.indexOf('#')).filter(_ != -1).getOrElse(withoutHost.length)
    val pathEndPos = Some(withoutHost.indexOf('?')).filter(_ != -1).getOrElse(queryEndPos)
    val path = withoutHost.substring(0, pathEndPos)
    // https://tools.ietf.org/html/rfc3986#section-3.4
    // The query component is indicated by the first question
    // mark ("?") character and terminated by a number sign ("#") character
    // or by the end of the URI.
    val queryString = withoutHost.substring(pathEndPos, queryEndPos)
    (path, queryString)
  }

  /**
   * Convert a Netty request to a Play RequestHeader.
   *
   * Will return a failure if there's a protocol error or some other error in the header.
   */
  def convertRequest(
    channel: Channel,
    request: HttpRequest): Try[RequestHeader] = {

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

  /** Capture a request's connection info from its channel and headers. */
  private def createRemoteConnection(channel: Channel, headers: Headers): RemoteConnection = {
    val rawConnection = new RemoteConnection {
      override lazy val remoteAddress: InetAddress = channel.remoteAddress().asInstanceOf[InetSocketAddress].getAddress
      private val sslHandler = Option(channel.pipeline().get(classOf[SslHandler]))
      override def secure: Boolean = sslHandler.isDefined
      override lazy val clientCertificateChain: Option[Seq[X509Certificate]] = {
        try {
          sslHandler.map { handler =>
            handler.engine.getSession.getPeerCertificates.toSeq.collect { case x509: X509Certificate => x509 }
          }
        } catch {
          case e: SSLPeerUnverifiedException => None
        }
      }
    }
    forwardedHeaderHandler.forwardedConnection(rawConnection, headers)
  }

  /** Create request target information from a Netty request. */
  private def createRequestTarget(request: HttpRequest): RequestTarget = {
    val (unsafePath, parsedQueryString) = parsePathAndQuery(request.uri)
    // wrapping into URI to handle absoluteURI and path validation
    val parsedPath = Option(new URI(unsafePath).getRawPath).getOrElse {
      // if the URI has a invalid path, this will trigger a 400 error
      throw new IllegalStateException(s"Cannot parse path from URI: $unsafePath")
    }
    new RequestTarget {
      override lazy val uri: URI = new URI(uriString)
      override def uriString: String = request.uri
      override val path: String = parsedPath
      override val queryString: String = parsedQueryString.stripPrefix("?")
      override lazy val queryMap: Map[String, Seq[String]] = {
        val decoder = new QueryStringDecoder(parsedQueryString)
        try {
          decoder.parameters().asScala.mapValues(_.asScala.toList).toMap
        } catch {
          case NonFatal(e) =>
            logger.warn("Failed to parse query string; returning empty map.", e)
            Map.empty
        }
      }
    }
  }

  /**
   * Create request target information from a Netty request where
   * there was a parsing failure.
   */
  def createUnparsedRequestTarget(request: HttpRequest): RequestTarget = new RequestTarget {
    override lazy val uri: URI = new URI(uriString)
    override def uriString: String = request.uri
    override lazy val path: String = {
      // The URI may be invalid, so instead, do a crude heuristic to drop the host and query string from it to get the
      // path, and don't decode.
      // RICH: This looks like a source of potential security bugs to me!
      val withoutHost = uriString.dropWhile(_ != '/')
      val withoutQueryString = withoutHost.split('?').head
      if (withoutQueryString.isEmpty) "/" else withoutQueryString
    }
    override lazy val queryMap: Map[String, Seq[String]] = {
      // Very rough parse of query string that doesn't decode
      if (request.uri.contains("?")) {
        request.uri.split("\\?", 2)(1).split('&').map { keyPair =>
          keyPair.split("=", 2) match {
            case Array(key) => key -> ""
            case Array(key, value) => key -> value
          }
        }.groupBy(_._1).map {
          case (name, values) => name -> values.map(_._2).toSeq
        }
      } else {
        Map.empty
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
    val headers = new NettyHeadersWrapper(request.headers)
    new RequestHeaderImpl(
      createRemoteConnection(channel, headers),
      request.method.name(),
      target,
      request.protocolVersion.text(),
      headers,
      // Send an attribute so our tests can tell which kind of server we're using.
      // We only do this for the "non-default" engine, so we used to tag
      // akka-http explicitly, so that benchmarking isn't affected by this.
      TypedMap(RequestAttrKey.Server -> "netty")
    )
  }

  /** Create the source for the request body */
  def convertRequestBody(request: HttpRequest)(implicit mat: Materializer): Option[Source[ByteString, Any]] = {
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
    errorHandler: HttpErrorHandler)(implicit mat: Materializer): Future[HttpResponse] = {

    resultUtils.resultConversionWithErrorHandling(requestHeader, result, errorHandler) { result =>

      val responseStatus = result.header.reasonPhrase match {
        case Some(phrase) => new HttpResponseStatus(result.header.status, phrase)
        case None => HttpResponseStatus.valueOf(result.header.status)
      }

      val connectionHeader = resultUtils.determineConnectionHeader(requestHeader, result)
      val skipEntity = requestHeader.method == HttpMethod.HEAD.name()

      val response: HttpResponse = result.body match {

        case any if skipEntity =>
          resultUtils.cancelEntity(any)
          new DefaultFullHttpResponse(httpVersion, responseStatus, Unpooled.EMPTY_BUFFER)

        case HttpEntity.Strict(data, _) =>
          new DefaultFullHttpResponse(httpVersion, responseStatus, byteStringToByteBuf(data))

        case HttpEntity.Streamed(stream, _, _) =>
          createStreamedResponse(stream, httpVersion, responseStatus)

        case HttpEntity.Chunked(chunks, _) =>
          createChunkedResponse(chunks, httpVersion, responseStatus)
      }

      // Set response headers
      val headers = resultUtils.splitSetCookieHeaders(result.header.headers)

      headers foreach {
        case (name, value) => response.headers().add(name, value)
      }

      // Content type and length
      if (resultUtils.mayHaveEntity(result.header.status)) {
        result.body.contentLength.foreach { contentLength =>
          if (HttpUtil.isContentLengthSet(response)) {
            val manualContentLength = response.headers.get(CONTENT_LENGTH)
            if (manualContentLength == contentLength.toString) {
              logger.info(s"Manual Content-Length header, ignoring manual header.")
            } else {
              logger.warn(s"Content-Length header was set manually in the header ($manualContentLength) but is not the same as actual content length ($contentLength).")
            }
          }
          HttpUtil.setContentLength(response, contentLength)
        }
      } else if (HttpUtil.isContentLengthSet(response)) {
        val manualContentLength = response.headers.get(CONTENT_LENGTH)
        logger.warn(s"Ignoring manual Content-Length ($manualContentLength) since it is not allowed for ${result.header.status} responses.")
        response.headers.remove(CONTENT_LENGTH)
      }
      result.body.contentType.foreach { contentType =>
        if (response.headers().contains(CONTENT_TYPE)) {
          logger.warn(s"Content-Type set both in header (${response.headers().get(CONTENT_TYPE)}) and attached to entity ($contentType), ignoring content type from entity. To remove this warning, use Result.as(...) to set the content type, rather than setting the header manually.")
        } else {
          response.headers().add(CONTENT_TYPE, contentType)
        }
      }

      connectionHeader.header.foreach { headerValue =>
        response.headers().set(CONNECTION, headerValue)
      }

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
      val response = new DefaultFullHttpResponse(httpVersion, HttpResponseStatus.INTERNAL_SERVER_ERROR, Unpooled.EMPTY_BUFFER)
      HttpUtil.setContentLength(response, 0)
      response.headers().add(DATE, dateHeader)
      serverHeader.foreach(response.headers().add(SERVER, _))
      response.headers().add(CONNECTION, "close")
      response
    }
  }

  /** Create a Netty streamed response. */
  private def createStreamedResponse(stream: Source[ByteString, _], httpVersion: HttpVersion,
    responseStatus: HttpResponseStatus)(implicit mat: Materializer) = {
    val publisher = SynchronousMappedStreams.map(stream.runWith(Sink.asPublisher(false)), byteStringToHttpContent)
    new DefaultStreamedHttpResponse(httpVersion, responseStatus, publisher)
  }

  /** Create a Netty chunked response. */
  private def createChunkedResponse(chunks: Source[HttpChunk, _], httpVersion: HttpVersion,
    responseStatus: HttpResponseStatus)(implicit mat: Materializer) = {

    val publisher = chunks.runWith(Sink.asPublisher(false))

    val httpContentPublisher = SynchronousMappedStreams.map[HttpChunk, HttpContent](publisher, {
      case HttpChunk.Chunk(bytes) =>
        new DefaultHttpContent(byteStringToByteBuf(bytes))
      case HttpChunk.LastChunk(trailers) =>
        val lastChunk = new DefaultLastHttpContent()
        trailers.headers.foreach {
          case (name, value) =>
            lastChunk.trailingHeaders().add(name, value)
        }
        lastChunk
    })

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
  private def dateHeader: String = {
    val currentTimeMillis = System.currentTimeMillis()
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
