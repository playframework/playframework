/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.akkahttp

import java.net.{ InetAddress, InetSocketAddress, URI }
import java.util.Locale

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.util.FastFuture._
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Logger
import play.api.http.{ HttpChunk, HttpErrorHandler, Status, HttpEntity => PlayHttpEntity }
import play.api.libs.typedmap.TypedMap
import play.api.mvc._
import play.api.mvc.request.{ RemoteConnection, RequestTarget }
import play.core.server.common.{ ForwardedHeaderHandler, ServerResultUtils }
import play.mvc.Http.HeaderNames

import scala.collection.immutable
import scala.concurrent.Future

/**
 * Conversions between Akka's and Play's HTTP model objects.
 */
private[server] class AkkaModelConversion(
    resultUtils: ServerResultUtils,
    forwardedHeaderHandler: ForwardedHeaderHandler) {

  private val logger = Logger(getClass)

  /**
   * Convert an Akka `HttpRequest` to a `RequestHeader` and an `Enumerator`
   * for its body.
   */
  def convertRequest(requestId: Long, remoteAddress: InetSocketAddress, secureProtocol: Boolean, request: HttpRequest)(implicit fm: Materializer): (RequestHeader, Either[ByteString, Source[ByteString, Any]]) = {

    (
      convertRequestHeader(requestId, remoteAddress, secureProtocol, request),
      convertRequestBody(request)
    )

    //    // FIXME this is if you want to try out avoiding conversion 
    //    (
    //      new RequestHeaderImpl(
    //        forwardedHeaderHandler.forwardedConnection(
    //          new RemoteConnection {
    //            override def remoteAddress: InetAddress = InetAddress.getLocalHost
    //            override def secure: Boolean = secureProtocol
    //            // TODO - Akka does not yet expose the SSLEngine used for the request
    //            override lazy val clientCertificateChain = None
    //          },
    //          Headers()),
    //        request.method.name,
    //        new RequestTarget {
    //          override lazy val uri: URI = new URI(uriString)
    //          override lazy val uriString: String = request.header[`Raw-Request-URI`] match {
    //            case None =>
    //              logger.warn("Can't get raw request URI.")
    //              request.uri.toString
    //            case Some(rawUri) =>
    //              rawUri.uri
    //          }
    //          override lazy val path: String = request.uri.path.toString
    //          override lazy val queryMap: Map[String, Seq[String]] = request.uri.query().toMultiMap
    //        },
    //        request.protocol.value,
    //        Headers(),
    //        TypedMap.empty
    //      ),
    //      None
    //    )
  }

  /**
   * Convert an Akka `HttpRequest` to a `RequestHeader`.
   */
  private def convertRequestHeader(
    requestId: Long,
    remoteAddress: InetSocketAddress,
    secureProtocol: Boolean,
    request: HttpRequest): RequestHeader = {

    val headers = convertRequestHeadersAkka(request)
    val remoteAddressArg = remoteAddress // Avoid clash between method arg and RequestHeader field

    new RequestHeaderImpl(
      forwardedHeaderHandler.forwardedConnection(
        new RemoteConnection {
          override def remoteAddress: InetAddress = remoteAddressArg.getAddress
          override def secure: Boolean = secureProtocol
          // TODO - Akka does not yet expose the SSLEngine used for the request
          override lazy val clientCertificateChain = None
        },
        headers),
      request.method.name,
      new RequestTarget {
        override lazy val uri: URI = new URI(headers.uri)
        override def uriString: String = headers.uri
        override lazy val path: String = request.uri.path.toString
        override lazy val queryMap: Map[String, Seq[String]] = request.uri.query().toMultiMap
      },
      request.protocol.value,
      headers,
      TypedMap.empty
    )
  }

  /**
   * Convert the request headers of an Akka `HttpRequest` to a Play `Headers` object.
   */
  private def convertRequestHeadersAkka(request: HttpRequest): AkkaHeadersWrapper = {
    var knownContentLength: Option[String] = None
    var isChunked: Option[String] = None

    request.entity match {
      case HttpEntity.Strict(_, data) =>
        if (request.method.requestEntityAcceptance == RequestEntityAcceptance.Expected || data.nonEmpty) {
          knownContentLength = Some(data.length.toString)
        }
      case HttpEntity.Default(_, cLength, _) =>
        if (request.method.requestEntityAcceptance == RequestEntityAcceptance.Expected || cLength > 0) {
          knownContentLength = Some(cLength.toString)
        }
      case e: HttpEntity.Chunked =>
        isChunked = Some(TransferEncodings.chunked.value)
    }

    var requestUri: String = null
    request.headers.foreach {
      case `Raw-Request-URI`(u) => requestUri = u
      case e: `Transfer-Encoding` => isChunked = Some(e.value())
      case _ => // continue
    }
    if (requestUri eq null) requestUri = request.uri.toString() // fallback value

    new AkkaHeadersWrapper(request, knownContentLength, request.headers, isChunked, requestUri)
  }

  /**
   * Convert an Akka `HttpRequest` to an `Enumerator` of the request body.
   */
  private def convertRequestBody(
    request: HttpRequest)(implicit fm: Materializer): Either[ByteString, Source[ByteString, Any]] = {
    request.entity match {
      case HttpEntity.Strict(_, data) =>
        Left(data)

      case HttpEntity.Default(_, 0, _) =>
        Left(ByteString.empty)

      case HttpEntity.Default(contentType, contentLength, pubr) =>
        // FIXME: should do something with the content-length?
        Right(pubr)

      case HttpEntity.Chunked(contentType, chunks) =>
        // FIXME: do something with trailing headers?
        Right(chunks.takeWhile(!_.isLastChunk).map(_.data()))
    }
  }

  /**
   * Convert a Play `Result` object into an Akka `HttpResponse` object.
   */
  def convertResult(
    requestHeaders: RequestHeader,
    unvalidated: Result,
    protocol: HttpProtocol,
    errorHandler: HttpErrorHandler)(implicit mat: Materializer): Future[HttpResponse] = {

    import play.core.Execution.Implicits.trampoline

    resultUtils.resultConversionWithErrorHandling(requestHeaders, unvalidated, errorHandler) { unvalidated =>
      // Convert result

      resultUtils.validateResult(requestHeaders, unvalidated, errorHandler).fast.map { validated: Result =>
        val convertedHeaders = convertHeaders(validated.header.headers)
        val entity = convertResultBody(requestHeaders, validated, protocol)
        val response = HttpResponse(
          status = validated.header.status,
          headers = convertedHeaders,
          entity = entity,
          protocol = protocol
        )
        response
      }
    } {
      // Fallback response in case an exception is thrown during normal error handling
      HttpResponse(
        status = Status.INTERNAL_SERVER_ERROR,
        headers = immutable.Seq(Connection("close")),
        entity = HttpEntity.Empty,
        protocol = protocol
      )
    }
  }

  def parseContentType(contentType: Option[String]): ContentType = {
    // actually play allows content types to be not spec compliant
    // so we can't rely on the parsed content type of akka
    contentType.fold(ContentTypes.NoContentType: ContentType) { ct =>
      MediaType.custom(ct, binary = true) match {
        case b: MediaType.Binary => ContentType(b)
        case _ => ContentTypes.NoContentType
      }
    }
  }

  def convertResultBody(
    requestHeaders: RequestHeader,
    result: Result,
    protocol: HttpProtocol): ResponseEntity = {

    val contentType = parseContentType(result.body.contentType)

    result.body match {
      case PlayHttpEntity.Strict(data, _) =>
        HttpEntity.Strict(contentType, data)

      case PlayHttpEntity.Streamed(data, Some(contentLength), _) if contentLength == 0 =>
        HttpEntity.Strict(contentType, ByteString.empty)

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
    headers.flatMap {
      case (HeaderNames.SET_COOKIE, value) =>
        resultUtils.splitSetCookieHeaderValue(value).map(RawHeader(HeaderNames.SET_COOKIE, _))
      case (name, value) if name != HeaderNames.TRANSFER_ENCODING =>
        HttpHeader.parse(name, value) match {
          case HttpHeader.ParsingResult.Ok(header, errors /* errors are ignored if Ok */ ) =>
            header :: Nil
          case HttpHeader.ParsingResult.Error(error) =>
            sys.error(s"Error parsing header: $error")
        }

      case _ => Nil
    }(collection.breakOut): Vector[HttpHeader]
  }
}

final case class AkkaHeadersWrapper(
    request: HttpRequest,
    knownContentLength: Option[String],
    hs: immutable.Seq[HttpHeader],
    isChunked: Option[String],
    uri: String
) extends Headers(null) {
  import AkkaHeadersWrapper._

  private lazy val contentType = request.entity.contentType.value

  override lazy val headers: Seq[(String, String)] = {
    val h0 = (HeaderNames.CONTENT_TYPE -> contentType) +: hs.map(h => h.name() -> h.value)
    val h1 = knownContentLength match {
      case Some(cl) => (HeaderNames.CONTENT_LENGTH -> cl) +: h0
      case _ => h0
    }
    val h2 = isChunked match {
      case Some(ch) => (HeaderNames.TRANSFER_ENCODING -> ch) +: h1
      case _ => h1
    }
    h2
  }

  override def hasHeader(headerName: String): Boolean =
    headerName.toLowerCase(Locale.ROOT) match {
      case CONTENT_LENGTH_LOWER_CASE => knownContentLength.isDefined
      case TRANSFER_ENCODING_LOWER_CASE => isChunked.isDefined
      case CONTENT_TYPE_LOWER_CASE => true
      case _ => get(headerName).isDefined
    }

  override def hasBody: Boolean = request.entity match {
    case HttpEntity.Strict(_, data) => data.length > 0
    case _ => true
  }

  override def apply(key: String): String =
    get(key).getOrElse(throw new RuntimeException(s"Header with name $key not found!"))

  override def get(key: String): Option[String] =
    key.toLowerCase(Locale.ROOT) match {
      case CONTENT_LENGTH_LOWER_CASE => knownContentLength
      case TRANSFER_ENCODING_LOWER_CASE => isChunked
      case CONTENT_TYPE_LOWER_CASE => Some(contentType)
      case lowerCased => hs.collectFirst { case h if h.is(lowerCased) => h.value }
    }

  override def getAll(key: String): immutable.Seq[String] =
    key.toLowerCase(Locale.ROOT) match {
      case CONTENT_LENGTH_LOWER_CASE => knownContentLength.toList
      case TRANSFER_ENCODING_LOWER_CASE => isChunked.toList
      case CONTENT_TYPE_LOWER_CASE => contentType :: Nil
      case lowerCased => hs.collect { case h if h.is(lowerCased) => h.value }
    }

  override lazy val keys: immutable.Set[String] =
    hs.map(_.name).toSet ++
      Set(CONTENT_LENGTH_LOWER_CASE, TRANSFER_ENCODING_LOWER_CASE, CONTENT_TYPE_LOWER_CASE).filter(hasHeader)

  // note that these are rarely used, mostly just in tests
  override def add(headers: (String, String)*): AkkaHeadersWrapper =
    copy(hs = this.hs ++ raw(headers))

  override def remove(keys: String*): Headers =
    copy(hs = hs.filterNot(keys.contains))

  override def replace(headers: (String, String)*): Headers = {
    val replaced = hs.filterNot(h => headers.exists(rm => h.is(rm._1))) ++ raw(headers)
    copy(hs = replaced)
  }

  override def equals(other: Any): Boolean = {
    other match {
      case that: AkkaHeadersWrapper => that.request == this.request
      case _ => false
    }
  }

  private def raw(headers: Seq[(String, String)]): Seq[RawHeader] =
    headers.map(t => RawHeader(t._1, t._2))

  override def hashCode: Int = request.hashCode()
}

object AkkaHeadersWrapper {
  val CONTENT_LENGTH_LOWER_CASE = "content-length"
  val CONTENT_TYPE_LOWER_CASE = "content-type"
  val TRANSFER_ENCODING_LOWER_CASE = "transfer-encoding"
}
