package play.api.mvc

import play.core._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.http._
import play.api.libs.json._
import play.api.http.Status._
import play.api.http.HeaderNames._

import scala.concurrent.{ Future, ExecutionContext }

import play.core.Execution.internalContext

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

/**
 * Any Action result.
 */
sealed trait Result extends NotNull with WithHeaders[Result]

sealed trait WithHeaders[+A <: Result] {
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
  def withHeaders(headers: (String, String)*): A

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
  def withCookies(cookies: Cookie*): A


  /**
   * Discards cookies along this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").discardingCookies("theme")
   * }}}
   *
   * @param names the names of the cookies to discard along to this result
   * @return the new result
   */
  @deprecated("This method can only discard cookies on the / path with no domain and without secure set.  Use discardingCookies(DiscardingCookie*) instead.", "2.1")
  def discardingCookies(name: String, names: String*): A = discardingCookies((name :: names.toList).map(n => DiscardingCookie(n)):_*)

  /**
   * Discards cookies along this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").discardingCookies(DiscardingCookie("theme"))
   * }}}
   *
   * @param cookies the cookies to discard along to this result
   * @return the new result
   */
  def discardingCookies(cookies: DiscardingCookie*): A

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
  def withSession(session: Session): A

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
  def withSession(session: (String, String)*): A

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
  def withNewSession: A

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
  def flashing(flash: Flash): A

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
  def flashing(values: (String, String)*): A

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
  def as(contentType: String): A
}

/**
 * Helper utilities for Result values.
 */
object PlainResult {

  /**
   * Extractor:
   *
   * {{{
   * case Result(status, headers) => ...
   * }}}
   */
  def unapply(result: Result): Option[(Int, Map[String, String])] = result match {
    case r: PlainResult => Some((r.header.status, r.header.headers))
    case _ => None
  }

}

/**
 * A plain HTTP result.
 */
trait PlainResult extends Result with WithHeaders[PlainResult] {

