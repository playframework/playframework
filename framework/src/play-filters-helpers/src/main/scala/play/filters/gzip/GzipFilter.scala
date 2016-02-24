/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.gzip

import java.util.function.BiFunction
import java.util.zip.GZIPOutputStream
import javax.inject.{ Provider, Inject, Singleton }

import akka.stream.{ OverflowStrategy, FlowShape, Materializer }
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.config.ConfigMemorySize
import play.api.inject.Module
import play.api.libs.streams.GzipFlow
import play.api.{ Environment, PlayConfig, Configuration }
import play.api.mvc._
import play.core.j
import scala.concurrent.Future
import play.api.mvc.RequestHeader.acceptHeader
import play.api.http.{ HttpChunk, HttpEntity, Status }
import play.api.libs.concurrent.Execution.Implicits._
import scala.compat.java8.FunctionConverters._

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

  def this(bufferSize: Int = 8192, chunkedThreshold: Int = 102400,
    shouldGzip: (RequestHeader, Result) => Boolean = (_, _) => true)(implicit mat: Materializer) =
    this(GzipFilterConfig(bufferSize, chunkedThreshold, shouldGzip))

  def apply(next: EssentialAction) = new EssentialAction {
    def apply(request: RequestHeader) = {
      if (mayCompress(request)) {
        next(request).mapFuture(result => handleResult(request, result))
      } else {
        next(request)
      }
    }
  }

  private def handleResult(request: RequestHeader, result: Result): Future[Result] = {
    if (shouldCompress(result) && config.shouldGzip(request, result)) {
      val header = result.header.copy(headers = setupHeader(result.header.headers))

      result.body match {

        case HttpEntity.Strict(data, contentType) =>
          Future.successful(Result(header, compressStrictEntity(data, contentType)))

        case entity @ HttpEntity.Streamed(_, Some(contentLength), contentType) if contentLength <= config.chunkedThreshold =>
          // It's below the chunked threshold, so buffer then compress and send
          entity.consumeData.map { data =>
            Result(header, compressStrictEntity(data, contentType))
          }

        case HttpEntity.Streamed(data, _, contentType) =>
          // It's above the chunked threshold, compress through the gzip flow, and send as chunked
          val gzipped = data via GzipFlow.gzip(config.bufferSize) map (d => HttpChunk.Chunk(d))
          Future.successful(Result(header, HttpEntity.Chunked(gzipped, contentType)))

        case HttpEntity.Chunked(chunks, contentType) =>
          val gzipFlow = Flow.fromGraph(GraphDSL.create[FlowShape[HttpChunk, HttpChunk]]() { implicit builder =>
            import GraphDSL.Implicits._

            val extractChunks = Flow[HttpChunk] collect { case HttpChunk.Chunk(data) => data }
            val createChunks = Flow[ByteString].map[HttpChunk](HttpChunk.Chunk.apply)
            val filterLastChunk = Flow[HttpChunk]
              .filter(_.isInstanceOf[HttpChunk.LastChunk])
              // Since we're doing a merge by concatenating, the filter last chunk won't receive demand until the gzip
              // flow is finished. But the broadcast won't start broadcasting until both flows start demanding. So we
              // put a buffer of one in to ensure the filter last chunk flow demands from the broadcast.
              .buffer(1, OverflowStrategy.backpressure)

            val broadcast = builder.add(Broadcast[HttpChunk](2))
            val concat = builder.add(Concat[HttpChunk]())

            // Broadcast the stream through two separate flows, one that collects chunks and turns them into
            // ByteStrings, sends those ByteStrings through the Gzip flow, and then turns them back into chunks,
            // the other that just allows the last chunk through. Then concat those two flows together.
            broadcast.out(0) ~> extractChunks ~> GzipFlow.gzip(config.bufferSize) ~> createChunks ~> concat.in(0)
            broadcast.out(1) ~> filterLastChunk ~> concat.in(1)

            new FlowShape(broadcast.in, concat.out)
          })

          Future.successful(Result(header, HttpEntity.Chunked(chunks via gzipFlow, contentType)))
      }
    } else {
      Future.successful(result)
    }
  }

  private def compressStrictEntity(data: ByteString, contentType: Option[String]) = {
    val builder = ByteString.newBuilder
    val gzipOs = new GZIPOutputStream(builder.asOutputStream, config.bufferSize, true)
    gzipOs.write(data.toArray)
    gzipOs.close()
    HttpEntity.Strict(builder.result(), contentType)
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
   * responses that already define a content encoding.  Empty responses also shouldn't be compressed, as they will
   * actually always get bigger.
   */
  private def shouldCompress(result: Result) = isAllowedContent(result.header) &&
    isNotAlreadyCompressed(result.header) &&
    !result.body.isKnownEmpty

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
    header + (CONTENT_ENCODING -> "gzip") + addToVaryHeader(header, VARY, ACCEPT_ENCODING)
  }

  /**
   * There may be an existing Vary value, which we must add to (comma separated)
   */
  private def addToVaryHeader(existingHeaders: Map[String, String], headerName: String, headerValue: String): (String, String) = {
    existingHeaders.get(headerName) match {
      case None => (headerName, headerValue)
      case Some(existing) if existing.split(",").exists(_.trim.equalsIgnoreCase(headerValue)) => (headerName, existing)
      case Some(existing) => (headerName, s"$existing,$headerValue")
    }
  }
}

/**
 * Configuration for the gzip filter
 *
 * @param bufferSize The size of the buffer to use for gzipping.
 * @param chunkedThreshold The content length threshold, after which the filter will switch to chunking the result.
 * @param shouldGzip Whether the given request/result should be gzipped.  This can be used, for example, to implement
 *                   black/white lists for gzipping by content type.
 */
case class GzipFilterConfig(bufferSize: Int = 8192,
    chunkedThreshold: Int = 102400,
    shouldGzip: (RequestHeader, Result) => Boolean = (_, _) => true) {

  // alternate constructor and builder methods for Java
  def this() = this(shouldGzip = (_, _) => true)

  def withShouldGzip(shouldGzip: (RequestHeader, Result) => Boolean): GzipFilterConfig = copy(shouldGzip = shouldGzip)

  def withShouldGzip(shouldGzip: BiFunction[play.mvc.Http.RequestHeader, play.mvc.Result, Boolean]): GzipFilterConfig =
    withShouldGzip((req, res) => shouldGzip.asScala(new j.RequestHeaderImpl(req), res.asJava))

  def withChunkedThreshold(threshold: Int): GzipFilterConfig = copy(chunkedThreshold = threshold)

  def withBufferSize(size: Int): GzipFilterConfig = copy(bufferSize = size)
}

object GzipFilterConfig {

  def fromConfiguration(conf: Configuration) = {
    val config = PlayConfig(conf).get[PlayConfig]("play.filters.gzip")

    GzipFilterConfig(
      bufferSize = config.get[ConfigMemorySize]("bufferSize").toBytes.toInt,
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
  def materializer: Materializer

  lazy val gzipFilterConfig: GzipFilterConfig = GzipFilterConfig.fromConfiguration(configuration)
  lazy val gzipFilter: GzipFilter = new GzipFilter(gzipFilterConfig)(materializer)
}
