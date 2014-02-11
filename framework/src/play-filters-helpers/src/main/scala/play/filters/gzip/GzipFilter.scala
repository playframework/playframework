package play.filters.gzip

import play.api.libs.iteratee._
import play.api.mvc._
import scala.concurrent.Future
import play.api.mvc.SimpleResult
import play.api.mvc.ResponseHeader
import org.jboss.netty.handler.codec.http.HttpHeaders.Names
import play.api.http.{ Status, MimeTypes }
import play.api.libs.concurrent.Execution.Implicits._

/**
 * A gzip filter.
 *
 * This filter may gzip the responses for any requests that aren't HEAD requests and specify an accept encoding of gzip.
 *
 * It will only gzip non chunked responses.  Chunked responses are often comet responses, gzipping will interfere in
 * that case.  If you want to gzip a chunked response, you can apply the gzip enumeratee manually to the enumerator.
 *
 * For non chunked responses, it won't gzip under the following conditions:
 *
 * - The response code is 204 or 304 (these codes MUST NOT contain a body, and an empty gzipped response is 20 bytes
 * long)
 * - The response already defines a Content-Encoding header
 * - The response content type is text/event-stream
 * - A custom shouldGzip function is supplied and it returns false
 *
 * Since gzipping changes the content length of the response, this filter may do some buffering.  If a content length
 * is sent by the action, that content length is filtered out and ignored.  If the connection flag on the result is
 * Close, the filter will not attempt to buffer, and will simply rely on the closing the response to signify the end
 * of the response.  Otherwise, it will buffer up to the configured chunkedThreshold, which defaults to 100kb.  If the
 * response fits in that buffer, the filter will send the content length, otherwise it falls back to sending a chunked
 * response, or if the protocol is HTTP/1.0, it closes the connection at the end of the response.
 *
 * You can use this filter in your project simply by including it in the Global filters, like this:
 *
 * {{{
 * object Global extends WithFilters(new GzipFilter()) {
 *   ...
 * }
 * }}}
 *
 * @param gzip The gzip enumeratee to use.
 * @param chunkedThreshold The content length threshold, after which the filter will switch to chunking the result.
 * @param shouldGzip Whether the given request/result should be gzipped.  This can be used, for example, to implement
 *                   black/white lists for gzipping by content type.
 */
