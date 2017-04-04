/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.akkahttp

import java.net.{ InetAddress, InetSocketAddress, URI }
import java.util.Locale

import akka.http.javadsl.model.headers.RawRequestURI
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Logger
import play.api.http.HeaderNames._
import play.api.http.{ HeaderNames, HttpChunk, HttpErrorHandler, Status, HttpEntity => PlayHttpEntity }
import play.api.libs.typedmap.TypedMap
import play.api.mvc._
import play.api.mvc.request.{ RemoteConnection, RequestTarget }
import play.core.server.common.{ ForwardedHeaderHandler, ServerResultUtils }
import play.core.utils.CaseInsensitiveOrdered

import scala.collection.immutable
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.TreeSet
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
      case `Raw-Request-URI`(u) =>
        requestUri = u
      case e: `Transfer-Encoding` =>
        isChunked = Some(e.value())
      case _ => // continue
    }
    if (requestUri eq null) requestUri = request.uri.toString() // fallback value

    new AkkaHeadersWrapper(request, knownContentLength, request.headers, isChunked, requestUri)
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
    protocol: HttpProtocol,
    errorHandler: HttpErrorHandler)(implicit mat: Materializer): Future[HttpResponse] = {

    import play.core.Execution.Implicits.trampoline

    resultUtils.resultConversionWithErrorHandling(requestHeaders, unvalidated, errorHandler) { unvalidated =>
      // Convert result
      resultUtils.validateResult(requestHeaders, unvalidated, errorHandler).map { validated: Result =>
        val convertedHeaders: AkkaHttpHeaders = convertResponseHeaders(validated.header.headers)
        val entity = convertResultBody(requestHeaders, convertedHeaders, validated, protocol)
        val connectionHeader = resultUtils.determineConnectionHeader(requestHeaders, validated)
        val closeHeader = connectionHeader.header.map(Connection(_))
        val response = HttpResponse(
          status = validated.header.status,
          headers = convertedHeaders.misc ++ closeHeader,
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
    convertedHeaders: AkkaHttpHeaders,
    result: Result,
    protocol: HttpProtocol): ResponseEntity = {

    val contentType = parseContentType(result.body.contentType)

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
    val rawHeaders: Iterable[(String, String)] = resultUtils.splitSetCookieHeaders(playHeaders)
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

final case class AkkaHeadersWrapper(
    request: HttpRequest,
    knownContentLength: Option[String],
    hs: immutable.Seq[HttpHeader],
    isChunked: Option[String],
    uri: String
) extends Headers(null) {

  override lazy val headers: Seq[(String, String)] =
    hs.map(h => h.name() -> h.value)

  // note that these are rarely used, mostly just in tests
  override def add(headers: (String, String)*): AkkaHeadersWrapper =
    copy(hs = this.hs ++ raw(headers))

  override def apply(key: String): String =
    get(key).getOrElse(throw new RuntimeException(s"Header with name ${key} not found!"))

  override def get(key: String): Option[String] =
    key match {
      case HeaderNames.CONTENT_LENGTH => knownContentLength
      case HeaderNames.TRANSFER_ENCODING => isChunked
      case HeaderNames.CONTENT_TYPE => Some(request.entity.contentType.value)
      case _ =>
        val lower = key.toLowerCase(Locale.ROOT)
        hs.collectFirst({ case h if h.lowercaseName == lower => h.value })
    }

  // note that these are rarely used, mostly just in tests
  override def getAll(key: String): immutable.Seq[String] =
    hs.collect({ case h if h.is(key) => h.value })

  override lazy val keys: immutable.Set[String] =
    hs.map(_.name).toSet

  // note that these are rarely used, mostly just in tests
  override def remove(keys: String*): Headers =
    copy(hs = hs.filterNot(keys.contains))

  // note that these are rarely used, mostly just in tests
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

  override def hashCode: Int =
    request.hashCode()

}
