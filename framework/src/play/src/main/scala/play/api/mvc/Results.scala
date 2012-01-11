package play.api.mvc

import play.core._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import play.api.libs.json._
import play.api.http.Status._
import play.api.http.HeaderNames._

/**
 * A simple HTTP response header, used for standard responses.
 *
 * @param status the response status, e.g. ‘200 OK’
 * @param headers the HTTP headers
 */
case class ResponseHeader(status: Int, headers: Map[String, String] = Map.empty) {

  override def toString = {
    status + ", " + headers
  }

}

/** Any Action result. */
sealed trait Result

object Result {

  def unapply(result: Result): Option[(Int, Map[String, String])] = result match {
    case r: PlainResult => Some(r.header.status, r.header.headers)
    case _ => None
  }

}

trait PlainResult extends Result {

  /**
   * The response header
   */
  val header: ResponseHeader

  /**
   * Adds HTTP headers to this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withHeaders(ETAG -> "0")
   * }}}
   *
   * @param headers the headers to add to this result.
   * @return the new result
   */
  def withHeaders(headers: (String, String)*): PlainResult

  /**
   * Adds cookies to this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withCookies(Cookie("theme", "blue"))
   * }}}
   *
   * @param cookies the cookies to add to this result
   * @return the new result
   */
  def withCookies(cookies: Cookie*): PlainResult = {
    withHeaders(SET_COOKIE -> Cookies.merge(header.headers.get(SET_COOKIE).getOrElse(""), cookies))
  }

  /**
   * Discards cookies along this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").discardingCookies("theme")
   * }}}
   *
   * @param cookies the cookies to discard along to this result
   * @return the new result
   */
  def discardingCookies(names: String*): PlainResult = {
    withHeaders(SET_COOKIE -> Cookies.merge(header.headers.get(SET_COOKIE).getOrElse(""), Nil, discard = names))
  }

  /**
   * Sets a new session for this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withSession(session + ("saidHello" -> "true"))
   * }}}
   *
   * @param session the session to set with this result
   * @return the new result
   */
  def withSession(session: Session): PlainResult = {
    if (session.isEmpty) discardingCookies(Session.COOKIE_NAME) else withCookies(Session.encodeAsCookie(session))
  }

  /**
   * Sets a new session for this result, discarding the existing session.
   *
   * For example:
   * {{{
   * Ok("Hello world").withSession("saidHello" -> "yes")
   * }}}
   *
   * @param session the session to set with this result
   * @return the new result
   */
  def withSession(session: (String, String)*): PlainResult = withSession(Session(session.toMap))

  /**
   * Discards the existing session for this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withNewSession
   * }}}
   *
   * @return the new result
   */
  def withNewSession: PlainResult = withSession(Session())

  /**
   * Adds values to the flash scope for this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").flashing(flash + ("success" -> "Done!"))
   * }}}
   *
   * @param flash the flash scope to set with this result
   * @return the new result
   */
  def flashing(flash: Flash): PlainResult = {
    withCookies(Flash.encodeAsCookie(flash))
  }

  /**
   * Adds values to the flash scope for this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").flashing("success" -> "Done!")
   * }}}
   *
   * @param flash the flash values to set with this result
   * @return the new result
   */
  def flashing(values: (String, String)*): PlainResult = flashing(Flash(values.toMap))

  /**
   * Changes the result content type.
   *
   * For example:
   * {{{
   * Ok("<text>Hello world</text>").as("text/xml")
   * }}}
   *
   * @param contentType the new content type.
   * @return the new result
   */
  def as(contentType: String): PlainResult = withHeaders(CONTENT_TYPE -> contentType)

}

/**
 * A simple result, which defines the response header and a body ready to send to the client.
 *
 * @tparam A the response body content type
 * @param header the response header, which contains status code and HTTP headers
 * @param body the response body
 */
case class SimpleResult[A](header: ResponseHeader, body: Enumerator[A])(implicit val writeable: Writeable[A]) extends PlainResult {

  /** The body content type. */
  type BODY_CONTENT = A

  /**
   * Adds headers to this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withHeaders(ETAG -> "0")
   * }}}
   *
   * @param headers the headers to add to this result.
   * @return the new result
   */
  def withHeaders(headers: (String, String)*) = {
    copy(header = header.copy(headers = header.headers ++ headers))
  }

  override def toString = {
    "SimpleResult(" + header + ")"
  }

}

/**
 * A chunked result, which defines the response header and a chunks enumerator to send asynchronously to the client.
 *
 * @tparam A the response body content type.
 * @param header the response header, which contains status code and HTTP headers
 * @param chunks the chunks enumerator
 */
