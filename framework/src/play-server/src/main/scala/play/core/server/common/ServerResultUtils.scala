/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.common

import play.api._
import play.api.mvc._
import play.api.http.HeaderNames._
import play.api.libs.iteratee._
import scala.concurrent.{ Future, Promise }

object ServerResultUtils {

  /** Save allocation by caching an empty array */
  private val emptyBytes = new Array[Byte](0)

  sealed trait ResultStreaming
  final case class CannotStream(reason: String, alternativeResult: Result) extends ResultStreaming
  final case class StreamWithClose(enum: Enumerator[Array[Byte]]) extends ResultStreaming
  final case class StreamWithKnownLength(enum: Enumerator[Array[Byte]]) extends ResultStreaming
  final case class StreamWithStrictBody(body: Array[Byte]) extends ResultStreaming
  final case class UseExistingTransferEncoding(transferEncodedEnum: Enumerator[Array[Byte]]) extends ResultStreaming
  final case class PerformChunkedTransferEncoding(enum: Enumerator[Array[Byte]]) extends ResultStreaming

  /**
   * Analyze the Result and determine how best to send it. This may involve looking at
   * headers, buffering the enumerator, etc. The returned value will indicate how to
   * stream the result and will provide an Enumerator or Array with the result body
   * that should be streamed. CannotStream will be returned if the Result cannot be
   * streamed to the given client. This can happen if a result requires Transfer-Encoding
   * but the client uses HTTP 1.0. It can also happen if there is an error in the
   * Result headers.
   */
  def determineResultStreaming(result: Result, isHttp10: Boolean): Future[ResultStreaming] = {

    result match {
      case _ if result.header.headers.exists(_._2 == null) =>
        Future.successful(CannotStream(
          "A header was set to null",
          Results.InternalServerError("")
        ))
      case _ if (result.connection == HttpConnection.Close) =>
        Future.successful(StreamWithClose(result.body))
      case _ if (result.header.headers.contains(TRANSFER_ENCODING)) =>
        if (isHttp10) {
          Future.successful(CannotStream(
            "Chunked response to HTTP/1.0 request",
            Results.HttpVersionNotSupported("The response to this request is chunked and hence requires HTTP 1.1 to be sent, but this is a HTTP 1.0 request.")
          ))
        } else {
          Future.successful(UseExistingTransferEncoding(result.body))
        }
      case _ if (result.header.headers.contains(CONTENT_LENGTH)) =>
        Future.successful(StreamWithKnownLength(result.body))
      case _ =>
        import play.api.libs.iteratee.Execution.Implicits.trampoline
        val bodyReadAhead = readAheadOne(result.body >>> Enumerator.eof)
        bodyReadAhead.map {
          case Left(bodyOption) =>
            val body = bodyOption.getOrElse(emptyBytes)
            StreamWithStrictBody(body)
          case Right(bodyEnum) =>
            if (isHttp10) {
              StreamWithClose(bodyEnum) // HTTP 1.0 doesn't support chunked encoding
            } else {
              PerformChunkedTransferEncoding(bodyEnum)
            }
        }
    }

  }

  /**
   * Start reading an Enumerator and see if it is only zero or one
   * elements long.
   * - If zero-length, return Left(None).
   * - If one-length, return the element in Left(Some(el))
   * - If more than one element long, return Right(enumerator) where
   *   enumerator is an Enumerator that contains *all* the input. Any
   *   already-read elements will still be included in this Enumerator.
   */
  def readAheadOne[A](enum: Enumerator[A]): Future[Either[Option[A], Enumerator[A]]] = {
    import Execution.Implicits.trampoline
    val result = Promise[Either[Option[A], Enumerator[A]]]()
    val it: Iteratee[A, Unit] = for {
      taken <- Iteratee.takeUpTo(1)
      emptyAfterTaken <- Iteratee.isEmpty
      _ <- {
        if (emptyAfterTaken) {
          assert(taken.length <= 1)
          result.success(Left(taken.headOption))
          Done[A, Unit](())
        } else {
          val (remainingIt, remainingEnum) = Concurrent.joined[A]
          result.success(Right(Enumerator.enumerate(taken) >>> remainingEnum))
          remainingIt
        }
      }
    } yield ()
    enum(it)
    result.future
  }

  def cleanFlashCookie(requestHeader: RequestHeader, result: Result): Result = {
    val header = result.header

    val flashCookie = {
      header.headers.get(SET_COOKIE)
        .map(Cookies.decode(_))
        .flatMap(_.find(_.name == Flash.COOKIE_NAME)).orElse {
          Option(requestHeader.flash).filterNot(_.isEmpty).map { _ =>
            Flash.discard.toCookie
          }
        }
    }

    flashCookie.map { newCookie =>
      result.withHeaders(SET_COOKIE -> Cookies.merge(header.headers.get(SET_COOKIE).getOrElse(""), Seq(newCookie)))
    }.getOrElse(result)
  }

  /**
   * Given a map of headers, split it into a sequence of individual headers.
   * Most headers map into a single pair in the new sequence. The exception is
   * the `Set-Cookie` header which we split into a pair for each cookie it
   * contains. This allows us to work around issues with clients that can't
   * handle combined headers. (Also RFC6265 says multiple headers shouldn't
   * be folded together, which Play's API unfortunately  does.)
   */
  def splitHeadersIntoSeq(headers: Map[String, String]): Seq[(String, String)] = {
    headers.to[Seq].flatMap {
      case (SET_COOKIE, value) => {
        val cookieParts: Seq[Cookie] = Cookies.decode(value)
        cookieParts.map { cookiePart =>
          (SET_COOKIE, Cookies.encode(Seq(cookiePart)))
        }
      }
      case (name, value) =>
        Seq((name, value))
    }
  }

}
