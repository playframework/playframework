/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import java.nio.file.{ Files, Path }

import akka.stream.scaladsl.{ StreamConverters, Source }
import akka.util.ByteString
import org.joda.time.{ DateTime, DateTimeZone }
import org.joda.time.format.{ DateTimeFormat, DateTimeFormatter }
import play.api.i18n.{ MessagesApi, Lang }
import play.api.libs.iteratee._
import play.api.http._
import play.api.http.HeaderNames._
import play.api.libs.streams.Streams

import play.core.utils.CaseInsensitiveOrdered
import scala.collection.immutable.TreeMap

/**
 * A simple HTTP response header, used for standard responses.
 *
 * @param status the response status, e.g. 200
 * @param _headers the HTTP headers
 * @param reasonPhrase the human-readable description of status, e.g. "Ok";
 *   if None, the default phrase for the status will be used
 */
final class ResponseHeader(val status: Int, _headers: Map[String, String] = Map.empty, val reasonPhrase: Option[String] = None) {
  private[play] def this(status: Int, _headers: java.util.Map[String, String], reasonPhrase: Option[String]) =
    this(status, collection.JavaConversions.mapAsScalaMap(_headers).toMap, reasonPhrase)

  val headers: Map[String, String] = TreeMap[String, String]()(CaseInsensitiveOrdered) ++ _headers

  def copy(status: Int = status, headers: Map[String, String] = headers, reasonPhrase: Option[String] = reasonPhrase): ResponseHeader =
    new ResponseHeader(status, headers, reasonPhrase)

  override def toString = s"$status, $headers"
  override def hashCode = (status, headers).hashCode
  override def equals(o: Any) = o match {
    case ResponseHeader(s, h, r) => (s, h, r).equals((status, headers, reasonPhrase))
    case _ => false
  }
}
object ResponseHeader {
  val basicDateFormatPattern = "EEE, dd MMM yyyy HH:mm:ss"
  val httpDateFormat: DateTimeFormatter =
    DateTimeFormat.forPattern(basicDateFormatPattern + " 'GMT'")
      .withLocale(java.util.Locale.ENGLISH)
      .withZone(DateTimeZone.UTC)

  def apply(status: Int, headers: Map[String, String] = Map.empty, reasonPhrase: Option[String] = None): ResponseHeader =
    new ResponseHeader(status, headers)
  def unapply(rh: ResponseHeader): Option[(Int, Map[String, String], Option[String])] =
    if (rh eq null) None else Some((rh.status, rh.headers, rh.reasonPhrase))
}

/**
 * A simple result, which defines the response header and a body ready to send to the client.
 *
 * @param header the response header, which contains status code and HTTP headers
 * @param body the response body
 */