case class ChunkedResult[A](header: ResponseHeader, chunks: Iteratee[A, Unit] => _)(implicit val writeable: Writeable[A]) extends PlainResult {

  /** The body content type. */
  type BODY_CONTENT = A

  /**
   * Adds headers to this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withHeaders(ETAG -> "0")
   * }}}
   *
   * @param headers the headers to add to this result.
   * @return the new result
   */
  def withHeaders(headers: (String, String)*) = {
    copy(header = header.copy(headers = header.headers ++ headers))
  }

}

/**
 * An `AsyncResult` handles a `Promise` of result for cases where the result is not ready yet.
 *
 * @param result the promise of result, which can be any other result type
 */
case class AsyncResult(result: Promise[Result]) extends Result

/**
 * A Codec handle the conversion of String to Byte arrays.
 *
 * @param charset The charset to be sent to the client.
 * @param transform The transformation function.
 */
case class Codec(val charset: String)(val encode: String => Array[Byte], val decode: Array[Byte] => String)

/**
 * Default Codec support.
 */
object Codec {

  /**
   * Create a Codec from an encoding already supported by the JVM.
   */
  def javaSupported(charset: String) = Codec(charset)(str => str.getBytes(charset), bytes => new String(bytes, charset))

  /**
   * Codec for UTF-8
   */
  implicit val utf_8 = javaSupported("utf-8")

  /**
   * Codec for ISO-8859-1
   */
  val iso_8859_1 = javaSupported("iso-8859-1")

}

/**
 * Content writeable to the HTTP response.
 *
 * @tparam A the content type
 */
case class Writeable[A](transform: (A => Array[Byte]))

/**
 * Helper utilities for `Writeable`.
 */
object Writeable {

  /** Straightforward `Writeable` for String values. */
  implicit def wString(implicit codec: Codec): Writeable[String] = Writeable[String](str => codec.encode(str))

  /** Straightforward `Writeable` for Array[Byte] values. */
  implicit val wBytes: Writeable[Array[Byte]] = Writeable[Array[Byte]](identity)

}

/**
 * Defines the default content type for type `A`.
 *
 * @tparam A the content type
 * @param the default content type for `A`, if any
 */
case class ContentTypeOf[A](mimeType: Option[String])

/** Helper utilities to generate results. */
object Results extends Results {

  /** Empty result, i.e. nothing to send. */
  case class EmptyContent()

}

/** Helper utilities to generate results. */
trait Results {

  import play.api._
  import play.api.http.Status._
  import play.api.http.HeaderNames._
  import play.api.http.ContentTypes
  import play.api.templates._
  import play.api.libs.json._

  /** `Writeable` for `Content` values. */
  implicit def writeableOf_Content[C <: Content](implicit codec: Codec): Writeable[C] = {
    Writeable[C](content => codec.encode(content.body))
  }

  /** `Writeable` for `NodeSeq` values - literal Scala XML. */
  implicit def writeableOf_NodeSeq[C <: scala.xml.NodeSeq](implicit codec: Codec): Writeable[C] = {
    Writeable[C](xml => codec.encode(xml.toString))
  }

  /** `Writeable` for `NodeBuffer` values - literal Scala XML. */
  implicit def writeableOf_NodeBuffer(implicit codec: Codec): Writeable[scala.xml.NodeBuffer] = {
    Writeable[scala.xml.NodeBuffer](xml => codec.encode(xml.toString))
  }

  /** `Writeable` for `JsValue` values - Json */
  implicit def writeableOf_JsValue(implicit codec: Codec): Writeable[JsValue] = {
    Writeable[JsValue](jsval => codec.encode(jsval.toString))
  }

  /** `Writeable` for empty responses. */
  implicit val writeableOf_EmptyContent = Writeable[Results.EmptyContent](_ => Array.empty)

  /** Default content type for `Html` values (`text/html`). */
  implicit def contentTypeOf_Html(implicit codec: Codec): ContentTypeOf[Html] = {
    ContentTypeOf[Html](Some(ContentTypes.HTML))
  }

  /** Default content type for `Xml` values (`text/xml`). */
  implicit def contentTypeOf_Xml(implicit codec: Codec): ContentTypeOf[Xml] = {
    ContentTypeOf[Xml](Some(ContentTypes.XML))
  }

  /** Default content type for `JsValue` values (`application/json`). */
  implicit def contentTypeOf_JsValue(implicit codec: Codec): ContentTypeOf[JsValue] = {
    ContentTypeOf[JsValue](Some(ContentTypes.JSON))
  }

  /** Default content type for `Txt` values (`text/plain`). */
  implicit def contentTypeOf_Txt(implicit codec: Codec): ContentTypeOf[Txt] = {
    ContentTypeOf[Txt](Some(ContentTypes.TEXT))
  }

