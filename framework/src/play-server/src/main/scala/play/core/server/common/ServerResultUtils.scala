/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.common

import java.util.{ BitSet => JBitSet }

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import play.api.Logger
import play.api.mvc._
import play.api.http._
import play.api.http.HeaderNames._
import play.api.http.Status._
import play.api.mvc.request.RequestAttrKey
import play.core.utils.{ AsciiBitSet, AsciiSet }

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.util.control.NonFatal

private[play] final class ServerResultUtils(
    sessionBaker: SessionCookieBaker,
    flashBaker: FlashCookieBaker,
    cookieHeaderEncoding: CookieHeaderEncoding) {

  private val logger = Logger(getClass)

  /**
   * Determine whether the connection should be closed, and what header, if any, should be added to the response.
   */
  def determineConnectionHeader(request: RequestHeader, result: Result): ConnectionHeader = {
    if (request.version == HttpProtocol.HTTP_1_1) {
      if (result.header.headers.get(CONNECTION).exists(_.equalsIgnoreCase(CLOSE))) {
        // Close connection, header already exists
        DefaultClose
      } else if ((result.body.isInstanceOf[HttpEntity.Streamed] && result.body.contentLength.isEmpty)
        || request.headers.get(CONNECTION).exists(_.equalsIgnoreCase(CLOSE))) {
        // We need to close the connection and set the header
        SendClose
      } else {
        DefaultKeepAlive
      }
    } else {
      if (result.header.headers.get(CONNECTION).exists(_.equalsIgnoreCase(CLOSE))) {
        DefaultClose
      } else if ((result.body.isInstanceOf[HttpEntity.Streamed] && result.body.contentLength.isEmpty) ||
        request.headers.get(CONNECTION).forall(!_.equalsIgnoreCase(KEEP_ALIVE))) {
        DefaultClose
      } else {
        SendKeepAlive
      }
    }
  }

  /**
   * Validate the result.
   *
   * Returns the validated result, which may be an error result if validation failed.
   */
  def validateResult(request: RequestHeader, result: Result, httpErrorHandler: HttpErrorHandler)(implicit mat: Materializer): Future[Result] = {
    if (request.version == HttpProtocol.HTTP_1_0 && result.body.isInstanceOf[HttpEntity.Chunked]) {
      cancelEntity(result.body)
      val exception = new ServerResultException("HTTP 1.0 client does not support chunked response", result, null)
      val errorResult: Future[Result] = httpErrorHandler.onServerError(request, exception)
      import play.core.Execution.Implicits.trampoline
      errorResult.map { originalErrorResult: Result =>
        // Update the original error with a new status code and a "Connection: close" header
        import originalErrorResult.{ header => h }
        val newHeader = h.copy(
          status = Status.HTTP_VERSION_NOT_SUPPORTED,
          headers = h.headers + (CONNECTION -> CLOSE)
        )
        originalErrorResult.copy(header = newHeader)
      }
    } else if (!mayHaveEntity(result.header.status) && !result.body.isKnownEmpty) {
      cancelEntity(result.body)
      Future.successful(result.copy(body = HttpEntity.Strict(ByteString.empty, result.body.contentType)))
    } else {
      Future.successful(result)
    }
  }

  /*

https://tools.ietf.org/html/rfc5234#appendix-B.1

         ALPHA          =  %x41-5A / %x61-7A   ; A-Z / a-z

         DIGIT          =  %x30-39
                                ; 0-9

         VCHAR          =  %x21-7E
                                ; visible (printing) characters

https://tools.ietf.org/html/rfc7230#section-1.2

   The following core rules are included by reference, as defined in
   [RFC5234], Appendix B.1: ALPHA (letters), CR (carriage return), CRLF
   (CR LF), CTL (controls), DIGIT (decimal 0-9), DQUOTE (double quote),
   HEXDIG (hexadecimal 0-9/A-F/a-f), HTAB (horizontal tab), LF (line
   feed), OCTET (any 8-bit sequence of data), SP (space), and VCHAR (any
   visible [USASCII] character).

  https://tools.ietf.org/html/rfc7230#section-3.2

       header-field   = field-name ":" OWS field-value OWS

     field-name     = token
     field-value    = *( field-content / obs-fold )
     field-content  = field-vchar [ 1*( SP / HTAB ) field-vchar ]
     field-vchar    = VCHAR / obs-text

     obs-fold       = CRLF 1*( SP / HTAB )
                    ; obsolete line folding
                    ; see Section 3.2.4

https://tools.ietf.org/html/rfc7230#section-3.2.6

     token          = 1*tchar

     tchar          = "!" / "#" / "$" / "%" / "&" / "'" / "*"
                    / "+" / "-" / "." / "^" / "_" / "`" / "|" / "~"
                    / DIGIT / ALPHA
                    ; any VCHAR, except delimiters

     quoted-string  = DQUOTE *( qdtext / quoted-pair ) DQUOTE
     qdtext         = HTAB / SP /%x21 / %x23-5B / %x5D-7E / obs-text
     obs-text       = %x80-FF

   */

  private def allowedHeaderChars: AsciiBitSet = {
    (AsciiSet('!', '#', '$', '%', '&', '\'', '*', '+', '-',
      '.', '^', '_', '`', '|', '~') ||| AsciiSet.Sets.Digit ||| AsciiSet.Sets.Alpha).toBitSet
  }

  def validateHeaderNameChars(headerName: String): Unit = {
    @tailrec def loop(i: Int): Unit = {
      if (i < headerName.length) {
        val c = headerName(i)
        if (c > 255) throw new IllegalArgumentException(s"Header value had non-ASCII character: 0x${c.toInt.toHexString}")
        if (!allowedHeaderChars.get(c)) throw new IllegalArgumentException()
        loop(i + 1)
      }
    }
    loop(0)
  }

  def validateHeaderValueChars(headerValue: String): Unit = {
    @tailrec def loop(i: Int): Unit = {
      if (i < headerValue.length) {
        val c = headerValue(i)
        if (c > 255) throw new IllegalArgumentException(s"Header value had non-ASCII character: 0x${c.toInt.toHexString}")
        if (c < 21) throw new IllegalArgumentException(s"Header value had non-visible ASCII character: 0x${c.toInt.toHexString}")
        loop(i + 1)
      }
    }
    loop(0)
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

    import play.core.Execution.Implicits.trampoline

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

  /** Whether the given status may have an entity or not. */
  def mayHaveEntity(status: Int): Boolean = status match {
    case CONTINUE | SWITCHING_PROTOCOLS | NO_CONTENT | NOT_MODIFIED =>
      false
    case _ =>
      true
  }

  /**
   * Cancel the entity.
   *
   * While theoretically, an Akka streams Source is not supposed to hold resources, in practice, this is very often not
   * the case, for example, the response from an Akka HTTP client may have an associated Source that must be consumed
   * (or cancelled) before the associated connection can be returned to the connection pool.
   */
  def cancelEntity(entity: HttpEntity)(implicit mat: Materializer) = {
    entity match {
      case HttpEntity.Chunked(chunks, _) => chunks.runWith(Sink.cancelled)
      case HttpEntity.Streamed(data, _, _) => data.runWith(Sink.cancelled)
      case _ =>
    }
  }

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
  case object SendKeepAlive extends ConnectionHeader {
    override def willClose = false
    override def header = Some(KEEP_ALIVE)
  }
  /**
   * A `Connection: close` header should be sent. Used to
   * force an HTTP 1.1 connection to close.
   */
  case object SendClose extends ConnectionHeader {
    override def willClose = true
    override def header = Some(CLOSE)
  }
  /**
   * No `Connection` header should be sent. Used on an HTTP 1.0
   * connection where the default behavior is to close the connection,
   * or when the response already has a Connection: close header.
   */
  case object DefaultClose extends ConnectionHeader {
    override def willClose = true
    override def header = None
  }
  /**
   * No `Connection` header should be sent. Used on an HTTP 1.1
   * connection where the default behavior is to keep the connection
   * open.
   */
  case object DefaultKeepAlive extends ConnectionHeader {
    override def willClose = false
    override def header = None
  }

  // Values for the Connection header
  private val KEEP_ALIVE = "keep-alive"
  private val CLOSE = "close"

  /**
   * Bake the cookies and prepare the new Set-Cookie header.
   */
  def prepareCookies(requestHeader: RequestHeader, result: Result): Result = {
    val requestHasFlash = requestHeader.attrs.get(RequestAttrKey.Flash) match {
      case None =>
        // The request didn't have a flash object in it, either because we
        // used a custom RequestFactory which didn't install the flash object
        // or because there was an error in request processing which caused
        // us to bypass the application's RequestFactory. In this case we
        // can assume that there is no flash object we need to clear.
        false
      case Some(flashCell) =>
        // The request had a flash object and it was non-empty, so the flash
        // cookie value may need to be cleared.
        !flashCell.value.isEmpty
    }
    result.bakeCookies(cookieHeaderEncoding, sessionBaker, flashBaker, requestHasFlash)
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
          splitSetCookieHeaderValue(value)
            .map { cookiePart =>
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

  def splitSetCookieHeaderValue(value: String): Seq[String] =
    cookieHeaderEncoding.SetCookieHeaderSeparatorRegex.split(value)
}