case class Result(header: ResponseHeader, body: HttpEntity) {

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
  def withHeaders(headers: (String, String)*): Result = {
    copy(header = header.copy(headers = header.headers ++ headers))
  }

  /**
   * Add a header with a DateTime formatted using the default http date format
   * @param headers the headers with a DateTime to add to this result.
   * @return the new result.
   */
  def withDateHeaders(headers: (String, DateTime)*): Result = {
    copy(header = header.copy(headers = header.headers ++ headers.map {
      case (name, dateTime) => (name, ResponseHeader.httpDateFormat.print(dateTime.getMillis))
    }))
  }

  /**
   * Adds cookies to this result. If the result already contains
   * cookies then the new cookies will be merged with the old cookies.
   *
   * For example:
   * {{{
   * Redirect(routes.Application.index()).withCookies(Cookie("theme", "blue"))
   * }}}
   *
   * @param cookies the cookies to add to this result
   * @return the new result
   */
  def withCookies(cookies: Cookie*): Result = {
    if (cookies.isEmpty) this else {
      withHeaders(SET_COOKIE -> Cookies.mergeSetCookieHeader(header.headers.getOrElse(SET_COOKIE, ""), cookies))
    }
  }

  /**
   * Discards cookies along this result.
   *
   * For example:
   * {{{
   * Redirect(routes.Application.index()).discardingCookies("theme")
   * }}}
   *
   * @param cookies the cookies to discard along to this result
   * @return the new result
   */
  def discardingCookies(cookies: DiscardingCookie*): Result = {
    withHeaders(SET_COOKIE -> Cookies.mergeSetCookieHeader(header.headers.getOrElse(SET_COOKIE, ""), cookies.map(_.toCookie)))
  }

  /**
   * Sets a new session for this result.
   *
   * For example:
   * {{{
   * Redirect(routes.Application.index()).withSession(session + ("saidHello" -> "true"))
   * }}}
   *
   * @param session the session to set with this result
   * @return the new result
   */
  def withSession(session: Session): Result = {
    if (session.isEmpty) discardingCookies(Session.discard) else withCookies(Session.encodeAsCookie(session))
  }

  /**
   * Sets a new session for this result, discarding the existing session.
   *
   * For example:
   * {{{
   * Redirect(routes.Application.index()).withSession("saidHello" -> "yes")
   * }}}
   *
   * @param session the session to set with this result
   * @return the new result
   */
  def withSession(session: (String, String)*): Result = withSession(Session(session.toMap))

  /**
   * Discards the existing session for this result.
   *
   * For example:
   * {{{
   * Redirect(routes.Application.index()).withNewSession
   * }}}
   *
   * @return the new result
   */
  def withNewSession: Result = withSession(Session())

  /**
   * Adds values to the flash scope for this result.
   *
   * For example:
   * {{{
   * Redirect(routes.Application.index()).flashing(flash + ("success" -> "Done!"))
   * }}}
   *
   * @param flash the flash scope to set with this result
   * @return the new result
   */
  def flashing(flash: Flash): Result = {
    if (shouldWarnIfNotRedirect(flash)) {
      logRedirectWarning("flashing")
    }
    withCookies(Flash.encodeAsCookie(flash))
  }

  /**
   * Adds values to the flash scope for this result.
   *
   * For example:
   * {{{
   * Redirect(routes.Application.index()).flashing("success" -> "Done!")
   * }}}
   *
   * @param values the flash values to set with this result
   * @return the new result
   */
  def flashing(values: (String, String)*): Result = flashing(Flash(values.toMap))

  /**
   * Changes the result content type.
   *
   * For example:
   * {{{
   * Ok("<text>Hello world</text>").as("application/xml")
   * }}}
   *
   * @param contentType the new content type.
   * @return the new result
   */
  def as(contentType: String): Result = copy(body = body.as(contentType))

  /**
   * @param request Current request
   * @return The session carried by this result. Reads the request’s session if this result does not modify the session.
   */
  def session(implicit request: RequestHeader): Session =
    Cookies.fromCookieHeader(header.headers.get(SET_COOKIE)).get(Session.COOKIE_NAME) match {
      case Some(cookie) => Session.decodeFromCookie(Some(cookie))
      case None => request.session
    }

  /**
   * Example:
   * {{{
   *   Ok.addingToSession("foo" -> "bar").addingToSession("baz" -> "bah")
   * }}}
   * @param values (key -> value) pairs to add to this result’s session
   * @param request Current request
   * @return A copy of this result with `values` added to its session scope.
   */
  def addingToSession(values: (String, String)*)(implicit request: RequestHeader): Result =
    withSession(new Session(session.data ++ values.toMap))

  /**
   * Example:
   * {{{
   *   Ok.removingFromSession("foo")
   * }}}
   * @param keys Keys to remove from session
   * @param request Current request
   * @return A copy of this result with `keys` removed from its session scope.
   */
  def removingFromSession(keys: String*)(implicit request: RequestHeader): Result =
    withSession(new Session(session.data -- keys))

  override def toString = {
    "Result(" + header + ")"
  }

  /**
   * Returns true if the status code is not 3xx and the application is in Dev mode.
   */
  private def shouldWarnIfNotRedirect(flash: Flash): Boolean = {
    play.api.Play.privateMaybeApplication.exists(app =>
      (app.mode == play.api.Mode.Dev) && (!flash.isEmpty) && (header.status < 300 || header.status > 399))
  }

  /**
   * Logs a redirect warning.
   */
  private def logRedirectWarning(methodName: String) {
    val status = header.status
    play.api.Logger("play").warn(s"You are using status code '$status' with $methodName, which should only be used with a redirect status!")
  }

  /**
   * Convert this result to a Java result.
   */
  def asJava: play.mvc.Result = new play.mvc.Result(header, body.asJava)
}

/**
 * A Codec handle the conversion of String to Byte arrays.
 *
 * @param charset The charset to be sent to the client.
 * @param encode The transformation function.
 */
case class Codec(charset: String)(val encode: String => ByteString, val decode: ByteString => String)

/**
 * Default Codec support.
 */
object Codec {

  /**
   * Create a Codec from an encoding already supported by the JVM.
   */
  def javaSupported(charset: String) = Codec(charset)(str => ByteString.apply(str, charset), bytes => bytes.decodeString(charset))

  /**
   * Codec for UTF-8
   */
  implicit val utf_8 = javaSupported("utf-8")

  /**
   * Codec for ISO-8859-1
   */
  val iso_8859_1 = javaSupported("iso-8859-1")

}

trait LegacyI18nSupport {

