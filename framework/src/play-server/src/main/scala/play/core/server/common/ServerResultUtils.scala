/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.common

import play.api.Logger
import play.api.mvc._
import play.api.http.{ DefaultHttpErrorHandler, HttpErrorHandler, HttpProtocol, Status }
import play.api.http.HeaderNames._
import play.api.libs.iteratee._
import scala.concurrent.{ Future, Promise }
import scala.util.control.NonFatal

object ServerResultUtils {

  private val logger = Logger(ServerResultUtils.getClass)

  /** Save allocation by caching an empty array */
  private val emptyBytes = new Array[Byte](0)

  /**
   * Used to indicate that a result can't be streamed. Offers an
   * alternative result that can be sent instead.
   */
  final case class InvalidResult(reason: String, alternativeResult: Result)

  /**
   * Indicates the streaming strategy to use for returning the response.
   */
  sealed trait ResultStreaming

  /**
   * Used for responses that may not contain a body, e.g. 204 or 304 responses.
   * The server shouldn't send a body and, since the response cannot have a
   * body, the Content-Length header shouldn't be sent either.
   */
  final case object StreamWithNoBody extends ResultStreaming
  /**
   * Used for responses that have unknown length and should be delimited by
   * the connection closing. This is used for all HTTP 1.0 responses with
   * unknown length, since HTTP 1.0 doesn't support chunked encoding. It can
   * also be used for some HTTP 1.1 responses, if chunked encoding isn't
   * desired for some reason, e.g. see the `Results.feed` method.
   */
  final case class StreamWithClose(enum: Enumerator[Array[Byte]]) extends ResultStreaming
  /**
   * A stream with a known length where the Content-Length header can be
   * set.
   */
  final case class StreamWithKnownLength(enum: Enumerator[Array[Byte]]) extends ResultStreaming
  /**
   * A stream with bytes that are already entirely known. The Content-Length
   * can be sent and an efficient streaming strategy can be used by the server.
   */
  final case class StreamWithStrictBody(body: Array[Byte]) extends ResultStreaming
  /**
   * A stream where the response has already been encoded by the user, e.g. using
   * `Results.chunked`. The server may be able to feed this encoded data directly -
   * or it may need to reverse the encoding before resending it. :(
   */
  final case class UseExistingTransferEncoding(transferEncodedEnum: Enumerator[Array[Byte]]) extends ResultStreaming
  /**
   * A stream where the response should be chunk encoded. This is usually used for
   * an HTTP 1.1 connection where the response has unknown size.
   */
  final case class PerformChunkedTransferEncoding(enum: Enumerator[Array[Byte]]) extends ResultStreaming

  /**
   * The connection header logic to use for the result.
   */
  sealed trait ConnectionHeader {
    def willClose: Boolean
    def header: Option[String]
  }
  /**
   * A `Connection: keep-alive` header should be sent. Used to
   * force an HTTP 1.0 connection to remain open.
   */
  final case object SendKeepAlive extends ConnectionHeader {
    override def willClose = false
    override def header = Some(KEEP_ALIVE)
  }
  /**
   * A `Connection: close` header should be sent. Used to
   * force an HTTP 1.1 connection to close.
   */
  final case object SendClose extends ConnectionHeader {
    override def willClose = true
    override def header = Some(CLOSE)
  }
  /**
   * No `Connection` header should be sent. Used on an HTTP 1.0
   * connection where the default behavior is to close the connection.
   */
  final case object DefaultClose extends ConnectionHeader {
    override def willClose = true
    override def header = None
  }
  /**
   * No `Connection` header should be sent. Used on an HTTP 1.1
   * connection where the default behavior is to keep the connection
   * open.
   */
  final case object DefaultKeepAlive extends ConnectionHeader {
    override def willClose = false
    override def header = None
  }

  // Values for the Connection header
  private val KEEP_ALIVE = "keep-alive"
  private val CLOSE = "close"

