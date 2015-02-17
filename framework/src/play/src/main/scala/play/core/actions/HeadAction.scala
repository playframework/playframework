/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package play.core.actions

import play.api.mvc._
import play.api.libs.iteratee._
import play.api.http.{ HttpProtocol, HeaderNames, DefaultWriteables }
import play.api.mvc.Result
import scala.concurrent.Future

import HeaderNames._

/**
 * RFC2616-compatible HEAD implementation: provides a full header set and empty body for a given GET resource
 *
 * @param action Action for the relevant GET path.
 */
class HeadAction(action: EssentialAction) extends EssentialAction with DefaultWriteables with HttpProtocol {

  def apply(requestHeader: RequestHeader): Iteratee[Array[Byte], Result] = {

    val bodyIterator: Iteratee[Array[Byte], Result] = action(requestHeader)

    def createHeadResult(result: Result): Future[Result] = result match {
      // Respond immediately for bodies which have finished evaluating
      case UsesTransferEncoding() | HasContentLength() =>
        val newResult = Result(result.header, Enumerator(Array.emptyByteArray), result.connection)
        Future.successful(newResult)
      // We need to evaluate the body further to determine appropriate headers (Content-Length or Transfer-Encoding)
      case _ =>
        result.body |>>> singleChunkIteratee(result, requestHeader.version)
    }

    import play.api.libs.iteratee.Execution.Implicits.trampoline

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
  def singleChunkIteratee(result: Result, httpVersion: String): Iteratee[Array[Byte], Result] = {
    lazy val resultWithEmptyBody = Result(result.header, Enumerator(Array.emptyByteArray), result.connection)

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
        val contentLength = chunk.map(_.length).getOrElse(0)
        val newResult = resultWithEmptyBody.withHeaders(
          CONTENT_LENGTH -> contentLength.toString
        )

        Done[Array[Byte], Result](newResult)

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

        Done[Array[Byte], Result](newResult)
    }
  }
}

/**
 * Extractor object that determines whether the result uses a transfer encoding
 */
object UsesTransferEncoding {
  def unapply(result: Result): Boolean = result.header.headers.contains(TRANSFER_ENCODING)
}

/**
 * Extractor that determines whether a content-length has been set on a result
 */
object HasContentLength {
  def unapply(result: Result): Boolean = result.header.headers.contains(CONTENT_LENGTH)
}
