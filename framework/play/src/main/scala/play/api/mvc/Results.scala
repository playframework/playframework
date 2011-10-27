package play.api.mvc

import play.core._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import play.api.http.Status._
import play.api.http.HeaderNames._

/**
 * A simple HTTP response headers. Used for classical responses.
 *
 * @param status The response status (for example 200 OK)
 * @param headers The HTTP headers.
 */
case class ResponseHeader(status: Int, headers: Map[String, String] = Map.empty)

/**
 * Any Action result.
 */
sealed trait Result

/**
 * A simple result (defines the response header and a body ready to send to the client)
 *
 * @tparam A the response body content type.
 * @param header The response header (contains status code and HTTP headers)
 * @param body The response body.
 */
case class SimpleResult[A](header: ResponseHeader, body: Enumerator[A])(implicit val writeable: Writeable[A]) extends Result {

  /**
   * The body content type.
   */
  type BODY_CONTENT = A

  /**
   * Add headers to this result.
   *
   * Example:
   * {{{
   * Ok("Hello world").withHeaders(ETAG -> "0")
   * }}}
   *
   * @param headers The headers to add to this result.
   * @return The new result.
   */
  def withHeaders(headers: (String, String)*) = {
    copy(header = header.copy(headers = header.headers ++ headers))
  }

  /**
   * Add cookies to this result.
   *
   * Example:
   * {{{
   * Ok("Hello world").withCookies(Cookie("theme", "blue"))
   * }}}
   *
   * @param cookies The cookies to add to this result.
   * @return The new result.
   */
  def withCookies(cookies: Cookie*) = {
    withHeaders(SET_COOKIE -> Cookies.merge(header.headers.get(SET_COOKIE).getOrElse(""), cookies))
  }

  /**
   * Discard cookies along this result.
   *
   * Example:
   * {{{
   * Ok("Hello world").discardingCookies("theme")
   * }}}
   *
   * @param cookies The cookies to discard along to this result.
   * @return The new result.
   */
  def discardingCookies(names: String*) = {
    withHeaders(SET_COOKIE -> Cookies.merge(header.headers.get(SET_COOKIE).getOrElse(""), Nil, discard = names))
  }

  /**
   * Set a new session for this result.
   *
   * Example:
   * {{{
   * Ok("Hello world").withSession(session + ("saidHello" -> "true"))
   * }}}
   *
   * @param session The session to set with this result.
   * @return The new result.
   */
  def withSession(session: Session): SimpleResult[A] = {
    if (session.isEmpty) discardingCookies(Session.COOKIE_NAME) else withCookies(Session.encodeAsCookie(session))
  }

  /**
   * Set a new session for this result (discard the existing session).
   *
   * Example:
   * {{{
   * Ok("Hello world").withSession("saidHello" -> "yes")
   * }}}
   *
   * @param session The session to set with this result.
   * @return The new result.
   */
  def withSession(session: (String, String)*): SimpleResult[A] = withSession(Session(session.toMap))

  /**
   * Discard the existing session for this result.
   *
   * Example:
   * {{{
   * Ok("Hello world").withNewSession
   * }}}
   *
   * @return The new result.
   */
  def withNewSession = withSession(Session())

  /**
   * Add values to the flash scope along this result.
   *
   * Example:
   * {{{
   * Ok("Hello world").flashing(flash + ("success" -> "Done!"))
   * }}}
   *
   * @param flash The flash scope to set with this result.
   * @return The new result.
   */
  def flashing(flash: Flash): SimpleResult[A] = {
    withCookies(Flash.encodeAsCookie(flash))
  }

  /**
   * Add values to the flash scope along this result.
   *
   * Example:
   * {{{
   * Ok("Hello world").flashing("success" -> "Done!")
   * }}}
   *
   * @param flash The flash values to set with this result.
   * @return The new result.
   */
  def flashing(values: (String, String)*): SimpleResult[A] = flashing(Flash(values.toMap))

  /**
   * Change the result content type.
   *
   * Example:
   * {{{
   * Ok("<text>Hello world</text>").as("text/xml")
   * }}}
   *
   * @param contentType The new content type.
   * @return The new result.
   */
  def as(contentType: String) = withHeaders(CONTENT_TYPE -> contentType)

}

/**
 * A chunked result (defines the response header and a chunks enumerator to send asynchronously to the client)
 *
 * @tparam A the response body content type.
 * @param header The response header (contains status code and HTTP headers)
 * @param chunks The chunks enumerator.
 */
case class ChunkedResult[A](header: ResponseHeader, chunks: Enumerator[A])(implicit val writeable: Writeable[A]) extends Result {

  /**
   * The body content type.
   */
  type BODY_CONTENT = A

}

/**
 * A websocket result.
 *
 * @tparam A The socket messages type.
 * @param f The socket messages generator.
 */
case class SocketResult[A](f: (Enumerator[String], Iteratee[A, Unit]) => Unit)(implicit val writeable: AsString[A]) extends Result

/**
 * Helper utilities to generate websockets.
 */
object SocketResult {

  /**
   * Create a websoket result from inbound and outbound channels.
   *
   * @param readIn The inboud channel.
   * @param writeOut The outbound channel.
   * @return A SocketResult
   */
  def using[A](readIn: Iteratee[String, Unit], writeOut: Enumerator[A])(implicit writeable: AsString[A]) = new SocketResult[A]((e, i) => { readIn <<: e; i <<: writeOut })

}

/**
 * An AsyncResult handle a promise of result for cases where the result is not ready yet.
 *
 * @param result The promise of result (can be any other result type).
 */
case class AsyncResult(result: Promise[Result]) extends Result