  /**
   * Analyze the Result and determine how best to send it. This may involve looking at
   * headers, buffering the enumerator, etc. The returned value will indicate how to
   * stream the result and will provide an Enumerator or Array with the result body
   * that should be streamed.
   *
   * CannotStream will be returned if the Result cannot be
   * streamed to the given client. This can happen if a result requires Transfer-Encoding
   * but the client uses HTTP 1.0. It can also happen if there is an error in the
   * Result headers.
   *
   * The ConnectionHeader returned for a successful result will indicate how the
   * header should be set in the response header.
   */
  def determineResultStreaming(
    requestHeader: RequestHeader,
    result: Result,
    errorHandler: HttpErrorHandler): Future[Either[InvalidResult, (ResultStreaming, ConnectionHeader)]] = {

    // The protocol version will affect how we stream the result and
    // the value of the Connection header that we set
    val isHttp10 = requestHeader.version == HttpProtocol.HTTP_1_0

    // Work out whether we should close the connection after our response
    val needsClose: Boolean = {
      // Has the user has requested that the connection be closed?
      val forceClose: Boolean = result.connection == HttpConnection.Close
      // Did the request we receive indicate whether the connection should be closed?
      def defaultClose: Boolean = {
        val requestConnectionHeader: Option[String] = requestHeader.headers.get(CONNECTION)
        def requestConnectionHeaderMatches(value: String): Boolean = requestConnectionHeader.exists(_.equalsIgnoreCase(value))
        (isHttp10 && !requestConnectionHeaderMatches(KEEP_ALIVE)) || (!isHttp10 && requestConnectionHeaderMatches(CLOSE))
      }
      forceClose || defaultClose
    }

    // Get a Connection header to use that will close the connection or keep it alive,
    // depending on what we need to do.
    val connection: ConnectionHeader = {
      if (needsClose) {
        if (isHttp10) DefaultClose else SendClose
      } else {
        if (isHttp10) SendKeepAlive else DefaultKeepAlive
      }
    }

    // Helpers for creating return values for this method
    def invalid(reason: String, statusCode: Int): Future[Left[InvalidResult, Nothing]] = {
      import play.api.libs.iteratee.Execution.Implicits.trampoline
      // Use the HttpErrorHandler to generate an error response. Then patch the status code
      // since the HttpErrorHandler won't generate non-500 errors. Finally wrap all of this
      // in an InvalidResult so that the caller can detect the error and do any extra handling
      // that is needed.
      val exception = new ServerResultException(s"Invalid result: $reason", result, null)
      errorHandler.onServerError(requestHeader, exception).map { errorResult: Result =>
        val patchedErrorResult: Result = errorResult.copy(header = errorResult.header.copy(status = statusCode))
        Left(InvalidResult(reason, patchedErrorResult))
      }
    }
    def valid(streaming: ResultStreaming, connection: ConnectionHeader): Future[Right[Nothing, (ResultStreaming, ConnectionHeader)]] = {
      Future.successful(Right((streaming, connection)))
    }

    result match {

      // Check if the header has invalid values
      case _ if result.header.headers.exists(_._2 == null) =>
        invalid(
          "A header was set to null",
          Status.INTERNAL_SERVER_ERROR
        )

      // The HTTP spec requires that some responses don't have a body
      case _ if result.header.status == 204 || result.header.status == 304 =>
        valid(StreamWithNoBody, connection)

      // Check if the user has already transfer encoded the response
      case _ if (result.header.headers.contains(TRANSFER_ENCODING)) =>
        if (isHttp10) {
          invalid(
            "Chunked response to HTTP/1.0 request",
            Status.HTTP_VERSION_NOT_SUPPORTED
          )
        } else {
          valid(UseExistingTransferEncoding(result.body), connection)
        }

      // Check if the result has a known length
      case _ if (result.header.headers.contains(CONTENT_LENGTH)) =>
        valid(StreamWithKnownLength(result.body), connection)

      // Check if the connection is required to close (if so we don't need to
      // worry about chunking the response)
      case _ if connection.willClose =>
        valid(StreamWithClose(result.body), connection)

      // Read ahead one element and see if we can send the body
      // in one element, or if we need to chunk it, or if we need
      // to stream it and then close the connection
      case _ =>
        import play.api.libs.iteratee.Execution.Implicits.trampoline
        val bodyReadAhead = readAheadOne(result.body >>> Enumerator.eof)
        bodyReadAhead.map {
          case Left(bodyOption) =>
            val body = bodyOption.getOrElse(emptyBytes)
            Right((StreamWithStrictBody(body), connection))
          case Right(bodyEnum) =>
            // Use chunked encoding for HTTP 1.1. For HTTP 1.0
            // delimit the end of the result by closing the
            // connection.
            if (isHttp10) {
              Right((StreamWithClose(bodyEnum), DefaultClose))
            } else {
              Right((PerformChunkedTransferEncoding(bodyEnum), connection))
            }
        }
    }

  }