class GzipFilter(gzip: Enumeratee[Array[Byte], Array[Byte]] = Gzip.gzip(GzipFilter.DefaultChunkSize),
    chunkedThreshold: Int = GzipFilter.DefaultChunkedThreshold,
    shouldGzip: (RequestHeader, ResponseHeader) => Boolean = (_, _) => true) extends EssentialFilter {

  import play.api.http.HeaderNames._
  import play.api.http.HttpProtocol._

  /**
   * Allows use with a custom chunked threshold from Java
   */
  def this(chunkedThreshold: Int) = this(Gzip.gzip(GzipFilter.DefaultChunkSize), chunkedThreshold, (_, _) => true)

  /**
   * This allows it to be used from Java
   */
  def this() = this(GzipFilter.DefaultChunkedThreshold)

  def apply(next: EssentialAction) = new EssentialAction {
    def apply(request: RequestHeader) = {
      if (mayCompress(request)) {
        next(request).mapM(result => handleResult(request, result))
      } else {
        next(request)
      }
    }
  }

  private def handleResult(request: RequestHeader, result: SimpleResult): Future[SimpleResult] = {
    if (shouldCompress(result.header) && shouldGzip(request, result.header)) {
      // If connection is close, don't bother buffering it, we can send it without a content length
      if (result.connection == HttpConnection.Close) {
        Future.successful(SimpleResult(
          header = result.header.copy(headers = setupHeader(result.header.headers)),
          body = result.body &> gzip,
          connection = result.connection
        ))
      } else {

        // Attempt to buffer it
        // left means we didn't buffer the whole thing before reaching the threshold, and contains the chunks that we did buffer
        // right means we did buffer it before reaching the threshold, and contains the chunks and the length of data
        def buffer(chunks: List[Array[Byte]], count: Int): Iteratee[Array[Byte], Either[List[Array[Byte]], (List[Array[Byte]], Int)]] = {
          Cont {
            case Input.EOF => Done(Right((chunks.reverse, count)), Input.EOF)
            // If we have 10 or less bytes already, then we have so far only seen the gzip header
            case Input.El(data) if count <= GzipFilter.GzipHeaderLength || count + data.length < chunkedThreshold =>
              buffer(data :: chunks, count + data.length)
            case Input.El(data) => Done(Left((data :: chunks).reverse))
            case Input.Empty => buffer(chunks, count)
          }
        }

        // Run the enumerator partially (means we get an enumerator that contains the rest of the input)
        Concurrent.runPartial(result.body &> gzip, buffer(Nil, 0)).map {
          // We successfully buffered the whole thing, so we have a content length
          case (Right((chunks, contentLength)), empty) =>
            SimpleResult(
              header = result.header.copy(headers = setupHeader(result.header.headers)
                + (CONTENT_LENGTH -> Integer.toString(contentLength))),
              // include the empty enumerator so that it's fully consumed
              // needed by New Relic monitoring, which tracks all promises within a request
              body = Enumerator.enumerate(chunks) >>> empty,
              connection = result.connection
            )
          // We still had some input remaining
          case (Left(chunks), remaining) => {
            if (request.version == HTTP_1_0) {
              // Don't chunk for HTTP/1.0
              SimpleResult(
                header = result.header.copy(headers = setupHeader(result.header.headers)),
                body = Enumerator.enumerate(chunks) >>> remaining,
                connection = HttpConnection.Close
              )
            } else {
              // Otherwise chunk
              SimpleResult(
                header = result.header.copy(headers = setupHeader(result.header.headers)
                  + (TRANSFER_ENCODING -> CHUNKED)),
                body = (Enumerator.enumerate(chunks) >>> remaining) &> Results.chunk,
                connection = result.connection
              )
            }
          }
        }
      }
    } else {
      Future.successful(result)
    }
  }

  /**
   * Whether this request may be compressed.
   */
  private def mayCompress(request: RequestHeader) = request.method != "HEAD" &&
    request.headers.get(Names.ACCEPT_ENCODING).flatMap(_.split(',').find(_ == "gzip")).isDefined

  /**
   * Whether this response should be compressed.  Responses that may not contain content won't be compressed, nor will
   * responses that already define a content encoding, server sent event responses will not be compressed, and chunked
   * responses won't be compressed.
   */
  private def shouldCompress(header: ResponseHeader) = isAllowedContent(header) &&
    isNotAlreadyCompressed(header) &&
    isNotServerSentEvents(header) &&
    isNotChunked(header)

  /**
   * We don't compress chunked responses because this is often used for comet events, and because we would have to
   * dechunk them first if we did.
   */
  private def isNotChunked(header: ResponseHeader) = !header.headers.get(TRANSFER_ENCODING).exists(_ == CHUNKED)

  /**
   * We don't compress server sent events because these must be pushed immediately, and compressing buffers.
   */
  private def isNotServerSentEvents(header: ResponseHeader) = !header.headers.get(CONTENT_TYPE).exists(_ == MimeTypes.EVENT_STREAM)

  /**
   * Certain response codes are forbidden by the HTTP spec to contain content, but a gzipped response always contains
   * a minimum of 20 bytes, even for empty responses.
   */
  private def isAllowedContent(header: ResponseHeader) = header.status != Status.NO_CONTENT && header.status != Status.NOT_MODIFIED

  /**
   * Of course, we don't want to double compress responses
   */
  private def isNotAlreadyCompressed(header: ResponseHeader) = header.headers.get(Names.CONTENT_ENCODING).isEmpty

  private def setupHeader(header: Map[String, String]): Map[String, String] = {
    header.filterNot(_._1 == Names.CONTENT_LENGTH) + (Names.CONTENT_ENCODING -> "gzip") + (Names.VARY -> Names.ACCEPT_ENCODING)
  }
}

object GzipFilter {
  /** Default threshold before chunking happens is 100kb */
  private val DefaultChunkedThreshold = 102400
  /** The default buffer for gzip chunk size to use, 8kb matches plays default chunking size when streaming */
  private val DefaultChunkSize = 8192
  /** The GZIP header length */
  private val GzipHeaderLength = 10
}