  /**
   * Adds convenient methods to handle the client-side language.
   *
   * This class exists only for backward compatibility.
   */
  implicit class ResultWithLang(result: Result)(implicit messagesApi: MessagesApi) {

    /**
     * Sets the user's language permanently for future requests by storing it in a cookie.
     *
     * For example:
     * {{{
     * implicit val lang = Lang("fr-FR")
     * Ok(Messages("hello.world")).withLang(lang)
     * }}}
     *
     * @param lang the language to store for the user
     * @return the new result
     */
    def withLang(lang: Lang): Result =
      messagesApi.setLang(result, lang)

    /**
     * Clears the user's language by discarding the language cookie set by withLang
     *
     * For example:
     * {{{
     * Ok(Messages("hello.world")).clearingLang
     * }}}
     *
     * @return the new result
     */
    def clearingLang: Result =
      messagesApi.clearLang(result)

  }

}

/** Helper utilities to generate results. */
object Results extends Results with LegacyI18nSupport {

  /** Empty result, i.e. nothing to send. */
  case class EmptyContent()

}

/** Helper utilities to generate results. */
trait Results {

  import play.api.http.Status._

  /**
   * Generates default `Result` from a content type, headers and content.
   *
   * @param status the HTTP response status, e.g ‘200 OK’
   */
  class Status(status: Int) extends Result(header = ResponseHeader(status), body = HttpEntity.NoEntity) {

    /**
     * Set the result's content.
     *
     * @param content The content to send.
     */
    def apply[C](content: C)(implicit writeable: Writeable[C]): Result = {
      Result(
        header,
        writeable.toEntity(content)
      )
    }

    private def streamFile(file: Source[ByteString, _], name: String, length: Long, inline: Boolean): Result = {
      Result(
        ResponseHeader(status,
          Map(
            CONTENT_DISPOSITION -> {
              val dispositionType = if (inline) "inline" else "attachment"
              dispositionType + "; filename=\"" + name + "\""
            }
          )
        ),
        HttpEntity.Streamed(
          file,
          Some(length),
          play.api.libs.MimeTypes.forFileName(name).orElse(Some(play.api.http.ContentTypes.BINARY))
        )
      )
    }

    /**
     * Send a file.
     *
     * @param content The file to send.
     * @param inline Use Content-Disposition inline or attachment.
     * @param fileName Function to retrieve the file name. By default the name of the file is used.
     */
    def sendFile(content: java.io.File, inline: Boolean = false, fileName: java.io.File => String = _.getName, onClose: () => Unit = () => ()): Result = {
      streamFile(StreamConverters.fromInputStream(() => Files.newInputStream(content.toPath)), fileName(content), content.length, inline)
    }

    /**
     * Send a file.
     *
     * @param content The file to send.
     * @param inline Use Content-Disposition inline or attachment.
     * @param fileName Function to retrieve the file name. By default the name of the file is used.
     */
    def sendPath(content: Path, inline: Boolean = false, fileName: Path => String = _.getFileName.toString, onClose: () => Unit = () => ()): Result = {
      streamFile(StreamConverters.fromInputStream(() => Files.newInputStream(content)),
        fileName(content), Files.size(content), inline)
    }

    /**
     * Send the given resource from the given classloader.
     *
     * @param resource The path of the resource to load.
     * @param classLoader The classloader to load it from, defaults to the classloader for this class.
     * @param inline Whether it should be served as an inline file, or as an attachment.
     */
    def sendResource(resource: String, classLoader: ClassLoader = Results.getClass.getClassLoader,
      inline: Boolean = true): Result = {
      val stream = classLoader.getResourceAsStream(resource)
      val fileName = resource.split('/').last
      streamFile(StreamConverters.fromInputStream(() => stream), fileName, stream.available(), inline)
    }

    /**
     * Feed the content as the response, using chunked transfer encoding.
     *
     * Chunked transfer encoding is only supported for HTTP 1.1 clients.  If the client is an HTTP 1.0 client, Play will
     * instead return a 505 error code.
     *
     * Chunked encoding allows the server to send a response where the content length is not known, or for potentially
     * infinite streams, while still allowing the connection to be kept alive and reused for the next request.
     *
     * @param content Source providing the content to stream.
     */
    def chunked[C](content: Source[C, _])(implicit writeable: Writeable[C]): Result = {
      Result(
        header = header,
        body = HttpEntity.Chunked(content.map(c => HttpChunk.Chunk(writeable.transform(c))), writeable.contentType)
      )
    }

    /**
     * Feed the content as the response, using chunked transfer encoding.
     *
     * Chunked transfer encoding is only supported for HTTP 1.1 clients.  If the client is an HTTP 1.0 client, Play will
     * instead return a 505 error code.
     *
     * Chunked encoding allows the server to send a response where the content length is not known, or for potentially
     * infinite streams, while still allowing the connection to be kept alive and reused for the next request.
     *
     * @param content Enumerator providing the content to stream.
     */
    @deprecated("Use chunked with an Akka streams Source instead", "2.5.0")
    def chunked[C](content: Enumerator[C])(implicit writeable: Writeable[C]): Result = {
      chunked(Source.fromPublisher(Streams.enumeratorToPublisher(content)))
    }