  /**
   * Handles result conversion in a safe way.
   *
   * 1. Tries to convert the `Result`.
   * 2. If there's an error, calls the `HttpErrorHandler` to get a new
   *    `Result`, then converts that.
   * 3. If there's an error with *that* `Result`, uses the
   *    `DefaultHttpErrorHandler` to get another `Result`, then converts
   *    that.
   * 4. Hopefully there are no more errors. :)
   * 5. If calling an `HttpErrorHandler` throws an exception, then a
   *    fallback response is returned, without an conversion.
   */
  def resultConversionWithErrorHandling[R](
    requestHeader: RequestHeader,
    result: Result,
    errorHandler: HttpErrorHandler)(resultConverter: Result => Future[R])(fallbackResponse: => R): Future[R] = {

    import play.api.libs.iteratee.Execution.Implicits.trampoline

    def handleConversionError(conversionError: Throwable): Future[R] = {
      try {
        // Log some information about the error
        if (logger.isErrorEnabled) {
          val prettyHeaders = result.header.headers.map { case (name, value) => s"<$name>: <$value>" }.mkString("[", ", ", "]")
          val msg = s"Exception occurred while converting Result with headers $prettyHeaders. Calling HttpErrorHandler to get alternative Result."
          logger.error(msg, conversionError)
        }

        // Call the HttpErrorHandler to generate an alternative error
        errorHandler.onServerError(
          requestHeader,
          new ServerResultException("Error converting Play Result for server backend", result, conversionError)
        ).flatMap { errorResult =>
            // Convert errorResult using normal conversion logic. This time use
            // the DefaultErrorHandler if there are any problems, e.g. if the
            // current HttpErrorHandler returns an invalid Result.
            resultConversionWithErrorHandling(requestHeader, errorResult, DefaultHttpErrorHandler)(resultConverter)(fallbackResponse)
          }
      } catch {
        case NonFatal(onErrorError) =>
          // Conservatively handle exceptions thrown by HttpErrorHandlers by
          // returning a fallback response.
          logger.error("Error occurred during error handling. Original error: ", conversionError)
          logger.error("Error occurred during error handling. Error handling error: ", onErrorError)
          Future.successful(fallbackResponse)
      }
    }

    try {
      // Try to convert the result
      resultConverter(result).recoverWith { case t => handleConversionError(t) }
    } catch {
      case NonFatal(e) => handleConversionError(e)
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

  /**
   * Update the result's Set-Cookie header so that it removes any Flash cookies we received
   * in the incoming request.
   */
  def cleanFlashCookie(requestHeader: RequestHeader, result: Result): Result = {
    val optResultFlashCookies: Option[_] = result.header.headers.get(SET_COOKIE).flatMap { setCookieValue: String =>
      Cookies.decodeSetCookieHeader(setCookieValue).find(_.name == Flash.COOKIE_NAME)
    }

    if (optResultFlashCookies.isDefined) {
      // We're already setting a flash cookie in the result, just pass that
      // through unchanged
      result
    } else {
      val requestFlash: Flash = requestHeader.flash
      if (requestFlash.isEmpty) {
        // Neither incoming nor outgoing flash cookies; nothing to do
        result
      } else {
        // We got incoming flash cookies, but there are no outgoing flash cookies,
        // so we need to clear the cookies for the next request
        result.discardingCookies(Flash.discard)
      }
    }
  }

  /**
   * Given a map of headers, split it into a sequence of individual headers.
   * Most headers map into a single pair in the new sequence. The exception is
   * the `Set-Cookie` header which we split into a pair for each cookie it
   * contains. This allows us to work around issues with clients that can't
   * handle combined headers. (Also RFC6265 says multiple headers shouldn't
   * be folded together, which Play's API unfortunately  does.)
   */
  def splitSetCookieHeaders(headers: Map[String, String]): Iterable[(String, String)] = {
    if (headers.contains(SET_COOKIE)) {
      // Rewrite the headers with Set-Cookie split into separate headers
      headers.to[Seq].flatMap {
        case (SET_COOKIE, value) =>
          val cookieParts = Cookies.SetCookieHeaderSeparatorRegex.split(value)
          cookieParts.map { cookiePart =>
            SET_COOKIE -> cookiePart
          }
        case (name, value) =>
          Seq((name, value))
      }
    } else {
      // No Set-Cookie header so we can just use the headers as they are
      headers
    }
  }
}
