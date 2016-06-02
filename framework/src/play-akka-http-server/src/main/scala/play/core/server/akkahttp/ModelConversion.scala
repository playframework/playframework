/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.akkahttp

import java.net.InetSocketAddress

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Logger
import play.api.http.HeaderNames._
import play.api.http.{ HttpChunk, HttpEntity => PlayHttpEntity }
import play.api.mvc._
import play.core.server.common.{ ConnectionInfo, ForwardedHeaderHandler, ServerResultUtils }

import scala.collection.immutable

/**
 * Conversions between Akka's and Play's HTTP model objects.
 */
private[akkahttp] class ModelConversion(forwardedHeaderHandler: ForwardedHeaderHandler) {

  private val logger = Logger(getClass)

  /**
   * Convert an Akka `HttpRequest` to a `RequestHeader` and an `Enumerator`
   * for its body.
   */
  def convertRequest(
    requestId: Long,
    remoteAddress: InetSocketAddress,
    secureProtocol: Boolean,
    request: HttpRequest)(implicit fm: Materializer): (RequestHeader, Option[Source[ByteString, Any]]) = {
    (
      convertRequestHeader(requestId, remoteAddress, secureProtocol, request),
      convertRequestBody(request)
    )
  }

  /**
   * Convert an Akka `HttpRequest` to a `RequestHeader`.
   */
  private def convertRequestHeader(
    requestId: Long,
    remoteAddress: InetSocketAddress,
    secureProtocol: Boolean,
    request: HttpRequest): RequestHeader = {
    val remoteHostAddress = remoteAddress.getAddress.getHostAddress
    // Taken from PlayDefaultUpstreamHander

    // Avoid clash between method arg and RequestHeader field
    val remoteAddressArg = remoteAddress

    new RequestHeader {
      override val id = requestId
      // Send a tag so our tests can tell which kind of server we're using.
      // We could get NettyServer to send a similar tag, but for the moment
      // let's not, just in case it slows NettyServer down a bit.
      override val tags = Map("HTTP_SERVER" -> "akka-http")
      // Note: Akka HTTP doesn't provide a direct way to get the raw URI
      // This will only work properly if
      override def uri = request.header[`Raw-Request-URI`].map(_.value) getOrElse {
        logger.warn("Can't get raw request URI. Please set akka.http.server.raw-request-uri-header = true")
        request.uri.toString
      }
      override def path = request.uri.path.toString
      override def method = request.method.name
      override def version = request.protocol.value
      override def queryString = request.uri.query().toMultiMap
      override val headers = convertRequestHeaders(request)
      private lazy val remoteConnection: ConnectionInfo = {
        forwardedHeaderHandler.remoteConnection(remoteAddressArg.getAddress, secureProtocol, headers)
      }
      override def remoteAddress = remoteConnection.address.getHostAddress
      override def secure = remoteConnection.secure
      override def clientCertificateChain = None // TODO - Akka does not yet expose the SSLEngine used for the request
    }
  }

  /**
   * Convert the request headers of an Akka `HttpRequest` to a Play `Headers` object.
   */
  private def convertRequestHeaders(request: HttpRequest): Headers = {
    val entityHeaders: Seq[(String, String)] = request.entity match {
      case HttpEntity.Strict(contentType, data) =>
        Seq(CONTENT_TYPE -> contentType.value, CONTENT_LENGTH -> data.length.toString)
      case HttpEntity.Default(contentType, contentLength, _) =>
        Seq(CONTENT_TYPE -> contentType.value, CONTENT_LENGTH -> contentLength.toString)
      case HttpEntity.Chunked(contentType, _) =>
        Seq(CONTENT_TYPE -> contentType.value, TRANSFER_ENCODING -> play.api.http.HttpProtocol.CHUNKED)
    }
    val normalHeaders: Seq[(String, String)] = request.headers
      .filter(_.isNot(`Raw-Request-URI`.lowercaseName))
      .map(rh => rh.name -> rh.value)
    new Headers(entityHeaders ++ normalHeaders)
  }

  /**
   * Convert an Akka `HttpRequest` to an `Enumerator` of the request body.
   */
  private def convertRequestBody(
    request: HttpRequest)(implicit fm: Materializer): Option[Source[ByteString, Any]] = {
    request.entity match {
      case HttpEntity.Strict(_, data) if data.isEmpty =>
        None
      case HttpEntity.Strict(_, data) =>
        Some(Source.single(data))
      case HttpEntity.Default(_, 0, _) =>
        None
      case HttpEntity.Default(contentType, contentLength, pubr) =>
        // FIXME: should do something with the content-length?
        Some(pubr)
      case HttpEntity.Chunked(contentType, chunks) =>
        // FIXME: do something with trailing headers?
        Some(chunks.takeWhile(!_.isLastChunk).map(_.data()))
    }
  }

  /**
   * Convert a Play `Result` object into an Akka `HttpResponse` object.
   */
  def convertResult(
    requestHeaders: RequestHeader,
    unvalidated: Result,
    protocol: HttpProtocol)(implicit mat: Materializer): HttpResponse = {

    val result = ServerResultUtils.validateResult(requestHeaders, unvalidated)
    val convertedHeaders: AkkaHttpHeaders = convertResponseHeaders(result.header.headers)
    val entity = convertResultBody(requestHeaders, convertedHeaders, result, protocol)
    val connectionHeader = ServerResultUtils.determineConnectionHeader(requestHeaders, result)
    val closeHeader = connectionHeader.header.map(Connection(_))
    HttpResponse(
      status = result.header.status,
      headers = convertedHeaders.misc ++ closeHeader,
      entity = entity,
      protocol = protocol
    )
  }

  def convertResultBody(
    requestHeaders: RequestHeader,
    convertedHeaders: AkkaHttpHeaders,
    result: Result,
    protocol: HttpProtocol): ResponseEntity = {

    val contentType = result.body.contentType.fold(ContentTypes.NoContentType: ContentType) { ct =>
      HttpHeader.parse(CONTENT_TYPE, ct) match {
        case HttpHeader.ParsingResult.Ok(`Content-Type`(akkaCt), _) => akkaCt
        case _ => ContentTypes.NoContentType
      }

    }

    result.body match {
      case PlayHttpEntity.Strict(data, _) =>
        HttpEntity.Strict(contentType, data)

      case PlayHttpEntity.Streamed(data, Some(contentLength), _) =>
        HttpEntity.Default(contentType, contentLength, data)

      case PlayHttpEntity.Streamed(data, _, _) =>
        HttpEntity.CloseDelimited(contentType, data)

      case PlayHttpEntity.Chunked(data, _) =>
        val akkaChunks = data.map {
          case HttpChunk.Chunk(chunk) =>
            HttpEntity.Chunk(chunk)
          case HttpChunk.LastChunk(trailers) if trailers.headers.isEmpty =>
            HttpEntity.LastChunk
          case HttpChunk.LastChunk(trailers) =>
            HttpEntity.LastChunk(trailer = convertHeaders(trailers.headers))
        }
        HttpEntity.Chunked(contentType, akkaChunks)
    }
  }

  private def convertHeaders(headers: Iterable[(String, String)]): immutable.Seq[HttpHeader] = {
    headers.map {
      case (name, value) =>
        HttpHeader.parse(name, value) match {
          case HttpHeader.ParsingResult.Ok(header, errors /* errors are ignored if Ok */ ) =>
            header
          case HttpHeader.ParsingResult.Error(error) =>
            sys.error(s"Error parsing header: $error")
        }
    }.to[immutable.Seq]
  }

  /**
   * A representation of Akka HTTP headers separate from an `HTTPMessage`.
   * Akka HTTP treats some headers specially and these are split out into
   * separate values.
   *
   * @param misc General headers. Guaranteed not to contain any of the special
   * headers stored in the other values.
   */
  case class AkkaHttpHeaders(
    misc: immutable.Seq[HttpHeader],
    transferEncoding: Option[immutable.Seq[TransferEncoding]])

  /**
   * Convert Play response headers into `HttpHeader` objects, then separate
   * out any special headers.
   */
  private def convertResponseHeaders(
    playHeaders: Map[String, String]): AkkaHttpHeaders = {
    val rawHeaders: Iterable[(String, String)] = ServerResultUtils.splitSetCookieHeaders(playHeaders)
    val convertedHeaders: Seq[HttpHeader] = convertHeaders(rawHeaders)
    val emptyHeaders = AkkaHttpHeaders(immutable.Seq.empty, None)
    convertedHeaders.foldLeft(emptyHeaders) {
      case (accum, te: `Transfer-Encoding`) =>
        accum.copy(transferEncoding = Some(te.encodings))
      case (accum, miscHeader) =>
        accum.copy(misc = accum.misc :+ miscHeader)
    }
  }

}
