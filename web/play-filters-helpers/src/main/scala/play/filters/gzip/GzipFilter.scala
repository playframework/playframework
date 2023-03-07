/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.gzip

import java.util.function.BiFunction
import java.util.zip.Deflater
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.jdk.FunctionConverters._

import akka.stream.scaladsl._
import akka.stream.FlowShape
import akka.stream.Materializer
import akka.stream.OverflowStrategy
import akka.util.ByteString
import com.typesafe.config.ConfigMemorySize
import play.api.http._
import play.api.inject._
import play.api.libs.streams.GzipFlow
import play.api.mvc._
import play.api.mvc.RequestHeader.acceptHeader
import play.api.Configuration
import play.api.Logger

/**
 * A gzip filter.
 *
 * This filter may gzip the responses for any requests that aren't HEAD requests and specify an accept encoding of gzip.
 *
 * It won't gzip under the following conditions:
 *
 * - The response code is 204 or 304 (these codes MUST NOT contain a body, and an empty gzipped response is 20 bytes
 * long)
 * - The response already defines a Content-Encoding header
 * - The size of the response body is equal or smaller than a given threshold. If the body size cannot be determined,
 *   then it is assumed the response is over the threshold
 * - A custom shouldGzip function is supplied and it returns false
 *
 * Since gzipping changes the content length of the response, this filter may do some buffering - it will buffer any
 * streamed responses that define a content length less than the configured chunked threshold.  Responses that are
 * greater in length, or that don't define a content length, will not be buffered, but will be sent as chunked
 * responses.
 */
@Singleton
class GzipFilter @Inject() (config: GzipFilterConfig)(implicit mat: Materializer) extends EssentialFilter {
  import play.api.http.HeaderNames._

  def this(
      bufferSize: Int = 8192,
      chunkedThreshold: Int = 102400,
      threshold: Int = 0,
      shouldGzip: (RequestHeader, Result) => Boolean = (_, _) => true,
      compressionLevel: Int = Deflater.DEFAULT_COMPRESSION
  )(implicit mat: Materializer) =
    this(GzipFilterConfig(bufferSize, chunkedThreshold, threshold, shouldGzip, compressionLevel))

  def apply(next: EssentialAction) = new EssentialAction {
    implicit val ec: ExecutionContext = mat.executionContext
    def apply(request: RequestHeader) = {
      if (mayCompress(request)) {
        next(request).mapFuture(result => handleResult(request, result))
      } else {
        next(request)
      }
    }
  }

  private def createGzipFlow: Flow[ByteString, ByteString, _] =
    GzipFlow.gzip(config.bufferSize, config.compressionLevel)

  private def handleResult(request: RequestHeader, result: Result): Future[Result] = {
    implicit val ec = mat.executionContext
    if (shouldCompress(result) && config.shouldGzip(request, result)) {
      val header = result.header.copy(headers = setupHeader(result.header))

      result.body match {
        case HttpEntity.Strict(data, contentType) =>
          compressStrictEntity(Source.single(data), contentType)
            .map(entity => result.copy(header = header, body = entity))

        case entity @ HttpEntity.Streamed(_, Some(contentLength), contentType)
            if contentLength <= config.chunkedThreshold =>
          // It's below the chunked threshold, so buffer then compress and send
          compressStrictEntity(entity.data, contentType)
            .map(strictEntity => result.copy(header = header, body = strictEntity))

        case HttpEntity.Streamed(data, _, contentType) if request.version == HttpProtocol.HTTP_1_0 =>
          // It's above the chunked threshold, but we can't chunk it because we're using HTTP 1.0.
          // Instead, we use a close delimited body (ie, regular body with no content length)
          val gzipped = data.via(createGzipFlow)
          Future.successful(
            result.copy(header = header, body = HttpEntity.Streamed(gzipped, None, contentType))
          )

        case HttpEntity.Streamed(data, _, contentType) =>
          // It's above the chunked threshold, compress through the gzip flow, and send as chunked
          val gzipped = data.via(createGzipFlow).map(d => HttpChunk.Chunk(d))
          Future.successful(
            result.copy(header = header, body = HttpEntity.Chunked(gzipped, contentType))
          )

        case HttpEntity.Chunked(chunks, contentType) =>
          val gzipFlow = Flow.fromGraph(GraphDSL.create[FlowShape[HttpChunk, HttpChunk]]() { implicit builder =>
            import GraphDSL.Implicits._

            val extractChunks = Flow[HttpChunk].collect { case HttpChunk.Chunk(data) => data }
            val createChunks  = Flow[ByteString].map[HttpChunk](HttpChunk.Chunk.apply)
            val filterLastChunk = Flow[HttpChunk]
              .filter(_.isInstanceOf[HttpChunk.LastChunk])
              // Since we're doing a merge by concatenating, the filter last chunk won't receive demand until the gzip
              // flow is finished. But the broadcast won't start broadcasting until both flows start demanding. So we
              // put a buffer of one in to ensure the filter last chunk flow demands from the broadcast.
              .buffer(1, OverflowStrategy.backpressure)

            val broadcast = builder.add(Broadcast[HttpChunk](2))
            val concat    = builder.add(Concat[HttpChunk]())

            // Broadcast the stream through two separate flows, one that collects chunks and turns them into
            // ByteStrings, sends those ByteStrings through the Gzip flow, and then turns them back into chunks,
            // the other that just allows the last chunk through. Then concat those two flows together.
            broadcast.out(0) ~> extractChunks ~> createGzipFlow ~> createChunks ~> concat.in(0)
            broadcast.out(1) ~> filterLastChunk ~> concat.in(1)

            new FlowShape(broadcast.in, concat.out)
          })

          Future.successful(
            result.copy(header = header, body = HttpEntity.Chunked(chunks.via(gzipFlow), contentType))
          )
      }
    } else {
      Future.successful(result)
    }
  }