  /** Default content type for `String` values (`text/plain`). */
  implicit def contentTypeOf_String(implicit codec: Codec): ContentTypeOf[String] = {
    ContentTypeOf[String](Some(ContentTypes.TEXT))
  }

  /** Default content type for `NodeSeq` values (`text/xml`). */
  implicit def contentTypeOf_NodeSeq[C <: scala.xml.NodeSeq](implicit codec: Codec): ContentTypeOf[C] = {
    ContentTypeOf[C](Some(ContentTypes.XML))
  }

  /** Default content type for `NodeBuffer` values (`text/xml`). */
  implicit def contentTypeOf_NodeBuffer(implicit codec: Codec): ContentTypeOf[scala.xml.NodeBuffer] = {
    ContentTypeOf[scala.xml.NodeBuffer](Some(ContentTypes.XML))
  }

  /** Default content type for byte array (application/application/octet-stream). */
  implicit def contentTypeOf_ByteArray: ContentTypeOf[Array[Byte]] = ContentTypeOf[Array[Byte]](Some(ContentTypes.BINARY))

  /** Default content type for empty responses (no content type). */
  implicit def contentTypeOf_EmptyContent: ContentTypeOf[Results.EmptyContent] = ContentTypeOf[Results.EmptyContent](None)

  /**
   * Generates default `SimpleResult` from a content type, headers and content.
   *
   * @param status the HTTP response status, e.g ‘200 OK’
   */
  class Status(status: Int) extends SimpleResult[Results.EmptyContent](header = ResponseHeader(status), body = Enumerator(Results.EmptyContent())) {

    /**
     * Set the result's content.
     *
     * @tparam C the content type
     * @param content content to send
     * @param a `SimpleResult`
     */
    def apply[C](content: C)(implicit writeable: Writeable[C], contentTypeOf: ContentTypeOf[C]): SimpleResult[C] = {
      SimpleResult(
        header = ResponseHeader(status, contentTypeOf.mimeType.map(ct => Map(CONTENT_TYPE -> ct)).getOrElse(Map.empty)),
        Enumerator(content))
    }

    def sendFile(content: java.io.File, fileName: java.io.File => String = _.getName): SimpleResult[Array[Byte]] = {
      SimpleResult(
        header = ResponseHeader(OK, Map(
          CONTENT_LENGTH -> content.length.toString,
          CONTENT_TYPE -> play.api.libs.MimeTypes.forFileName(content.getName).getOrElse(play.api.http.ContentTypes.BINARY),
          CONTENT_DISPOSITION -> ("attachment; filename=" + fileName(content))
        )),
        Enumerator.enumerateFile(content)
      )
    }

    /**
     * Set the result's content as chunked.
     *
     * @tparam C the chunk type
     * @param content Enumerator providing the chunked content.
     * @param a `ChunkedResult`
     */
    def stream[C](content: Enumerator[C])(implicit writeable: Writeable[C], contentTypeOf: ContentTypeOf[C]): ChunkedResult[C] = {
      ChunkedResult(
        header = ResponseHeader(status, contentTypeOf.mimeType.map(ct => Map(CONTENT_TYPE -> ct)).getOrElse(Map.empty)),
        iteratee => content |>> iteratee)
    }

    /**
     * Set the result's content as chunked.
     *
     * @tparam C the chunk type
     * @param content A function that will give you the Iteratee to write in once ready.
     * @param a `ChunkedResult`
     */
    def stream[C](content: Iteratee[C, Unit] => Unit)(implicit writeable: Writeable[C], contentTypeOf: ContentTypeOf[C]): ChunkedResult[C] = {
      ChunkedResult(
        header = ResponseHeader(status, contentTypeOf.mimeType.map(ct => Map(CONTENT_TYPE -> ct)).getOrElse(Map.empty)),
        content)
    }

  }

  def Async(promise: Promise[Result]) = AsyncResult(promise)

  /** Generates a ‘200 OK’ result. */
  val Ok = new Status(OK)

  /** Generates a ‘201 CREATED’ result. */
  val Created = new Status(CREATED)

  /** Generates a ‘202 ACCEPTED’ result. */
  val Accepted = new Status(ACCEPTED)

  /** Generates a ‘203 NON_AUTHORITATIVE_INFORMATION’ result. */
  val NonAuthoritativeInformation = new Status(NON_AUTHORITATIVE_INFORMATION)

  /** Generates a ‘204 NO_CONTENT’ result. */
  val NoContent = SimpleResult(header = ResponseHeader(NO_CONTENT), body = Enumerator(Results.EmptyContent()))

  /** Generates a ‘205 RESET_CONTENT’ result. */
  val ResetContent = SimpleResult(header = ResponseHeader(RESET_CONTENT), body = Enumerator(Results.EmptyContent()))

