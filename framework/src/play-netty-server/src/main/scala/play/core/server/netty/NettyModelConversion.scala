/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.netty

import java.net.{ URI, InetSocketAddress }
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLPeerUnverifiedException

import akka.stream.Materializer
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.ByteString
import com.typesafe.netty.http.{ DefaultStreamedHttpResponse, StreamedHttpRequest }
import io.netty.buffer.{ ByteBuf, Unpooled }
import io.netty.handler.codec.http._
import io.netty.handler.ssl.SslHandler
import io.netty.util.ReferenceCountUtil
import play.api.Logger
import play.api.http.HeaderNames._
import play.api.http.{ Status, HttpChunk, HttpEntity }
import play.api.mvc._
import play.core.server.common.{ ConnectionInfo, ServerResultUtils, ForwardedHeaderHandler }

import scala.collection.JavaConverters._
import scala.util.{ Failure, Try }
import scala.util.control.NonFatal

private[server] class NettyModelConversion(forwardedHeaderHandler: ForwardedHeaderHandler) {

  private val logger = Logger(classOf[NettyModelConversion])

  /**
   * Convert a Netty request to a Play RequestHeader.
   *
   * Will return a failure if there's a protocol error or some other error in the header.
   */
  def convertRequest(requestId: Long,
    remoteAddress: InetSocketAddress,
    sslHandler: Option[SslHandler],
    request: HttpRequest): Try[RequestHeader] = {

    if (request.getDecoderResult.isFailure) {
      Failure(request.getDecoderResult.cause())
    } else {
      tryToCreateRequest(request, requestId, remoteAddress, sslHandler)
    }
  }

  /** Try to create the request. May fail if the path is invalid */
  private def tryToCreateRequest(request: HttpRequest, requestId: Long, remoteAddress: InetSocketAddress, sslHandler: Option[SslHandler]): Try[RequestHeader] = {

    Try {
      val uri = new QueryStringDecoder(request.getUri)
      val parameters: Map[String, Seq[String]] = {
        val decodedParameters = uri.parameters()
        if (decodedParameters.isEmpty) Map.empty else {
          decodedParameters.asScala.mapValues(_.asScala).toMap
        }
      }
      // wrapping into URI to handle absoluteURI
      val path = new URI(uri.path()).getRawPath
      createRequestHeader(request, requestId, path, parameters, remoteAddress, sslHandler)
    }
  }

  /** Create the request header */
  private def createRequestHeader(request: HttpRequest, requestId: Long, parsedPath: String,
    parameters: Map[String, Seq[String]], _remoteAddress: InetSocketAddress,
    sslHandler: Option[SslHandler]): RequestHeader = {

    new RequestHeader {
      override val id = requestId
      override val tags = Map.empty[String, String]
      override def uri = request.getUri
      override def path = parsedPath
      override def method = request.getMethod.name()
      override def version = request.getProtocolVersion.text()
      override def queryString = parameters
      override val headers = new NettyHeadersWrapper(request.headers)
      private lazy val remoteConnection: ConnectionInfo = {
        forwardedHeaderHandler.remoteConnection(_remoteAddress.getAddress, sslHandler.isDefined, headers)
      }
      override def remoteAddress = remoteConnection.address.getHostAddress
      override def secure = remoteConnection.secure
      override lazy val clientCertificateChain = clientCertificatesFromSslEngine(sslHandler.map(_.engine()))
    }
  }

  /** Create an unparsed request header. Used when even Netty couldn't parse the request. */
  def createUnparsedRequestHeader(requestId: Long, request: HttpRequest, _remoteAddress: InetSocketAddress, sslHandler: Option[SslHandler]) = {

    new RequestHeader {
      override def id = requestId
      override def tags = Map.empty[String, String]
      override def uri = request.getUri
      override lazy val path = {
        // The URI may be invalid, so instead, do a crude heuristic to drop the host and query string from it to get the
        // path, and don't decode.
        val withoutHost = request.getUri.dropWhile(_ != '/')
        val withoutQueryString = withoutHost.split('?').head
        if (withoutQueryString.isEmpty) "/" else withoutQueryString
      }
      override def method = request.getMethod.name()
      override def version = request.getProtocolVersion.text()
      override lazy val queryString: Map[String, Seq[String]] = {
        // Very rough parse of query string that doesn't decode
        if (request.getUri.contains("?")) {
          request.getUri.split("\\?", 2)(1).split('&').map { keyPair =>
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
      override val headers = new NettyHeadersWrapper(request.headers)
      override def remoteAddress = _remoteAddress.getAddress.toString
      override def secure = sslHandler.isDefined
      override lazy val clientCertificateChain = clientCertificatesFromSslEngine(sslHandler.map(_.engine()))
    }
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
  def convertResult(result: Result, requestHeader: RequestHeader, httpVersion: HttpVersion)(implicit mat: Materializer): HttpResponse = {

    val responseStatus = result.header.reasonPhrase match {
      case Some(phrase) => new HttpResponseStatus(result.header.status, phrase)
      case None => HttpResponseStatus.valueOf(result.header.status)
    }

    val connectionHeader = ServerResultUtils.determineConnectionHeader(requestHeader, result)
    val skipEntity = requestHeader.method == HttpMethod.HEAD.name()

    val response: HttpResponse = result.body match {

      case any if skipEntity =>
        ServerResultUtils.cancelEntity(any)
        new DefaultFullHttpResponse(httpVersion, responseStatus, Unpooled.EMPTY_BUFFER)

      case HttpEntity.Strict(data, _) =>
        new DefaultFullHttpResponse(httpVersion, responseStatus, byteStringToByteBuf(data))

      case HttpEntity.Streamed(stream, _, _) =>
        createStreamedResponse(stream, httpVersion, responseStatus)

      case HttpEntity.Chunked(chunks, _) =>
        createChunkedResponse(chunks, httpVersion, responseStatus)
    }

    // Set response headers
    val headers = ServerResultUtils.splitSetCookieHeaders(result.header.headers)

    try {
      headers foreach {
        case (name, value) => response.headers().add(name, value)
      }

      // Content type and length
      if (mayHaveContentLength(result.header.status)) {
        result.body.contentLength.foreach { contentLength =>
          if (HttpHeaders.isContentLengthSet(response)) {
            val manualContentLength = response.headers.get(CONTENT_LENGTH)
            if (manualContentLength == contentLength.toString) {
              logger.info(s"Manual Content-Length header, ignoring manual header.")
            } else {
              logger.warn(s"Content-Length header was set manually in the header ($manualContentLength) but is not the same as actual content length ($contentLength).")
            }
          }
          HttpHeaders.setContentLength(response, contentLength)
        }
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

      response
    } catch {
      case NonFatal(e) =>
        if (logger.isErrorEnabled) {
          val prettyHeaders = headers.map { case (name, value) => s"$name -> $value" }.mkString("[", ",", "]")
          val msg = s"Exception occurred while setting response's headers to $prettyHeaders. Action taken is to set the response's status to ${HttpResponseStatus.INTERNAL_SERVER_ERROR} and discard all headers."
          logger.error(msg, e)
        }
        val response = new DefaultFullHttpResponse(httpVersion, HttpResponseStatus.INTERNAL_SERVER_ERROR, Unpooled.EMPTY_BUFFER)
        HttpHeaders.setContentLength(response, 0)
        response.headers().add(DATE, dateHeader)
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
    HttpHeaders.setTransferEncodingChunked(response)
    response
  }

  /** Whether the given status may have a content length header or not. */
  private def mayHaveContentLength(status: Int) =
    status != Status.NO_CONTENT && status != Status.NOT_MODIFIED

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

  private def clientCertificatesFromSslEngine(sslEngine: Option[SSLEngine]): Option[Seq[X509Certificate]] = {
    try {
      sslEngine.map { engine =>
        engine.getSession.getPeerCertificates.toSeq.collect { case x509: X509Certificate => x509 }
      }
    } catch {
      case e: SSLPeerUnverifiedException => None
    }
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
        val dateHeaderString = ResponseHeader.httpDateFormat.print(currentTimeMillis)
        cachedDateHeader = currentTimeSeconds -> dateHeaderString
        dateHeaderString
    }
  }
}