    /**
     * Feed the content as the response, closing the connection when done.
     *
     * @param content Enumerator providing the content to stream.
     */
    @deprecated("Use sendEntity with a Streamed entity instead", "2.5.0")
    def feed[C](content: Enumerator[C])(implicit writeable: Writeable[C]): Result = {
      Result(
        header = header,
        body = HttpEntity.Streamed(Source.fromPublisher(Streams.enumeratorToPublisher(content)).map(writeable.transform),
          None, writeable.contentType)
      )
    }

    /**
     * Send an HTTP entity with this status.
     */
    def sendEntity(entity: HttpEntity): Result = {
      Result(
        header = header,
        body = entity
      )
    }
  }

  /** Generates a ‘200 OK’ result. */
  val Ok = new Status(OK)

  /** Generates a ‘201 CREATED’ result. */
  val Created = new Status(CREATED)

  /** Generates a ‘202 ACCEPTED’ result. */
  val Accepted = new Status(ACCEPTED)

  /** Generates a ‘203 NON_AUTHORITATIVE_INFORMATION’ result. */
  val NonAuthoritativeInformation = new Status(NON_AUTHORITATIVE_INFORMATION)

  /** Generates a ‘204 NO_CONTENT’ result. */
  val NoContent = Result(header = ResponseHeader(NO_CONTENT), body = HttpEntity.NoEntity)

  /** Generates a ‘205 RESET_CONTENT’ result. */
  val ResetContent = Result(header = ResponseHeader(RESET_CONTENT), body = HttpEntity.NoEntity)

  /** Generates a ‘206 PARTIAL_CONTENT’ result. */
  val PartialContent = new Status(PARTIAL_CONTENT)

  /** Generates a ‘207 MULTI_STATUS’ result. */
  val MultiStatus = new Status(MULTI_STATUS)

  /**
   * Generates a ‘301 MOVED_PERMANENTLY’ simple result.
   *
   * @param url the URL to redirect to
   */
  def MovedPermanently(url: String): Result = Redirect(url, MOVED_PERMANENTLY)

  /**
   * Generates a ‘302 FOUND’ simple result.
   *
   * @param url the URL to redirect to
   */
  def Found(url: String): Result = Redirect(url, FOUND)

  /**
   * Generates a ‘303 SEE_OTHER’ simple result.
   *
   * @param url the URL to redirect to
   */
  def SeeOther(url: String): Result = Redirect(url, SEE_OTHER)

  /** Generates a ‘304 NOT_MODIFIED’ result. */
  val NotModified = Result(header = ResponseHeader(NOT_MODIFIED), body = HttpEntity.NoEntity)

  /**
   * Generates a ‘307 TEMPORARY_REDIRECT’ simple result.
   *
   * @param url the URL to redirect to
   */
  def TemporaryRedirect(url: String): Result = Redirect(url, TEMPORARY_REDIRECT)

  /**
   * Generates a ‘308 PERMANENT_REDIRECT’ simple result.
   *
   * @param url the URL to redirect to
   */
  def PermanentRedirect(url: String): Result = Redirect(url, PERMANENT_REDIRECT)

  /** Generates a ‘400 BAD_REQUEST’ result. */
  val BadRequest = new Status(BAD_REQUEST)

  /** Generates a ‘401 UNAUTHORIZED’ result. */
  val Unauthorized = new Status(UNAUTHORIZED)

  /** Generates a ‘402 PAYMENT_REQUIRED’ result. */
  val PaymentRequired = new Status(PAYMENT_REQUIRED)

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

  /** Generates a ‘429 TOO_MANY_REQUESTS’ result. */
  val TooManyRequests = new Status(TOO_MANY_REQUESTS)

  /** Generates a ‘429 TOO_MANY_REQUEST’ result. */
  @deprecated("Use TooManyRequests instead", "3.0.0")
  val TooManyRequest = TooManyRequests

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
  def Redirect(url: String, status: Int): Result = Redirect(url, Map.empty, status)

  /**
   * Generates a redirect simple result.
   *
   * @param url the URL to redirect to
   * @param queryString queryString parameters to add to the queryString
   * @param status HTTP status for redirect, such as SEE_OTHER, MOVED_TEMPORARILY or MOVED_PERMANENTLY
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
  def Redirect(call: Call): Result = Redirect(call.url)

  /**
   * Generates a redirect simple result.
   *
   * @param call Call defining the URL to redirect to, which typically comes from the reverse router
   * @param status HTTP status for redirect, such as SEE_OTHER, MOVED_TEMPORARILY or MOVED_PERMANENTLY
   */
  def Redirect(call: Call, status: Int): Result = Redirect(call.url, Map.empty, status)

}
