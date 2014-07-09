package play.core.server.akkahttp

import akka.http.model._
import akka.http.model.ContentType
import akka.http.model.headers._
import akka.http.model.parser.HeaderParser
import akka.util.ByteString
import java.net.InetSocketAddress
import org.reactivestreams.Publisher
import play.api.libs.iteratee.{ Enumeratee, Enumerator, Input }
import play.api.libs.streams.Streams
import play.api.mvc._
import scala.collection.immutable

/**
 * Conversions between Akka's and Play's HTTP model objects.
 */
private[akkahttp] object ModelConversion {

  /**
   * Convert an Akka `HttpRequest` to a `RequestHeader` and an `Eumerator`
   * for its body.
   */
  def convertRequest(
    requestId: Long,
    remoteAddress: InetSocketAddress,
    request: HttpRequest): (RequestHeader, Enumerator[Array[Byte]]) = {
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
    val entityHeaders: Seq[HttpHeader] = request.entity match {
      case HttpEntity.Strict(contentType, _) =>
        Seq(`Content-Type`(contentType))
      case HttpEntity.Default(contentType, contentLength, _) =>
        Seq(`Content-Type`(contentType), `Content-Length`(contentLength))
      case HttpEntity.Chunked(contentType, _) =>
        Seq(`Content-Type`(contentType))
    }
    val allHeaders: Seq[HttpHeader] = request.headers ++ entityHeaders
    val pairs: scala.collection.Seq[(String, String)] = allHeaders.map((rh: HttpHeader) => (rh.name, rh.value))
    new Headers {
      val data: Seq[(String, Seq[String])] = pairs.groupBy(_._1).mapValues(_.map(_._2)).to[Seq]
    }
  }

  /**
   * Convert an Akka `HttpRequest` to an `Enumerator` of the request body.
   */
  private def convertRequestBody(
    request: HttpRequest): Enumerator[Array[Byte]] = {
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
        Streams.publisherToEnumerator(pubr) &> Enumeratee.map((data: ByteString) => data.toArray)
      case HttpEntity.Chunked(contentType, chunks) =>
        // FIXME: Don't enumerate LastChunk?
        // FIXME: do something with trailing headers?
        Streams.publisherToEnumerator(chunks) &> Enumeratee.map((chunk: HttpEntity.ChunkStreamPart) => chunk.data.toArray)
    }
  }

  /**
   * Convert a Play `Result` object into an Akka `HttpResponse` object.
   */
  def convertResult(
    result: Result): HttpResponse = {
    val convertedHeaders: AkkaHttpHeaders = convertResponseHeaders(result.header.headers)
    val entity = {
      import play.api.libs.iteratee.Execution.Implicits.trampoline
      val contentType = convertedHeaders.contentType.getOrElse(ContentTypes.`application/octet-stream`)
      val contentLength: Option[Long] = convertedHeaders.contentLength
      contentLength match {
        case None =>
          val chunksEnum = (
            result.body.map(HttpEntity.ChunkStreamPart(_)) >>>
            Enumerator.enumInput(Input.El(HttpEntity.LastChunk)) >>>
            Enumerator.eof
          )
          val chunksProd = Streams.enumeratorToPublisher(chunksEnum)
          HttpEntity.Chunked(
            contentType = contentType,
            chunks = chunksProd
          )
        case Some(0) =>
          HttpEntity.Strict(
            contentType = contentType,
            data = ByteString.empty
          )
        case Some(l) =>
          val dataEnum: Enumerator[ByteString] = result.body.map(ByteString(_)) >>> Enumerator.eof
          val dataProd: Publisher[ByteString] = Streams.enumeratorToPublisher(dataEnum)
          // TODO: Check if values already available so we can use HttpEntity.Strict
          HttpEntity.Default(
            contentType = contentType,
            contentLength = l,
            data = dataProd
          )
      }
    }

    HttpResponse(
      status = result.header.status,
      headers = convertedHeaders.misc,
      entity = entity,
      protocol = HttpProtocols.`HTTP/1.1`) // FIXME
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
    contentType: Option[ContentType],
    contentLength: Option[Long])

  /**
   * Convert Play response headers into `HttpHeader` objects, then separate
   * out any special headers.
   */
  private def convertResponseHeaders(
    playHeaders: Map[String, String]): AkkaHttpHeaders = {
    val rawHeaders = playHeaders.map { case (name, value) => RawHeader(name, value) }
    val convertedHeaders: List[HttpHeader] = HeaderParser.parseHeaders(rawHeaders.to[List]) match {
      case (Nil, headers) => headers
      case (errors, _) => sys.error(s"Error parsing response headers: $errors")
    }
    AkkaHttpHeaders(
      misc = convertedHeaders.filter {
        case _: `Content-Type` => false
        case _: `Content-Length` => false
        case _ => true
      },
      contentType = convertedHeaders.collectFirst {
        case ct: `Content-Type` => ct.contentType
      },
      contentLength = convertedHeaders.collectFirst {
        case cl: `Content-Length` => cl.length
      }
    )
  }

}