  private def compressStrictEntity(source: Source[ByteString, Any], contentType: Option[String])(
      implicit ec: ExecutionContext
  ) = {
    val compressed = source.via(createGzipFlow).runFold(ByteString.empty)(_ ++ _)
    compressed.map(data => HttpEntity.Strict(data, contentType))
  }

  /**
   * Whether this request may be compressed.
   */
  private def mayCompress(request: RequestHeader) =
    request.method != "HEAD" && gzipIsAcceptedAndPreferredBy(request)

  private def gzipIsAcceptedAndPreferredBy(request: RequestHeader) = {
    val codings                        = acceptHeader(request.headers, ACCEPT_ENCODING)
    def explicitQValue(coding: String) = codings.collectFirst { case (q, c) if c.equalsIgnoreCase(coding) => q }
    def defaultQValue(coding: String)  = if (coding == "identity") 0.001d else 0d
    def qvalue(coding: String)         = explicitQValue(coding).orElse(explicitQValue("*")).getOrElse(defaultQValue(coding))

    qvalue("gzip") > 0d && qvalue("gzip") >= qvalue("identity")
  }

  /**
   * Whether this response should be compressed.  Responses that may not contain content won't be compressed, nor will
   * responses that already define a content encoding.  Empty responses also shouldn't be compressed, as they will
   * actually always get bigger.  Also responses whose body size are equal or lower than the given byte threshold won't
   * be compressed, because it's assumed they end up being bigger than the original body.
   */
  private def shouldCompress(result: Result) =
    isAllowedContent(result.header) &&
      isNotAlreadyCompressed(result.header) &&
      !result.body.isKnownEmpty &&
      result.body.contentLength.forall(_ > config.threshold)

  /**
   * Certain response codes are forbidden by the HTTP spec to contain content, but a gzipped response always contains
   * a minimum of 20 bytes, even for empty responses.
   */
  private def isAllowedContent(header: ResponseHeader) =
    header.status != Status.NO_CONTENT && header.status != Status.NOT_MODIFIED

  /**
   * Of course, we don't want to double compress responses
   */
  private def isNotAlreadyCompressed(header: ResponseHeader) = header.headers.get(CONTENT_ENCODING).isEmpty

  private def setupHeader(rh: ResponseHeader): Map[String, String] = {
    rh.headers + (CONTENT_ENCODING -> "gzip") + rh.varyWith(ACCEPT_ENCODING)
  }
}

/**
 * Configuration for the gzip filter
 *
 * @param bufferSize The size of the buffer to use for gzipping.
 * @param chunkedThreshold The content length threshold, after which the filter will switch to chunking the result.
 * @param threshold The byte threshold for the response body size which controls if a response should be gzipped.
 * @param shouldGzip Whether the given request/result should be gzipped.  This can be used, for example, to implement
 *                   black/white lists for gzipping by content type.
 * @param compressionLevel Compression level to use for the underlying [[java.util.zip.Deflater]] instance.
 */