  /**
   * The response header
   */
  val header: ResponseHeader

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
  def discardingCookies(cookies: DiscardingCookie*): PlainResult = {
    withHeaders(SET_COOKIE -> Cookies.merge(header.headers.get(SET_COOKIE).getOrElse(""), cookies.map(_.toCookie)))
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
    if (session.isEmpty) discardingCookies(Session.discard) else withCookies(Session.encodeAsCookie(session))
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
case class AsyncResult(result: Future[Result]) extends Result with WithHeaders[AsyncResult] {

  /**
   * Apply some transformation to this `AsyncResult`
   *
   * @param f The transformation function
   * @return The transformed `AsyncResult`
   */
  def transform(f: PlainResult => Result)(implicit ec: ExecutionContext): AsyncResult = AsyncResult (result.map {
      case AsyncResult(r) => AsyncResult(r.map{
        case r:PlainResult => f(r)
        case r:AsyncResult => r.transform(f)})
      case r:PlainResult => f(r)
  })

  def unflatten:Future[PlainResult] = result.flatMap {
      case r:PlainResult => Promise.pure(r)
      case r@AsyncResult(_) => r.unflatten
  }(internalContext)

  def map(f: Result => Result)(implicit ec: ExecutionContext): AsyncResult = AsyncResult(result.map(f))

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
  def withHeaders(headers: (String, String)*): AsyncResult = {
    map(_.withHeaders(headers: _*))(internalContext)
  }

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
  def withCookies(cookies: Cookie*): AsyncResult = {
    map(_.withCookies(cookies: _*))(internalContext)
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
  def discardingCookies(cookies: DiscardingCookie*): AsyncResult = {
    map(_.discardingCookies(cookies: _*))(internalContext)
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
  def withSession(session: Session): AsyncResult = {
    map(_.withSession(session))(internalContext)
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
  def withSession(session: (String, String)*): AsyncResult = {
    map(_.withSession(session: _*))(internalContext)
  }

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
  def withNewSession: AsyncResult = {
    map(_.withNewSession)(internalContext)
  }

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
  def flashing(flash: Flash): AsyncResult = {
    map(_.flashing(flash))(internalContext)
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
  def flashing(values: (String, String)*): AsyncResult = {
    map(_.flashing(values: _*))(internalContext)
  }

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
  def as(contentType: String): AsyncResult = {
    map(_.as(contentType))(internalContext)
  }

}


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

    /**
     * Send a file.
     *
     * @param content The file to send
     * @param inline Use Content-Disposition inline or attachment.
     * @param fileName function to retrieve the file name (only used for Content-Disposition attachment)
     */
    def sendFile(content: java.io.File, inline: Boolean = false, fileName: java.io.File => String = _.getName, onClose: () => Unit = () => ()): SimpleResult[Array[Byte]] = {
      SimpleResult(
        header = ResponseHeader(OK, Map(
          CONTENT_LENGTH -> content.length.toString,
          CONTENT_TYPE -> play.api.libs.MimeTypes.forFileName(content.getName).getOrElse(play.api.http.ContentTypes.BINARY)
        ) ++ (if (inline) Map.empty else Map(CONTENT_DISPOSITION -> ("attachment; filename=" + fileName(content))))),
        Enumerator.fromFile(content) &> Enumeratee.onIterateeDone(onClose)
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

    def feed[C](content: Enumerator[C])(implicit writeable: Writeable[C]): SimpleResult[C] = {
      SimpleResult(
        header = ResponseHeader(status, Map(CONTENT_LENGTH -> "-1")),
        body = content)
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

  def Async(promise: Future[Result]) = AsyncResult(promise)

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

  /** Generates a ‘207 MULTI_STATUS’ result. */
  val MultiStatus = new Status(MULTI_STATUS)

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

  /** Generates a ‘422 UNPROCESSABLE_ENTITY’ result. */
  val UnprocessableEntity = new Status(UNPROCESSABLE_ENTITY)

  /** Generates a ‘423 LOCKED’ result. */
  val Locked = new Status(LOCKED)

  /** Generates a ‘424 FAILED_DEPENDENCY’ result. */
  val FailedDependency = new Status(FAILED_DEPENDENCY)

  /** Generates a ‘429 TOO_MANY_REQUEST’ result. */
  val TooManyRequest = new Status(TOO_MANY_REQUEST)

  /** Generates a ‘500 INTERNAL_SERVER_ERROR’ result. */
  val InternalServerError = new Status(INTERNAL_SERVER_ERROR)
  
  /** Generates a ‘501 NOT_IMPLEMENTED’ result. */
  val NotImplemented = new Status(NOT_IMPLEMENTED)

  /** Generates a ‘502 BAD_GATEWAY’ result. */
  val BadGateway = new Status(BAD_GATEWAY)

  /** Generates a ‘503 SERVICE_UNAVAILABLE’ result. */
  val ServiceUnavailable = new Status(SERVICE_UNAVAILABLE)

  /** Generates a ‘504 GATEWAY_TIMEOUT’ result. */
  val GatewayTimeout = new Status(GATEWAY_TIMEOUT)

  /** Generates a ‘505 HTTP_VERSION_NOT_SUPPORTED’ result. */
  val HttpVersionNotSupported = new Status(HTTP_VERSION_NOT_SUPPORTED)

  /** Generates a ‘507 INSUFFICIENT_STORAGE’ result. */
  val InsufficientStorage = new Status(INSUFFICIENT_STORAGE)

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
   * @param status HTTP status
   */
  def Redirect(url: String, status: Int): SimpleResult[Results.EmptyContent] = Redirect(url, Map.empty, status)

  /**
   * Generates a redirect simple result.
   *
   * @param url the URL to redirect to
   * @param queryString queryString parameters to add to the queryString
   * @param status HTTP status
   */
  def Redirect(url: String, queryString: Map[String, Seq[String]] = Map.empty, status: Int = SEE_OTHER) = {
    import java.net.URLEncoder
    val fullUrl = url + Option(queryString).filterNot(_.isEmpty).map { params =>
      (if (url.contains("?")) "&" else "?") + params.toSeq.flatMap { pair =>
        pair._2.map(value => (pair._1 + "=" + URLEncoder.encode(value, "utf-8")))
      }.mkString("&")
    }.getOrElse("")
    Status(status).withHeaders(LOCATION -> fullUrl)
  }

  /**
   * Generates a redirect simple result.
   *
   * @param call Call defining the URL to redirect to, which typically comes from the reverse router
   */
  def Redirect(call: Call): SimpleResult[Results.EmptyContent] = Redirect(call.url)

}
