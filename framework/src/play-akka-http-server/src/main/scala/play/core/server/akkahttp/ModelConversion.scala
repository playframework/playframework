package play.core.server.akkahttp

import akka.http.model._
import akka.http.model.ContentType
import akka.http.model.headers._
import akka.http.model.parser.HeaderParser
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import java.net.InetSocketAddress
import org.reactivestreams.Publisher
import play.api.Logger
import play.api.http.HeaderNames._
import play.api.libs.iteratee._
import play.api.libs.streams.Streams
import play.api.mvc._
import play.core.server.common.ServerResultUtils
import scala.collection.immutable
import scala.concurrent.Future

/**
 * Conversions between Akka's and Play's HTTP model objects.
 */
private[akkahttp] object ModelConversion {

  private val logger = Logger(getClass)

  /**
   * Convert an Akka `HttpRequest` to a `RequestHeader` and an `Eumerator`
   * for its body.
   */
  def convertRequest(
    requestId: Long,
    remoteAddress: InetSocketAddress,
    request: HttpRequest)(implicit fm: FlowMaterializer): (RequestHeader, Enumerator[Array[Byte]]) = {
    (
      convertRequestHeader(requestId, remoteAddress, request),
      convertRequestBody(request)
    )
  }

  /**
   * Convert an Akka `HttpRequest` to a `RequestHeader`.
   */
  private def convertRequestHeader(
    requestId: Long,
    remoteAddress: InetSocketAddress,
    request: HttpRequest): RequestHeader = {
    val remoteHostAddress = remoteAddress.getAddress.getHostAddress
    // Taken from PlayDefaultUpstreamHander
    new RequestHeader {
      val id = requestId
      // Send a tag so our tests can tell which kind of server we're using.
      // We could get NettyServer to send a similar tag, but for the moment
      // let's not, just in case it slows NettyServer down a bit.
      val tags = Map("HTTP_SERVER" -> "akka-http")
      def uri = request.uri.toString
      def path = request.uri.path.toString
      def method = request.method.name
      def version = request.protocol.toString
      def queryString = request.uri.query.toMultiMap
      val headers = convertRequestHeaders(request)
      def remoteAddress = remoteHostAddress
      def secure = false // FIXME: Stub
      def username = ??? // FIXME: Stub
    }
  }

  /**
   * Convert the request headers of an Akka `HttpRequest` to a Play
   * `Headers` object.
   */
  private def convertRequestHeaders(request: HttpRequest): Headers = {
    val entityHeaders: Seq[(String, String)] = request.entity match {
      case HttpEntity.Strict(contentType, _) =>
        Seq((CONTENT_TYPE, contentType.value))
      case HttpEntity.Default(contentType, contentLength, _) =>
        Seq((CONTENT_TYPE, contentType.value), (CONTENT_LENGTH, contentLength.toString))
      case HttpEntity.Chunked(contentType, _) =>
        Seq((CONTENT_TYPE, contentType.value))
    }
    val normalHeaders: Seq[(String, String)] = request.headers.map((rh: HttpHeader) => (rh.name, rh.value))
    new Headers {
      val data: Seq[(String, Seq[String])] = (entityHeaders ++ normalHeaders).groupBy(_._1).mapValues(_.map(_._2)).to[Seq]
    }
  }

  /**
   * Convert an Akka `HttpRequest` to an `Enumerator` of the request body.
   */
  private def convertRequestBody(
    request: HttpRequest)(implicit fm: FlowMaterializer): Enumerator[Array[Byte]] = {
    import play.api.libs.iteratee.Execution.Implicits.trampoline
    request.entity match {
      case HttpEntity.Strict(_, data) if data.isEmpty =>
        Enumerator.eof
      case HttpEntity.Strict(_, data) =>
        Enumerator.apply[Array[Byte]](data.toArray) >>> Enumerator.eof
      case HttpEntity.Default(_, 0, _) =>
        Enumerator.eof
      case HttpEntity.Default(contentType, contentLength, pubr) =>
        // FIXME: should do something with the content-length?
        AkkaStreamsConversion.sourceToEnumerator(pubr) &> Enumeratee.map((data: ByteString) => data.toArray)
      case HttpEntity.Chunked(contentType, chunks) =>
        // FIXME: Don't enumerate LastChunk?
        // FIXME: do something with trailing headers?
        AkkaStreamsConversion.sourceToEnumerator(chunks) &> Enumeratee.map((chunk: HttpEntity.ChunkStreamPart) => chunk.data.toArray)
    }
  }

  /**
   * Convert a Play `Result` object into an Akka `HttpResponse` object.
   */
  def convertResult(
    result: Result,
    protocol: HttpProtocol): Future[HttpResponse] = {

    val convertedHeaders: AkkaHttpHeaders = convertResponseHeaders(result.header.headers)
    import play.api.libs.iteratee.Execution.Implicits.trampoline
    convertResultBody(convertedHeaders, result, protocol).flatMap {
      case Left(alternativeResult) =>
        convertResult(alternativeResult, protocol)
      case Right(entity) =>
        Future.successful(HttpResponse(
          status = result.header.status,
          headers = convertedHeaders.misc,
          entity = entity,
          protocol = protocol))
    }
  }

  def convertResultBody(
    convertedHeaders: AkkaHttpHeaders,
    result: Result,
    protocol: HttpProtocol): Future[Either[Result, ResponseEntity]] = {

    import Execution.Implicits.trampoline

    def dataSource(enum: Enumerator[Array[Byte]]): Source[ByteString] = {
      val dataEnum: Enumerator[ByteString] = enum.map(ByteString(_)) >>> Enumerator.eof
      AkkaStreamsConversion.enumeratorToSource(dataEnum)
    }
    def chunksSource(enum: Enumerator[Array[Byte]]) = {
      val chunksEnum = (
        enum.map(HttpEntity.ChunkStreamPart(_)) >>>
        Enumerator.enumInput(Input.El(HttpEntity.LastChunk)) >>>
        Enumerator.eof
      )
      AkkaStreamsConversion.enumeratorToSource(chunksEnum)
    }

    val isHttp10 = protocol == HttpProtocols.`HTTP/1.0`
    ServerResultUtils.determineResultStreaming(result, isHttp10).map {
      case ServerResultUtils.CannotStream(reason, alternativeResult) =>
        logger.warn(s"Cannot send result, sending error result instead: $reason")
        Left(alternativeResult)
      case ServerResultUtils.StreamWithClose(enum) =>
        Right(HttpEntity.CloseDelimited(
          contentType = convertedHeaders.contentType,
          data = dataSource(enum)
        ))
      case ServerResultUtils.StreamWithKnownLength(enum) =>
        Right(HttpEntity.Default(
          contentType = convertedHeaders.contentType,
          contentLength = convertedHeaders.contentLength.get,
          data = dataSource(enum)
        ))
      case ServerResultUtils.StreamWithStrictBody(body) =>
        Right(HttpEntity.Strict(
          contentType = convertedHeaders.contentType,
          data = if (body.isEmpty) ByteString.empty else ByteString(body)
        ))
      case ServerResultUtils.UseExistingTransferEncoding(transferEncodedEnum) =>
        assert(convertedHeaders.transferEncoding.isDefined) // Guaranteed by ServerResultUtils
        val transferEncoding = convertedHeaders.transferEncoding.get
        transferEncoding match {
          case immutable.Seq(TransferEncodings.chunked) =>
            Right(HttpEntity.Chunked(
              contentType = convertedHeaders.contentType,
              chunks = chunksSource(transferEncodedEnum &> Results.dechunk)
            ))
          case other =>
            logger.warn(s"Cannot send result, sending error instead: Akka HTTP server only supports 'Transfer-Encoding: chunked', was $other")
            Left(Results.InternalServerError(""))
        }
      case ServerResultUtils.PerformChunkedTransferEncoding(enum) =>
        Right(HttpEntity.Chunked(
          contentType = convertedHeaders.contentType,
          chunks = chunksSource(enum)
        ))
    }
  }

  /**
   * A representation of Akka HTTP headers separate from an `HTTPMessage`.
   * Akka HTTP treats some headers specially and these are split out into
   * separate values.
   *
   * @misc General headers. Guaranteed not to contain any of the special
   * headers stored in the other values.
   * @contentType If present, the value of the `Content-Type` header.
   * @contentLength If present, the value of the `Content-Length` header.
   */
  case class AkkaHttpHeaders(
    misc: immutable.Seq[HttpHeader],
    contentType: ContentType,
    contentLength: Option[Long],
    transferEncoding: Option[immutable.Seq[TransferEncoding]])

  /**
   * Convert Play response headers into `HttpHeader` objects, then separate
   * out any special headers.
   */
  private def convertResponseHeaders(
    playHeaders: Map[String, String]): AkkaHttpHeaders = {
    val rawHeaders = ServerResultUtils.splitHeadersIntoSeq(playHeaders).map {
      case (name, value) => RawHeader(name, value)
    }
    val convertedHeaders: List[HttpHeader] = HeaderParser.parseHeaders(rawHeaders.to[List]) match {
      case (Nil, headers) => headers
      case (errors, _) => sys.error(s"Error parsing response headers: $errors")
    }
    val emptyHeaders = AkkaHttpHeaders(immutable.Seq.empty, ContentTypes.`application/octet-stream`, None, None)
    convertedHeaders.foldLeft(emptyHeaders) {
      case (accum, ct: `Content-Type`) =>
        accum.copy(contentType = ct.contentType)
      case (accum, cl: `Content-Length`) =>
        accum.copy(contentLength = Some(cl.length))
      case (accum, te: `Transfer-Encoding`) =>
        accum.copy(transferEncoding = Some(te.encodings))
      case (accum, miscHeader) =>
        accum.copy(misc = accum.misc :+ miscHeader)
    }
  }

}