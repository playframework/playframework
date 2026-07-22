/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.pekkohttp

import java.net.InetSocketAddress
import java.net.URI
import java.security.cert.X509Certificate
import java.util.Locale
import javax.net.ssl.SSLPeerUnverifiedException

import scala.collection.immutable
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.Try

import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.model.headers._
import org.apache.pekko.http.scaladsl.settings.ParserSettings
import org.apache.pekko.http.scaladsl.util.FastFuture._
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString
import play.api.http.{ HttpEntity => PlayHttpEntity }
import play.api.http.HttpChunk
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
import play.mvc.Http.HeaderNames

/**
 * Conversions between Pekko's and Play's HTTP model objects.
 */
private[server] class PekkoModelConversion(
    resultUtils: ServerResultUtils,
    forwardedHeaderHandler: ForwardedHeaderHandler,
    clientCertificateHeaderHandler: ClientCertificateHeaderHandler,
    illegalResponseHeaderValue: ParserSettings.IllegalResponseHeaderValueProcessingMode
) {
  private val logger = Logger(getClass)

  /**
   * Convert an Pekko `HttpRequest` to a `RequestHeader`.
   */
  def convertRequestHeader(
      remoteAddress: InetSocketAddress,
      secureProtocol: Boolean,
      request: HttpRequest
  ): Try[RequestHeader] = Try {
    val headers                         = convertRequestHeadersPekko(request)
    val (parsedPath, parsedQueryString) = PathAndQueryParser.parse(headers.uri)
    val rt                              = new RequestTarget {
      override lazy val uri: URI = new URI(headers.uri)

      override def uriString: String = headers.uri

      override val path: String = parsedPath

      override val queryString: String = parsedQueryString.stripPrefix("?")

      override lazy val queryMap: Map[String, Seq[String]] = {
        try {
          request.uri.query(mode = Uri.ParsingMode.Relaxed).toMultiMap
        } catch {
          case NonFatal(e) =>
            logger.warn("Failed to parse query string; returning empty map.", e)
            Map.empty
        }
      }
    }
    createRequestHeader(headers, secureProtocol, remoteAddress, rt, request)
  }

  def createRequestHeader(
      headers: Headers,
      secureProtocol: Boolean,
      remoteAddress: InetSocketAddress,
      requestTarget: RequestTarget,
      request: HttpRequest
  ): RequestHeader = {
    val transport     = createTransport(remoteAddress, secureProtocol, request)
    val rawRemote     = RemoteInfo.fromPeer(transport.peer)
    val directScheme  = RequestHeader.initialScheme(transport)
    val initialTarget = RequestHeader
      .initialRequestTarget(request.method.name, requestTarget, request.protocol.value, headers)
      .fold(error => throw new IllegalArgumentException(error), identity)
    val forwarding = forwardedHeaderHandler.forwardedRequest(
      rawRemote,
      headers,
      directScheme,
      initialTarget.authority
    )
    val effectiveScheme = RequestHeader
      .effectiveScheme(initialTarget.scheme, directScheme, forwarding.scheme)
      .fold(error => throw new IllegalArgumentException(error), identity)
    val normalizedTarget   = RequestHeader.normalizeRequestTargetPath(requestTarget, initialTarget)
    val clientCertificates = clientCertificateHeaderHandler.clientCertificates(transport, headers)
    val attrs              = TypedMap(
      // This is the earliest stage of a Play request at which we can set an id.
      RequestAttrKey.Id -> RequestIdProvider.freshId(),
    )
    new RequestHeaderImpl(
      forwarding.remote,
      request.method.name,
      normalizedTarget,
      request.protocol.value,
      headers,
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
      headers: Headers,
      secureProtocol: Boolean,
      remoteAddress: InetSocketAddress,
      requestTarget: RequestTarget,
      request: HttpRequest,
      requestFailure: Throwable
  ): RequestHeader = {
    val transport          = createTransport(remoteAddress, secureProtocol, request)
    val clientCertificates =
      clientCertificateHeaderHandler.clientCertificatesForErrorRequest(transport, headers, requestFailure)
    val rawRemote     = RemoteInfo.fromPeer(transport.peer)
    val directScheme  = RequestHeader.initialScheme(transport)
    val initialTarget = RequestHeader
      .initialRequestTarget(request.method.name, requestTarget, request.protocol.value, headers)
      .toOption
    val initialAuthority = initialTarget.flatMap(_.authority)
    val forwarding       = try {
      forwardedHeaderHandler.forwardedRequest(rawRemote, headers, directScheme, initialAuthority)
    } catch {
      case NonFatal(error) =>
        logger.warn("Failed to apply forwarded metadata to an error request; using direct metadata.", error)
        ForwardedHeaderHandler.ParsedForwarding(rawRemote, directScheme, initialAuthority)
    }
    val scheme = RequestHeader
      .effectiveScheme(initialTarget.flatMap(_.scheme), directScheme, forwarding.scheme)
      .getOrElse(forwarding.scheme)
    val attrs = TypedMap(RequestAttrKey.Id -> RequestIdProvider.freshId())

    new RequestHeaderImpl(
      forwarding.remote,
      request.method.name,
      requestTarget,
      request.protocol.value,
      headers,
      attrs,
      transport,
      clientCertificates.clientCertificate,
      scheme,
      forwarding.authority,
      clientCertificates.xForwardedClientCertificates
    )
  }

  private def createTransport(
      remoteAddress: InetSocketAddress,
      secureProtocol: Boolean,
      request: HttpRequest
  ): TransportConnection = {
    val peer = PeerEndpoint(remoteAddress.getAddress, Some(remoteAddress.getPort))
    val tls  = Option.when(secureProtocol) {
      val peerCertificates = try {
        request
          .header[`Tls-Session-Info`]
          .map { tlsSessionInfo =>
            immutable.ArraySeq
              .unsafeWrapArray(tlsSessionInfo.getSession.getPeerCertificates)
              .collect { case x509: X509Certificate => x509 }
          }
          .getOrElse(Seq.empty)
      } catch {
        case _: SSLPeerUnverifiedException => Seq.empty
      }
      TransportTls(peerCertificates)
    }
    TransportConnection(peer, tls)
  }

  /**
   * Convert the request headers of an Pekko `HttpRequest` to a Play `Headers` object.
   */
  def convertRequestHeadersPekko(request: HttpRequest): PekkoHeadersWrapper = {
    var knownContentLength: Option[String] = None
    var isChunked: Option[String]          = None

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
      case `Raw-Request-URI`(u)   => requestUri = u
      case e: `Transfer-Encoding` => isChunked = Some(e.value())
      case _                      => // continue
    }
    if (requestUri eq null) requestUri = request.uri.toString() // fallback value

    new PekkoHeadersWrapper(request, knownContentLength, request.headers, isChunked, requestUri)
  }

  /**
   * Convert an Pekko `HttpRequest` to an `Enumerator` of the request body.
   */
  def convertRequestBody(request: HttpRequest): Either[ByteString, Source[ByteString, Any]] = {
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
        Right(chunks.filter(!_.isLastChunk).map(_.data()))
    }
  }

  /**
   * Convert a Play `Result` object into an Pekko `HttpResponse` object.
   */
  def convertResult(
      requestHeaders: RequestHeader,
      unvalidated: Result,
      protocol: HttpProtocol,
      errorHandler: HttpErrorHandler
  )(implicit mat: Materializer): Future[HttpResponse] = {
    import play.core.Execution.Implicits.trampoline

    resultUtils.resultConversionWithErrorHandling(requestHeaders, unvalidated, errorHandler) { unvalidated =>
      // Convert result

      resultUtils.validateResult(requestHeaders, unvalidated, errorHandler).fast.map { (validated: Result) =>
        val convertedHeaders = convertHeaders(validated.header.headers)
        val entity           = convertResultBody(requestHeaders, validated, protocol)
        val intStatus        = validated.header.status
        val statusCode       = StatusCodes.getForKey(intStatus).getOrElse {
          val reasonPhrase = validated.header.reasonPhrase.getOrElse("")
          if (intStatus >= 600 || intStatus < 100) {
            StatusCodes.custom(intStatus, reasonPhrase, defaultMessage = "", isSuccess = false, allowsEntity = true)
          } else {
            StatusCodes.custom(intStatus, reasonPhrase)
          }
        }
        val response = HttpResponse(
          status = statusCode,
          headers = convertedHeaders,
          entity = entity,
          protocol = protocol
        )
        response
      }
    } {
      // Fallback response in case an exception is thrown during normal error handling
      HttpResponse(
        status = StatusCodes.InternalServerError,
        headers = immutable.Seq(Connection("close")),
        entity = HttpEntity.Empty,
        protocol = protocol
      )
    }
  }

  def parseContentType(contentType: Option[String]): ContentType = {
    contentType.fold(ContentTypes.NoContentType: ContentType) { ct =>
      ContentType
        .parse(ct)
        .left
        .map { errors => throw new RuntimeException(s"Error parsing response Content-Type: <$ct>: $errors") }
        .merge
    }
  }

  def convertResultBody(requestHeaders: RequestHeader, result: Result, protocol: HttpProtocol): ResponseEntity = {
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
        val pekkoChunks = data.map {
          case HttpChunk.Chunk(chunk) =>
            HttpEntity.Chunk(chunk)
          case HttpChunk.LastChunk(trailers) if trailers.headers.isEmpty =>
            HttpEntity.LastChunk
          case HttpChunk.LastChunk(trailers) =>
            HttpEntity.LastChunk(trailer = convertHeaders(trailers.headers))
        }
        HttpEntity.Chunked(contentType, pekkoChunks)
    }
  }

  // These headers are listed in the Pekko HTTP's HttpResponseRenderer class as being invalid when given as RawHeaders
  private val mustParseHeaders: Set[String] = Set(
    HeaderNames.CONTENT_TYPE,
    HeaderNames.CONTENT_LENGTH,
    HeaderNames.TRANSFER_ENCODING,
    HeaderNames.DATE,
    HeaderNames.SERVER,
    HeaderNames.CONNECTION
  ).map(_.toLowerCase(Locale.ROOT))

  private def convertHeaders(headers: Iterable[(String, String)]): immutable.Seq[HttpHeader] = {
    headers.iterator.flatMap {
      case (name, value) =>
        val lowerName = name.toLowerCase(Locale.ROOT)
        if (lowerName == "set-cookie") {
          resultUtils.splitSetCookieHeaderValue(value).map(RawHeader(HeaderNames.SET_COOKIE, _))
        } else if (mustParseHeaders.contains(lowerName)) {
          parseHeader(name, value)
        } else {
          resultUtils.validateHeaderNameChars(name)
          resultUtils.validateHeaderValueChars(value)
          RawHeader(name, value) :: Nil
        }
    }.toVector
  }

  private def parseHeader(name: String, value: String): Seq[HttpHeader] = {
    HttpHeader.parse(name, value) match {
      case HttpHeader.ParsingResult.Ok(header, errors /* errors are ignored if Ok */ ) =>
        if (!header.renderInResponses()) {
          // since play did not enforce the http spec when it came to headers
          // we actually relax it by converting the parsed header to a RawHeader
          // This will still fail on content-type, content-length, transfer-encoding, date, server and connection headers.
          illegalResponseHeaderValue match {
            case ParserSettings.IllegalResponseHeaderValueProcessingMode.Warn =>
              logger.warn(
                s"HTTP Header '$header' is not allowed in responses, you can turn off this warning by setting `play.server.pekko.illegal-response-header-value-processing-mode = ignore`"
              )
              RawHeader(name, value) :: Nil
            case ParserSettings.IllegalResponseHeaderValueProcessingMode.Ignore =>
              RawHeader(name, value) :: Nil
            case ParserSettings.IllegalResponseHeaderValueProcessingMode.Error =>
              logger.error(s"HTTP Header '$header' is not allowed in responses")
              Nil
          }
        } else {
          header :: Nil
        }
      case HttpHeader.ParsingResult.Error(error) =>
        sys.error(s"Error parsing header: $error")
    }
  }
}

