/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.encoding

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.stream.FlowShape
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.util.ByteString
import play.api.http._
import play.api.mvc._
import play.api.mvc.RequestHeader.acceptHeader

/**
 * A filter that encodes response bodies with a supplied stream transformation.
 *
 * The response is encoded when the request accepts the configured encoding and the response is suitable for encoding.
 * Responses to HEAD requests, responses without content, responses that already have a content encoding, and responses
 * at or below the configured threshold are not encoded.
 *
 * @param encodingName The content coding token used in the Accept-Encoding and Content-Encoding headers.
 * @param createFlow Creates a new flow that encodes a response body.
 * @param shouldTranscode Whether the given request and result should be encoded.
 * @param chunkedThreshold The content length threshold after which the filter switches to chunked encoding.
 * @param threshold The response body size threshold below which responses are not encoded.
 */
class ContentEncodingFilter(
    encodingName: String,
    createFlow: () => Flow[ByteString, ByteString, ?],
    shouldTranscode: (RequestHeader, Result) => Boolean = (_, _) => true,
    chunkedThreshold: Int = 102400,
    threshold: Int = 0
)(implicit mat: Materializer)
    extends EssentialFilter {
  import play.api.http.HeaderNames._

  def apply(next: EssentialAction): EssentialAction = new EssentialAction {
    implicit val ec: ExecutionContext = mat.executionContext

    def apply(request: RequestHeader) = {
      if (mayEncode(request)) {
        next(request).mapFuture(result => handleResult(request, result))
      } else {
        next(request)
      }
    }
  }

  private def handleResult(request: RequestHeader, result: Result): Future[Result] = {
    implicit val ec: ExecutionContext = mat.executionContext
    if (shouldEncode(result) && shouldTranscode(request, result)) {
      val header = result.header.copy(headers = setupHeader(result.header))

      result.body match {
        case HttpEntity.Strict(data, contentType) =>
          encodeStrictEntity(Source.single(data), contentType)
            .map(entity => result.copy(header = header, body = entity))

        case entity @ HttpEntity.Streamed(_, Some(contentLength), contentType) if contentLength <= chunkedThreshold =>
          // It's below the chunked threshold, so buffer then encode and send.
          encodeStrictEntity(entity.data, contentType)
            .map(strictEntity => result.copy(header = header, body = strictEntity))

        case HttpEntity.Streamed(data, _, contentType) if request.version == HttpProtocol.HTTP_1_0 =>
          // HTTP 1.0 cannot use chunked encoding, so use a close-delimited body without a content length.
          val encoded = data.via(createFlow())
          Future.successful(result.copy(header = header, body = HttpEntity.Streamed(encoded, None, contentType)))

        case HttpEntity.Streamed(data, _, contentType) =>
          // It's above the chunked threshold, so stream the encoded body as chunks.
          val encoded = data.via(createFlow()).map(d => HttpChunk.Chunk(d))
          Future.successful(result.copy(header = header, body = HttpEntity.Chunked(encoded, contentType)))

        case HttpEntity.Chunked(chunks, contentType) =>
          val encodingFlow = Flow.fromGraph(GraphDSL.create[FlowShape[HttpChunk, HttpChunk]]() { implicit builder =>
            import GraphDSL.Implicits._

            val extractChunks   = Flow[HttpChunk].collect { case HttpChunk.Chunk(data) => data }
            val createChunks    = Flow[ByteString].map[HttpChunk](HttpChunk.Chunk.apply)
            val filterLastChunk = Flow[HttpChunk]
              .filter(_.isInstanceOf[HttpChunk.LastChunk])
              // Concat does not demand the last chunk until the encoding flow completes. Buffering here lets the
              // broadcast start while preserving the last chunk and its trailers.
              .buffer(1, OverflowStrategy.backpressure)

            val broadcast = builder.add(Broadcast[HttpChunk](2))
            val concat    = builder.add(Concat[HttpChunk]())

            broadcast.out(0) ~> extractChunks ~> createFlow() ~> createChunks ~> concat.in(0)
            broadcast.out(1) ~> filterLastChunk ~> concat.in(1)

            new FlowShape(broadcast.in, concat.out)
          })

          Future.successful(
            result.copy(header = header, body = HttpEntity.Chunked(chunks.via(encodingFlow), contentType))
          )
      }
    } else {
      Future.successful(result)
    }
  }

  private def encodeStrictEntity(source: Source[ByteString, Any], contentType: Option[String])(
      implicit ec: ExecutionContext
  ) = {
    val encoded = source.via(createFlow()).runFold(ByteString.empty)(_ ++ _)
    encoded.map(data => HttpEntity.Strict(data, contentType))
  }

  private def mayEncode(request: RequestHeader) =
    request.method != "HEAD" && encodingIsAcceptedAndPreferredBy(request)

  private def encodingIsAcceptedAndPreferredBy(request: RequestHeader) = {
    val codings                        = acceptHeader(request.headers, ACCEPT_ENCODING)
    def explicitQValue(coding: String) = codings.collectFirst { case (q, c) if c.equalsIgnoreCase(coding) => q }
    def defaultQValue(coding: String)  = if (coding == "identity") 0.001d else 0d
    def qvalue(coding: String)         = explicitQValue(coding).orElse(explicitQValue("*")).getOrElse(defaultQValue(coding))

    qvalue(encodingName) > 0d && qvalue(encodingName) >= qvalue("identity")
  }

  private def shouldEncode(result: Result) =
    isAllowedContent(result.header) &&
      isNotAlreadyEncoded(result.header) &&
      !result.body.isKnownEmpty &&
      result.body.contentLength.forall(_ > threshold)

  private def isAllowedContent(header: ResponseHeader) =
    header.status != Status.NO_CONTENT && header.status != Status.NOT_MODIFIED

  private def isNotAlreadyEncoded(header: ResponseHeader) = header.headers.get(CONTENT_ENCODING).isEmpty

  private def setupHeader(rh: ResponseHeader): Map[String, String] =
    rh.headers + (CONTENT_ENCODING -> encodingName) + rh.varyWith(ACCEPT_ENCODING)
}