case class GzipFilterConfig(
    bufferSize: Int = 8192,
    chunkedThreshold: Int = 102400,
    threshold: Int = 0,
    shouldGzip: (RequestHeader, Result) => Boolean = (_, _) => true,
    compressionLevel: Int = Deflater.DEFAULT_COMPRESSION
) {
  // alternate constructor and builder methods for Java
  def this() = this(shouldGzip = (_, _) => true)

  def withShouldGzip(shouldGzip: (RequestHeader, Result) => Boolean): GzipFilterConfig = copy(shouldGzip = shouldGzip)

  def withShouldGzip(shouldGzip: BiFunction[play.mvc.Http.RequestHeader, play.mvc.Result, Boolean]): GzipFilterConfig =
    withShouldGzip((req: RequestHeader, res: Result) => shouldGzip.asScala(req.asJava, res.asJava))

  def withChunkedThreshold(threshold: Int): GzipFilterConfig = copy(chunkedThreshold = threshold)

  def withThreshold(threshold: Int): GzipFilterConfig = copy(threshold = threshold)

  def withBufferSize(size: Int): GzipFilterConfig = copy(bufferSize = size)

  def withCompressionLevel(level: Int): GzipFilterConfig = copy(compressionLevel = level)
}

object GzipFilterConfig {
  private val logger = Logger(this.getClass)

  def fromConfiguration(conf: Configuration): GzipFilterConfig = {
    def parseConfigMediaTypes(config: Configuration, key: String): Seq[MediaType] = {
      val mediaTypes = config.get[Seq[String]](key).flatMap {
        case "*" =>
          // "*" wildcards are accepted for backwards compatibility with when "MediaRange" was used for parsing,
          // but they are not part of the MediaType spec as defined in RFC2616.
          logger.warn(
            "Support for '*' wildcards may be removed in future versions of play," +
              " as they don't conform to the specification for MediaType strings. Use */* instead."
          )
          Some(MediaType("*", "*", Seq.empty))

        case MediaType.parse(mediaType) => Some(mediaType)

        case invalid =>
          logger.error(s"Failed to parse the configured MediaType mask '$invalid'")
          None
      }

      mediaTypes.foreach {
        case MediaType("*", "*", _) =>
          logger.warn(
            "Wildcard MediaTypes don't make much sense in a whitelist (too permissive) or " +
              "blacklist (too restrictive), and are not recommended. "
          )
        case _ => () // the configured MediaType mask is valid
      }

      mediaTypes
    }

    def matches(outgoing: MediaType, mask: MediaType): Boolean = {
      def capturedByMask(value: String, mask: String): Boolean = {
        mask == "*" || value.equalsIgnoreCase(mask)
      }

      capturedByMask(outgoing.mediaType, mask.mediaType) && capturedByMask(outgoing.mediaSubType, mask.mediaSubType)
    }

    val config    = conf.get[Configuration]("play.filters.gzip")
    val whiteList = parseConfigMediaTypes(config, "contentType.whiteList")
    val blackList = parseConfigMediaTypes(config, "contentType.blackList")

    GzipFilterConfig(
      bufferSize = config.get[ConfigMemorySize]("bufferSize").toBytes.toInt,
      chunkedThreshold = config.get[ConfigMemorySize]("chunkedThreshold").toBytes.toInt,
      threshold = config.get[ConfigMemorySize]("threshold").toBytes.toInt,
      shouldGzip = (_, res) =>
        if (whiteList.isEmpty) {
          if (blackList.isEmpty) {
            true // default case, both whitelist and blacklist are empty so we gzip it.
          } else {
            // The blacklist is defined, so we gzip the result if it's not blacklisted.
            res.body.contentType match {
              case Some(MediaType.parse(outgoing)) => blackList.forall(mask => !matches(outgoing, mask))
              case _                               => true // Fail open (to gziping), since blacklists have a tendency to fail open.
            }
          }
        } else {
          // The whitelist is defined. We gzip the result if there is a matching whitelist entry.
          res.body.contentType match {
            case Some(MediaType.parse(outgoing)) => whiteList.exists(mask => matches(outgoing, mask))
            case _                               => false // Fail closed (to not gziping), since whitelists are intentionally strict.
          }
        },
      compressionLevel = config.get[Int]("compressionLevel")
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
class GzipFilterModule
    extends SimpleModule(
      bind[GzipFilterConfig].toProvider[GzipFilterConfigProvider],
      bind[GzipFilter].toSelf
    )

/**
 * The gzip filter components.
 */
trait GzipFilterComponents {
  def configuration: Configuration
  def materializer: Materializer

  lazy val gzipFilterConfig: GzipFilterConfig = GzipFilterConfig.fromConfiguration(configuration)
  lazy val gzipFilter: GzipFilter             = new GzipFilter(gzipFilterConfig)(materializer)
}