/**
 * Content writeable to the HTTP response.
 *
 * @tparam A The content type.
 */
sealed trait Writeable[A]

/**
 * Defines a writeable for text content types.
 *
 * @tparam A The content type.
 */
case class AsString[A](transform: (A => String)) extends Writeable[A]

/**
 * Defines a writeable for binary content types.
 *
 * @tparam A The content type.
 */
case class AsBytes[A](transform: (A => Array[Byte])) extends Writeable[A]

/**
 * Helper utilities for Writeable.
 */
object Writeable {

  /**
   * Straight forward writeable for String values.
   */
  implicit val wString: Writeable[String] = AsString[String](identity)

  /**
   * Straight forward writeable for Array[Byte] values.
   */
  implicit val wBytes: Writeable[Array[Byte]] = AsBytes[Array[Byte]](identity)

}

/**
 * Defines the default content type for type A.
 *
 * @tparam A The content type.
 * @param Retrieve the default content type for A if any.
 */
case class ContentTypeOf[A](resolve: (A => Option[String]))

/**
 * Helper utilities to generate results.
 */
object Results extends Results {

  /**
   * Empty result (nothing to send)
   */
  case class Empty()

}

/**
 * Helper utilities to generate results.
 */
trait Results {

  import play.api._
  import play.api.http.Status._
  import play.api.http.HeaderNames._

  /**
   * Writeable for String values.
   */
  implicit val writeableStringOf_String: AsString[String] = AsString[String](identity)

  /**
   * Writeable for any values of type play.api.mvc.Content.
   *
   * @see play.api.mvc.Content
   */
  implicit def writeableStringOf_Content[C <: Content]: Writeable[C] = AsString[C](c => c.body)

  /**
   * Writeable for NodeSeq values (literal scala XML).
   */
  implicit def writeableStringOf_NodeSeq[C <: scala.xml.NodeSeq] = AsString[C](x => x.toString)

  /**
   * Writeable for empty responses.
   */
  implicit val writeableStringOf_Empty = AsString[Results.Empty](_ => "")

  /**
   * Default content type for String values (text/plain)
   */
  implicit val contentTypeOf_String = ContentTypeOf[String](_ => Some("text/plain"))

  /**
   * Default content type for any values of type play.api.mvc.Content (read from the content trait).
   *
   * @see play.api.mvc.Content
   */
  implicit def contentTypeOf_Content[C <: Content] = ContentTypeOf[C](c => Some(c.contentType))

  /**
   * Default content type for NodeSeq values (text/xml)
   */
  implicit def contentTypeOf_NodeSeq[C <: scala.xml.NodeSeq] = ContentTypeOf[C](_ => Some("text/xml"))

  /**
   * Default content type for empty responses (no content type).
   */
  implicit def contentTypeOf_Empty = ContentTypeOf[Results.Empty](_ => None)

  /**
   * Generate default SimpleResult from a content type, headers and content.
   *
   * @param status The HTTP response status (for example 200 OK).
   */
  class Status(status: Int) extends SimpleResult[Results.Empty](header = ResponseHeader(status), body = Enumerator(Results.Empty())) {

    /**
     * Add content to the result.
     *
     * @tparam C the content type.
     * @param content Content to send.
     * @param A SimpleResult
     */
    def apply[C](content: C = Results.Empty())(implicit writeable: Writeable[C], contentTypeOf: ContentTypeOf[C]): SimpleResult[C] = {
      SimpleResult(header = ResponseHeader(status, contentTypeOf.resolve(content).map(ct => Map(CONTENT_TYPE -> ct)).getOrElse(Map.empty)), Enumerator(content))
    }

  }

  /**
   * Generates a 200 OK simple result.
   */
  val Ok = new Status(OK)

  /**
   * Generates a 401 UNAUTHORIZED simple result.
   */
  val Unauthorized = new Status(UNAUTHORIZED)

  /**
   * Generates a 404 NOT_FOUND simple result.
   */
  val NotFound = new Status(NOT_FOUND)

  /**
   * Generates a 403 FORBIDDEN simple result.
   */
  val Forbidden = new Status(FORBIDDEN)

  /**
   * Generates a 400 BAD_REQUEST simple result.
   */
  val BadRequest = new Status(BAD_REQUEST)

  /**
   * Generates a 500 INTERNAL_SERVER_ERROR simple result.
   */
  val InternalServerError = new Status(INTERNAL_SERVER_ERROR)

  /**
   * Generates a 501 NOT_IMPLEMENTED simple result.
   */
  val NotImplemented = new Status(NOT_IMPLEMENTED)

  /**
   * Generates a simple result.
   *
   * @param code The status code.
   */
  def Status(code: Int) = new Status(code)

  /**
   * Generates a 302 FOUND simple result.
   *
   * @param url The url to redirect.
   */
  def Redirect(url: String): SimpleResult[Results.Empty] = Status(FOUND).withHeaders(LOCATION -> url)

  /**
   * Generates a 302 FOUND simple result.
   *
   * @param call Call defining the url to redirect (typically comes from reverse router).
   */
  def Redirect(call: Call): SimpleResult[Results.Empty] = Redirect(call.url)

  /**
   * To be deleted...
   */
  def Binary(stream: java.io.InputStream, length: Option[Long] = None, contentType: String = "application/octet-stream") = {
    import scalax.io.Resource
    val e = Enumerator(Resource.fromInputStream(stream).byteArray)

    SimpleResult[Array[Byte]](header = ResponseHeader(
      OK,
      Map(CONTENT_TYPE -> contentType) ++ length.map(length =>
        Map(CONTENT_LENGTH -> (length.toString))).getOrElse(Map.empty)),
      body = e)

  }

}