final case class PekkoHeadersWrapper(
    request: HttpRequest,
    knownContentLength: Option[String],
    hs: immutable.Seq[HttpHeader],
    isChunked: Option[String],
    uri: String
) extends Headers(null) {
  import PekkoHeadersWrapper._

  private lazy val contentType: Option[String] = {
    if (request.entity.contentType == ContentTypes.NoContentType)
      None
    else
      Some(request.entity.contentType.value)
  }

  override lazy val headers: Seq[(String, String)] = {
    val h: immutable.Seq[(String, String)] = hs.map(h => h.name() -> h.value)
    val h0                                 = contentType match {
      case Some(ct) => (HeaderNames.CONTENT_TYPE -> ct) +: h
      case None     => h
    }
    val h1 = knownContentLength match {
      case Some(cl) => (HeaderNames.CONTENT_LENGTH -> cl) +: h0
      case _        => h0
    }
    val h2 = isChunked match {
      case Some(ch) => (HeaderNames.TRANSFER_ENCODING -> ch) +: h1
      case _        => h1
    }
    h2
  }

  override def hasHeader(headerName: String): Boolean =
    headerName.toLowerCase(Locale.ROOT) match {
      case CONTENT_LENGTH_LOWER_CASE    => knownContentLength.isDefined
      case TRANSFER_ENCODING_LOWER_CASE => isChunked.isDefined
      case CONTENT_TYPE_LOWER_CASE      => contentType.isDefined
      case _                            => get(headerName).isDefined
    }

  override def hasBody: Boolean = request.entity match {
    case HttpEntity.Strict(_, data) => data.length > 0
    case _                          => true
  }

  override def apply(key: String): String =
    get(key).getOrElse(throw new RuntimeException(s"Header with name $key not found!"))

  override def get(key: String): Option[String] =
    key.toLowerCase(Locale.ROOT) match {
      case CONTENT_LENGTH_LOWER_CASE    => knownContentLength
      case TRANSFER_ENCODING_LOWER_CASE => isChunked
      case CONTENT_TYPE_LOWER_CASE      => contentType
      case lowerCased                   => hs.collectFirst { case h if h.is(lowerCased) => h.value }
    }

  override def getAll(key: String): immutable.Seq[String] =
    key.toLowerCase(Locale.ROOT) match {
      case CONTENT_LENGTH_LOWER_CASE    => knownContentLength.toList
      case TRANSFER_ENCODING_LOWER_CASE => isChunked.toList
      case CONTENT_TYPE_LOWER_CASE      => contentType.toList
      case lowerCased                   => hs.collect { case h if h.is(lowerCased) => h.value }
    }

  override lazy val keys: immutable.Set[String] = {
    hs.map(_.name).toSet ++
      Set(CONTENT_LENGTH_LOWER_CASE, TRANSFER_ENCODING_LOWER_CASE, CONTENT_TYPE_LOWER_CASE).filter(hasHeader)
  }

  // note that these are rarely used, mostly just in tests
  override def add(headers: (String, String)*): PekkoHeadersWrapper =
    copy(hs = this.hs ++ raw(headers))

  override def remove(keys: String*): Headers = {
    val lowerCasedKeys = keys.map(_.toLowerCase(Locale.ROOT))
    copy(
      hs = hs.filterNot(h => lowerCasedKeys.exists(h.is)),
      knownContentLength =
        if (lowerCasedKeys.contains(CONTENT_LENGTH_LOWER_CASE))
          None
        else
          knownContentLength,
      request =
        if (lowerCasedKeys.contains(CONTENT_TYPE_LOWER_CASE))
          request.mapEntity(_.withContentType(ContentTypes.NoContentType))
        else
          request
    )
  }

  override def replace(headers: (String, String)*): Headers =
    remove(headers.map(_._1)*).add(headers*)

  override def equals(other: Any): Boolean =
    other match {
      case that: PekkoHeadersWrapper => that.request == this.request
      case _                         => false
    }

  private def raw(headers: Seq[(String, String)]): Seq[RawHeader] =
    headers.map(t => RawHeader(t._1, t._2))

  override def hashCode: Int = request.hashCode()
}

object PekkoHeadersWrapper {
  val CONTENT_LENGTH_LOWER_CASE    = "content-length"
  val CONTENT_TYPE_LOWER_CASE      = "content-type"
  val TRANSFER_ENCODING_LOWER_CASE = "transfer-encoding"
}
