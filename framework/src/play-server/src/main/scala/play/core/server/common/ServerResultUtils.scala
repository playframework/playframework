/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.common

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import play.api.mvc._
import play.api.http.{ Status, HttpEntity, HttpProtocol }
import play.api.http.HeaderNames._

object ServerResultUtils {

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
  def validateResult(request: RequestHeader, result: Result)(implicit mat: Materializer): Result = {
    if (request.version == HttpProtocol.HTTP_1_0 && result.body.isInstanceOf[HttpEntity.Chunked]) {
      cancelEntity(result.body)
      Results.Status(Status.HTTP_VERSION_NOT_SUPPORTED)
        .apply("The response to this request is chunked and hence requires HTTP 1.1 to be sent, but this is a HTTP 1.0 request.")
        .withHeaders(CONNECTION -> CLOSE)
    } else if (!mayHaveEntity(result.header.status) && !result.body.isKnownEmpty) {
      cancelEntity(result.body)
      result.copy(body = HttpEntity.Strict(ByteString.empty, result.body.contentType))
    } else {
      result
    }
  }

  private def mayHaveEntity(status: Int) =
    status != Status.NO_CONTENT && status != Status.NOT_MODIFIED

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
