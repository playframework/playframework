/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package play.api.controllers

import play.api.mvc._
import play.api.libs.iteratee._
import play.api.http.{ HttpProtocol, HeaderNames, DefaultWriteables }
import play.core.server.netty.NettyResultStreamer.UsesTransferEncoding
import org.jboss.netty.buffer.ChannelBuffers
import scala.Some
import play.api.mvc.SimpleResult
import scala.concurrent.Future

/**
 * RFC2616-compatible HEAD implementation: provides a full header set and empty body for a given GET resource
 *
 * @param handler Action for the relevant GET path.
 */
class HeadAction(handler: Handler) extends EssentialAction with DefaultWriteables with HeaderNames with HttpProtocol {
  def apply(requestHeader: RequestHeader): Iteratee[Array[Byte], SimpleResult] = {
    def bodyIterator: Iteratee[Array[Byte], SimpleResult] = handler.asInstanceOf[EssentialAction](requestHeader)

    def createHeadResult(result: SimpleResult): Future[SimpleResult] = result match {
      // Respond immediately for bodies which have finished evaluating
      case UsesTransferEncoding() | HasContentLength() =>
        val newResult = SimpleResult(result.header, Enumerator(Array.emptyByteArray), result.connection)
        Future.successful(newResult)
      // We need to evaluate the body further to determine appropriate headers (Content-Length or Transfer-Encoding)
      case _ =>
        result.body |>>> singleChunkIteratee(result, requestHeader.version)
    }

    import play.core.Execution.Implicits.internalContext

    bodyIterator.mapM(result =>
      createHeadResult(result)
    )
  }

  /**
   * Creates an Iteratee that will evaluate at most one chunk of a given resource
   * @param result Contains initial result information
   * @param httpVersion HTTP Version from the RequestHeader to ensure proper response headers
   * @return
   */
  def singleChunkIteratee(result: SimpleResult, httpVersion: String): Iteratee[Array[Byte], SimpleResult] = {
    lazy val resultWithEmptyBody = SimpleResult(result.header, Enumerator(Array.emptyByteArray), result.connection)

    def takeUpToOneChunk(chunk: Option[Array[Byte]]): Iteratee[Array[Byte], Either[Array[Byte], Option[Array[Byte]]]] = Cont {
      // We have a second chunk, fail with left
      case in @ Input.El(data) if chunk.isDefined => Done(Left(chunk.get), in)
      // This is the first chunk
      case Input.El(data) => takeUpToOneChunk(Some(data))
      case Input.Empty => takeUpToOneChunk(chunk)
      // We reached EOF, which means we either have one or zero chunks
      case Input.EOF => Done(Right(chunk))
    }

    import play.api.libs.iteratee.Execution.Implicits.trampoline

    takeUpToOneChunk(None).flatMap {
      // Single chunk response
      case Right(chunk) =>
        // Push the chunk into a buffer to measure its length and use that as the correct Content-Length
        // for the head request
        val buffer = chunk.map(ChannelBuffers.wrappedBuffer).getOrElse(ChannelBuffers.EMPTY_BUFFER)

        val newResult = resultWithEmptyBody.withHeaders(
          CONTENT_LENGTH -> buffer.readableBytes().toString
        )

        Done[Array[Byte], SimpleResult](newResult)

      case Left(chunk) =>
        // The body is in multiple chunks.
        val newResult = httpVersion match {
          // HTTP 1.0 doesn't support chunked transfer
          case HTTP_1_0 =>
            resultWithEmptyBody
          case HTTP_1_1 =>
            resultWithEmptyBody.withHeaders(
              TRANSFER_ENCODING -> CHUNKED
            )
        }

        Done[Array[Byte], SimpleResult](newResult)
    }
  }
}

/**
 * Extractor that determines whether a content-length has been set on a result
 */
object HasContentLength extends HeaderNames {
  def unapply(result: SimpleResult): Boolean = result.header.headers.contains(CONTENT_LENGTH)
}
