/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.gzip

import javax.inject.{ Provider, Inject, Singleton }

import com.typesafe.config.ConfigMemorySize
import play.api.inject.Module
import play.api.{ Environment, PlayConfig, Configuration }
import play.api.libs.iteratee._
import play.api.mvc._
import scala.concurrent.Future
import play.api.mvc.RequestHeader.acceptHeader
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
 */
@Singleton
class GzipFilter @Inject() (config: GzipFilterConfig) extends EssentialFilter {

  import play.api.http.HeaderNames._
  import play.api.http.HttpProtocol._

  def this(gzip: Enumeratee[Array[Byte], Array[Byte]] = Gzip.gzip(8192),
    chunkedThreshold: Int = 102400,
    shouldGzip: (RequestHeader, ResponseHeader) => Boolean = (_, _) => true) = this(GzipFilterConfig(gzip, chunkedThreshold, shouldGzip))

  def apply(next: EssentialAction) = new EssentialAction {
    def apply(request: RequestHeader) = {
      if (mayCompress(request)) {
        next(request).mapM(result => handleResult(request, result))
      } else {
        next(request)
      }
    }
  }

  private def handleResult(request: RequestHeader, result: Result): Future[Result] = {
    if (shouldCompress(result.header) && config.shouldGzip(request, result.header)) {
      // If connection is close, don't bother buffering it, we can send it without a content length
      if (result.connection == HttpConnection.Close) {
        Future.successful(Result(
          header = result.header.copy(headers = setupHeader(result.header.headers)),
          body = result.body &> config.gzip,
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
            case Input.El(data) if count <= GzipFilter.GzipHeaderLength || count + data.length < config.chunkedThreshold =>
              buffer(data :: chunks, count + data.length)
            case Input.El(data) => Done(Left((data :: chunks).reverse))
            case Input.Empty => buffer(chunks, count)
          }
        }

        // Run the enumerator partially (means we get an enumerator that contains the rest of the input)
        Concurrent.runPartial(result.body &> config.gzip, buffer(Nil, 0)).map {
          // We successfully buffered the whole thing, so we have a content length
          case (Right((chunks, contentLength)), empty) =>
            Result(
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
              Result(
                header = result.header.copy(headers = setupHeader(result.header.headers)),
                body = Enumerator.enumerate(chunks) >>> remaining,
                connection = HttpConnection.Close
              )
            } else {
              // Otherwise chunk
              Result(
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
  private def mayCompress(request: RequestHeader) =
    request.method != "HEAD" && gzipIsAcceptedAndPreferredBy(request)

  private def gzipIsAcceptedAndPreferredBy(request: RequestHeader) = {
    val codings = acceptHeader(request.headers, ACCEPT_ENCODING)
    def explicitQValue(coding: String) = codings collectFirst { case (q, c) if c equalsIgnoreCase coding => q }
    def defaultQValue(coding: String) = if (coding == "identity") 0.001d else 0d
    def qvalue(coding: String) = explicitQValue(coding) orElse explicitQValue("*") getOrElse defaultQValue(coding)

    qvalue("gzip") > 0d && qvalue("gzip") >= qvalue("identity")
  }

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
  private def isNotAlreadyCompressed(header: ResponseHeader) = header.headers.get(CONTENT_ENCODING).isEmpty

  private def setupHeader(header: Map[String, String]): Map[String, String] = {
    header.filterNot(_._1 == CONTENT_LENGTH) + (CONTENT_ENCODING -> "gzip") + addToVaryHeader(header, VARY, ACCEPT_ENCODING)
  }

  /**
   * There may be an existing Vary value, which we must add to (comma separated)
   */
  private def addToVaryHeader(existingHeaders: Map[String, String], headerName: String, headerValue: String): (String, String) = {
    existingHeaders.get(headerName) match {
      case None => (headerName, headerValue)
      case Some(existing) => (headerName, s"$existing,$headerValue")
    }
  }
}

object GzipFilter {
  /** The GZIP header length */
  private val GzipHeaderLength = 10
}

/**
 * Configuration for the gzip filter
 *
 * @param gzip The gzip enumeratee to use.
 * @param chunkedThreshold The content length threshold, after which the filter will switch to chunking the result.
 * @param shouldGzip Whether the given request/result should be gzipped.  This can be used, for example, to implement
 *                   black/white lists for gzipping by content type.
 */
case class GzipFilterConfig(gzip: Enumeratee[Array[Byte], Array[Byte]] = Gzip.gzip(8192),
    chunkedThreshold: Int = 102400,
    shouldGzip: (RequestHeader, ResponseHeader) => Boolean = (_, _) => true) {
}

object GzipFilterConfig {
  def fromConfiguration(conf: Configuration) = {
    val config = PlayConfig(conf).get[PlayConfig]("play.filters.gzip")

    GzipFilterConfig(
      gzip = Gzip.gzip(config.get[ConfigMemorySize]("bufferSize").toBytes.toInt),
      chunkedThreshold = config.get[ConfigMemorySize]("chunkedThreshold").toBytes.toInt
    )
  }
}

/**
 * The gzip filter configuration provider.
 */
@Singleton
class GzipFilterConfigProvider @Inject() (config: Configuration) extends Provider[GzipFilterConfig] {
  lazy val get = GzipFilterConfig.fromConfiguration(config)
}

/**
 * The gzip filter module.
 */
class GzipFilterModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[GzipFilterConfig].toProvider[GzipFilterConfigProvider],
    bind[GzipFilter].toSelf
  )
}

/**
 * The gzip filter components.
 */
trait GzipFilterComponents {
  def configuration: Configuration

  lazy val gzipFilterConfig: GzipFilterConfig = GzipFilterConfig.fromConfiguration(configuration)
  lazy val gzipFilter: GzipFilter = new GzipFilter(gzipFilterConfig)
}