  /** Generates a ‘206 PARTIAL_CONTENT’ result. */
  val PartialContent = new Status(PARTIAL_CONTENT)

  /**
   * Generates a ‘301 MOVED_PERMANENTLY’ simple result.
   *
   * @param url the URL to redirect to
   */
  def MovedPermanently(url: String): SimpleResult[Results.EmptyContent] = Redirect(url, MOVED_PERMANENTLY)

  /**
   * Generates a ‘302 FOUND’ simple result.
   *
   * @param url the URL to redirect to
   */
  def Found(url: String): SimpleResult[Results.EmptyContent] = Redirect(url, FOUND)

  /**
   * Generates a ‘303 SEE_OTHER’ simple result.
   *
   * @param url the URL to redirect to
   */
  def SeeOther(url: String): SimpleResult[Results.EmptyContent] = Redirect(url, SEE_OTHER)

  /** Generates a ‘304 NOT_MODIFIED’ result. */
  val NotModified = SimpleResult(header = ResponseHeader(NOT_MODIFIED), body = Enumerator(Results.EmptyContent()))

  /**
   * Generates a ‘307 TEMPORARY_REDIRECT’ simple result.
   *
   * @param url the URL to redirect to
   */
  def TemporaryRedirect(url: String): SimpleResult[Results.EmptyContent] = Redirect(url, TEMPORARY_REDIRECT)

  /** Generates a ‘400 BAD_REQUEST’ result. */
  val BadRequest = new Status(BAD_REQUEST)

  /** Generates a ‘401 UNAUTHORIZED’ result. */
  val Unauthorized = new Status(UNAUTHORIZED)

  /** Generates a ‘403 FORBIDDEN’ result. */
  val Forbidden = new Status(FORBIDDEN)

  /** Generates a ‘404 NOT_FOUND’ result. */
  val NotFound = new Status(NOT_FOUND)

  /** Generates a ‘405 METHOD_NOT_ALLOWED’ result. */
  val MethodNotAllowed = new Status(METHOD_NOT_ALLOWED)

  /** Generates a ‘406 NOT_ACCEPTABLE’ result. */
  val NotAcceptable = new Status(NOT_ACCEPTABLE)

  /** Generates a ‘408 REQUEST_TIMEOUT’ result. */
  val RequestTimeout = new Status(REQUEST_TIMEOUT)

  /** Generates a ‘409 CONFLICT’ result. */
  val Conflict = new Status(CONFLICT)

  /** Generates a ‘410 GONE’ result. */
  val Gone = new Status(GONE)

  /** Generates a ‘412 PRECONDITION_FAILED’ result. */
  val PreconditionFailed = new Status(PRECONDITION_FAILED)

  /** Generates a ‘413 REQUEST_ENTITY_TOO_LARGE’ result. */
  val EntityTooLarge = new Status(REQUEST_ENTITY_TOO_LARGE)

  /** Generates a ‘414 REQUEST_URI_TOO_LONG’ result. */
  val UriTooLong = new Status(REQUEST_URI_TOO_LONG)

  /** Generates a ‘415 UNSUPPORTED_MEDIA_TYPE’ result. */
  val UnsupportedMediaType = new Status(UNSUPPORTED_MEDIA_TYPE)

  /** Generates a ‘417 EXPECTATION_FAILED’ result. */
  val ExpectationFailed = new Status(EXPECTATION_FAILED)

  /** Generates a ‘429 TOO_MANY_REQUEST’ result. */
  val TooManyRequest = new Status(TOO_MANY_REQUEST)

  /** Generates a ‘500 INTERNAL_SERVER_ERROR’ result. */
  val InternalServerError = new Status(INTERNAL_SERVER_ERROR)

  /** Generates a ‘501 NOT_IMPLEMENTED’ result. */
  val NotImplemented = new Status(NOT_IMPLEMENTED)

  /** Generates a ‘503 SERVICE_UNAVAILABLE’ result. */
  val ServiceUnavailable = new Status(SERVICE_UNAVAILABLE)

  /**
   * Generates a simple result.
   *
   * @param code the status code
   */
  def Status(code: Int) = new Status(code)

  /**
   * Generates a redirect simple result.
   *
   * @param url the URL to redirect to
   */
  def Redirect(url: String, status: Int = SEE_OTHER): SimpleResult[Results.EmptyContent] = Status(status).withHeaders(LOCATION -> url)

  /**
   * Generates a redirect simple result.
   *
   * @param call Call defining the URL to redirect to, which typically comes from the reverse router
   */
  def Redirect(call: Call): SimpleResult[Results.EmptyContent] = Redirect(call.url)

}
