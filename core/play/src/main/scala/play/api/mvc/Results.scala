/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.lang.{ StringBuilder => JStringBuilder }
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import java.time.ZonedDateTime

import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.StreamConverters
import akka.util.ByteString
import play.api.http.HeaderNames._
import play.api.http.FileMimeTypes
import play.api.http._
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.Logger
import play.api.Mode
import play.core.utils.CaseInsensitiveOrdered
import play.core.utils.HttpHeaderParameterEncoding

import scala.collection.JavaConverters._
import scala.collection.immutable.TreeMap
import scala.concurrent.ExecutionContext

/**
 * A simple HTTP response header, used for standard responses.
 *
 * @param status the response status, e.g. 200
 * @param _headers the HTTP headers
 * @param reasonPhrase the human-readable description of status, e.g. "Ok";
 *   if None, the default phrase for the status will be used
 */
final class ResponseHeader(
    val status: Int,
    _headers: Map[String, String] = Map.empty,
    val reasonPhrase: Option[String] = None
) {
  private[play] def this(status: Int, _headers: java.util.Map[String, String], reasonPhrase: Option[String]) =
    this(status, _headers.asScala.toMap, reasonPhrase)

  val headers: Map[String, String] = TreeMap[String, String]()(CaseInsensitiveOrdered) ++ _headers

  // validate headers so we know this response header is well formed
  for ((name, value) <- headers) {
    if (name eq null) throw new NullPointerException("Response header names cannot be null!")
    if (value eq null) throw new NullPointerException(s"Response header '$name' has null value!")
  }

  def copy(
      status: Int = status,
      headers: Map[String, String] = headers,
      reasonPhrase: Option[String] = reasonPhrase
  ): ResponseHeader =
    new ResponseHeader(status, headers, reasonPhrase)

  override def toString = s"$status, $headers"
  override def hashCode = (status, headers).hashCode
  override def equals(o: Any) = o match {
    case ResponseHeader(s, h, r) => (s, h, r).equals((status, headers, reasonPhrase))
    case _                       => false
  }

  def asJava: play.mvc.ResponseHeader = {
    new play.mvc.ResponseHeader(status, headers.asJava, reasonPhrase.orNull)
  }

  /**
   * INTERNAL API
   *
   * Appends to the comma-separated `Vary` header of this request
   */
  private[play] def varyWith(headerValues: String*): (String, String) = {
    val newValue = headers.get(VARY) match {
      case Some(existing) if existing.nonEmpty =>
        val existingSet: Set[String] = existing.split(",").iterator.map(_.trim.toLowerCase).toSet
        val newValuesToAdd           = headerValues.filterNot(v => existingSet.contains(v.trim.toLowerCase))
        s"$existing${newValuesToAdd.map(v => s",$v").mkString}"
      case _ =>
        headerValues.mkString(",")
    }
    VARY -> newValue
  }
}

object ResponseHeader {
  val basicDateFormatPattern = "EEE, dd MMM yyyy HH:mm:ss"
  val httpDateFormat: DateTimeFormatter =
    DateTimeFormatter
      .ofPattern(basicDateFormatPattern + " 'GMT'")
      .withLocale(java.util.Locale.ENGLISH)
      .withZone(ZoneOffset.UTC)

  def apply(
      status: Int,
      headers: Map[String, String] = Map.empty,
      reasonPhrase: Option[String] = None
  ): ResponseHeader =
    new ResponseHeader(status, headers)
  def unapply(rh: ResponseHeader): Option[(Int, Map[String, String], Option[String])] =
    if (rh eq null) None else Some((rh.status, rh.headers, rh.reasonPhrase))
}

object Result {

  /**
   * Logs a redirect warning for flashing (in dev mode) if the status code is not 3xx
   */
  @inline def warnFlashingIfNotRedirect(flash: Flash, header: ResponseHeader): Unit = {
    if (!flash.isEmpty && !Status.isRedirect(header.status)) {
      Logger("play")
        .forMode(Mode.Dev)
        .warn(
          s"You are using status code '${header.status}' with flashing, which should only be used with a redirect status!"
        )
    }
  }
}

/**
 * A simple result, which defines the response header and a body ready to send to the client.
 *
 * @param header the response header, which contains status code and HTTP headers
 * @param body the response body
 */
case class Result(
    header: ResponseHeader,
    body: HttpEntity,
    newSession: Option[Session] = None,
    newFlash: Option[Flash] = None,
    newCookies: Seq[Cookie] = Seq.empty
) {

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
   *
   * @param headers the headers with a DateTime to add to this result.
   * @return the new result.
   */
  def withDateHeaders(headers: (String, ZonedDateTime)*): Result = {
    copy(header = header.copy(headers = header.headers ++ headers.map {
      case (name, dateTime) => (name, dateTime.format(ResponseHeader.httpDateFormat))
    }))
  }

  /**
   * Discards headers to this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").discardingHeader(ETAG)
   * }}}
   *
   * @param name the header to discard from this result.
   * @return the new result
   */
  def discardingHeader(name: String): Result = {
    copy(header = header.copy(headers = header.headers - name))
  }

  /**
   * Adds cookies to this result. If the result already contains cookies then cookies with the same name in the new
   * list will override existing ones.
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
    val filteredCookies = newCookies.filter(cookie => !cookies.exists(_.name == cookie.name))
    if (cookies.isEmpty) this else copy(newCookies = filteredCookies ++ cookies)
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
    withCookies(cookies.map(_.toCookie): _*)
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
  def withSession(session: Session): Result = copy(newSession = Some(session))

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
    Result.warnFlashingIfNotRedirect(flash, header)
    copy(newFlash = Some(flash))
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
  def session(implicit request: RequestHeader): Session = newSession.getOrElse(request.session)

  /**
   * Example:
   * {{{
   *   Ok.addingToSession("foo" -> "bar").addingToSession("baz" -> "bah")
   * }}}
   *
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
   *
   * @param keys Keys to remove from session
   * @param request Current request
   * @return A copy of this result with `keys` removed from its session scope.
   */
  def removingFromSession(keys: String*)(implicit request: RequestHeader): Result =
    withSession(new Session(session.data -- keys))

  override def toString = s"Result(${header})"

  /**
   * Convert this result to a Java result.
   */
  def asJava: play.mvc.Result =
    new play.mvc.Result(
      header.asJava,
      body.asJava,
      newSession.map(_.asJava).orNull,
      newFlash.map(_.asJava).orNull,
      newCookies.map(_.asJava).asJava
    )

  /**
   * Encode the cookies into the Set-Cookie header. The session is always baked first, followed by the flash cookie,
   * followed by all the other cookies in order.
   */
  def bakeCookies(
      cookieHeaderEncoding: CookieHeaderEncoding = new DefaultCookieHeaderEncoding(),
      sessionBaker: CookieBaker[Session] = new DefaultSessionCookieBaker(),
      flashBaker: CookieBaker[Flash] = new DefaultFlashCookieBaker(),
      requestHasFlash: Boolean = false
  ): Result = {
    val allCookies = {
      val setCookieCookies = cookieHeaderEncoding.decodeSetCookieHeader(header.headers.getOrElse(SET_COOKIE, ""))
      val session = newSession.map { data =>
        if (data.isEmpty) sessionBaker.discard.toCookie else sessionBaker.encodeAsCookie(data)
      }
      val flash = newFlash
        .map { data =>
          if (data.isEmpty) flashBaker.discard.toCookie else flashBaker.encodeAsCookie(data)
        }
        .orElse {
          if (requestHasFlash) Some(flashBaker.discard.toCookie) else None
        }
      setCookieCookies ++ session ++ flash ++ newCookies
    }

    if (allCookies.isEmpty) {
      this
    } else {
      withHeaders(SET_COOKIE -> cookieHeaderEncoding.encodeSetCookieHeader(allCookies))
    }
  }
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
  def javaSupported(charset: String) =
    Codec(charset)(str => ByteString.apply(str, charset), bytes => bytes.decodeString(charset))

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
     * Ok(Messages("hello.world")).withoutLang
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
  private[mvc] final val logger = Logger(getClass)

  /** Empty result, i.e. nothing to send. */
  case class EmptyContent()

  /**
   * Encodes and adds the query params to the given url
   *
   * @param url
   * @param queryStringParams
   * @return
   */
  private[play] def addQueryStringParams(url: String, queryStringParams: Map[String, Seq[String]]): String = {
    if (queryStringParams.isEmpty) {
      url
    } else {
      val queryString: String = queryStringParams
        .flatMap {
          case (key, values) =>
            val encodedKey = URLEncoder.encode(key, "utf-8")
            values.map(value => s"$encodedKey=${URLEncoder.encode(value, "utf-8")}")
        }
        .mkString("&")

      url + (if (url.contains("?")) "&" else "?") + queryString
    }
  }

  /**
   * Creates a {@code Content-Disposition} header.<br>
   * According to RFC 6266 (Section 4.2) there is no need to send the header {@code "Content-Disposition: inline"}.
   * Therefore if the header generated by this method ends up being exactly that header (when passing {@code inline = true}
   * and {@code None} as {@code name}), an empty Map ist returned.
   *
   * @param inline If the content should be rendered inline or as attachment.
   * @param name The name of the resource, usually displayed in a file download dialog.
   * @return a map with a {@code Content-Disposition} header entry or an empty map if explained conditions apply.
   * @see [[https://tools.ietf.org/html/rfc6266#section-4.2]]
   */
  def contentDispositionHeader(inline: Boolean, name: Option[String]): Map[String, String] =
    if (!inline || name.exists(_.nonEmpty))
      Map(
        CONTENT_DISPOSITION -> {
          val builder = new JStringBuilder
          builder.append(if (inline) "inline" else "attachment")
          name.foreach(filename => {
            builder.append("; ")
            HttpHeaderParameterEncoding.encodeToBuilder("filename", filename, builder)
          })
          builder.toString
        }
      )
    else Map.empty
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

    private def streamFile(file: Source[ByteString, _], name: Option[String], length: Option[Long], inline: Boolean)(
        implicit fileMimeTypes: FileMimeTypes
    ): Result = {
      Result(
        ResponseHeader(
          status,
          Results.contentDispositionHeader(inline, name)
        ),
        HttpEntity.Streamed(
          file,
          length,
          name.flatMap(fileMimeTypes.forFileName).orElse(Some(play.api.http.ContentTypes.BINARY))
        )
      )
    }

    /**
     * Send a file.
     *
     * @param content The file to send.
     * @param inline Use Content-Disposition inline or attachment.
     * @param fileName Function to retrieve the file name rendered in the {@code Content-Disposition} header. By default the name
     *      of the file is used. The response will also automatically include the MIME type in the {@code Content-Type} header
     *      deducing it from this file name if the {@code implicit fileMimeTypes} includes it or fallback to {@code application/octet-stream}
     *      if unknown.
     * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file generated for a download).
     */
    def sendFile(
        content: java.io.File,
        inline: Boolean = true,
        fileName: java.io.File => Option[String] = Option(_).map(_.getName),
        onClose: () => Unit = () => ()
    )(implicit ec: ExecutionContext, fileMimeTypes: FileMimeTypes): Result = {
      sendPath(content.toPath, inline, (p: Path) => fileName(p.toFile), onClose)
    }

    /**
     * Send a path.
     *
     * @param content The path to send.
     * @param inline Use Content-Disposition inline or attachment.
     * @param fileName Function to retrieve the file name rendered in the {@code Content-Disposition} header. By default the name
     *      of the file is used. The response will also automatically include the MIME type in the {@code Content-Type} header
     *      deducing it from this file name if the {@code implicit fileMimeTypes} includes it or fallback to {@code application/octet-stream}
     *      if unknown.
     * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file generated for a download).
     */
    def sendPath(
        content: Path,
        inline: Boolean = true,
        fileName: Path => Option[String] = Option(_).map(_.getFileName.toString),
        onClose: () => Unit = () => ()
    )(implicit ec: ExecutionContext, fileMimeTypes: FileMimeTypes): Result = {
      val io = FileIO
        .fromPath(content)
        .mapMaterializedValue(_.onComplete { _ =>
          onClose()
        })
      streamFile(io, fileName(content), Some(Files.size(content)), inline)
    }

    /**
     * Send the given resource from the given classloader.
     *
     * @param resource The path of the resource to load.
     * @param classLoader The classloader to load it from, defaults to the classloader for this class.
     * @param inline Whether it should be served as an inline file, or as an attachment.
     * @param fileName Function to retrieve the file name rendered in the {@code Content-Disposition} header. By default the name
     *      of the file is used. The response will also automatically include the MIME type in the {@code Content-Type} header
     *      deducing it from this file name if the {@code implicit fileMimeTypes} includes it or fallback to {@code application/octet-stream}
     *      if unknown.
     * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file generated for a download).
     */
    def sendResource(
        resource: String,
        classLoader: ClassLoader = Results.getClass.getClassLoader,
        inline: Boolean = true,
        fileName: String => Option[String] = Option(_).map(_.split('/').last),
        onClose: () => Unit = () => ()
    )(implicit ec: ExecutionContext, fileMimeTypes: FileMimeTypes): Result = {
      val stream = classLoader.getResourceAsStream(resource)
      val io = StreamConverters
        .fromInputStream(() => stream)
        .mapMaterializedValue(_.onComplete { _ =>
          onClose()
        })
      streamFile(io, fileName(resource), Some(stream.available()), inline)
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
     * @param contentType an optional content type.
     */
    def chunked[C](content: Source[C, _], contentType: Option[String] = None)(
        implicit writeable: Writeable[C]
    ): Result = {
      Result(
        header = header,
        body = HttpEntity
          .Chunked(content.map(c => HttpChunk.Chunk(writeable.transform(c))), contentType.orElse(writeable.contentType))
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
     * @param content Source providing the content to stream.
     * @param inline If the content should be rendered inline or as attachment.
     * @param fileName Function to retrieve the file name rendered in the {@code Content-Disposition} header. By default the name
     *      of the file is used. The response will also automatically include the MIME type in the {@code Content-Type} header
     *      deducing it from this file name if the {@code implicit fileMimeTypes} includes it or fallback to the content-type of the
     *      {@code implicit writeable} if unknown.
     */
    def chunked[C](content: Source[C, _], inline: Boolean, fileName: Option[String])(
        implicit writeable: Writeable[C],
        fileMimeTypes: FileMimeTypes
    ): Result = {
      Result(
        header = header.copy(headers = header.headers ++ Results.contentDispositionHeader(inline, fileName)),
        body = HttpEntity.Chunked(
          content.map(c => HttpChunk.Chunk(writeable.transform(c))),
          fileName.flatMap(fileMimeTypes.forFileName).orElse(writeable.contentType)
        )
      )
    }

    /**
     * Feed the content as the response, using a streamed entity.
     *
     * It will use the given Content-Type, but if is not present, then it fallsback
     * to use the [[Writeable]] contentType.
     *
     * @param content Source providing the content to stream.
     * @param contentLength an optional content length.
     * @param contentType an optional content type.
     */
    def streamed[C](content: Source[C, _], contentLength: Option[Long], contentType: Option[String] = None)(
        implicit writeable: Writeable[C]
    ): Result = {
      Result(
        header = header,
        body = HttpEntity
          .Streamed(content.map(c => writeable.transform(c)), contentLength, contentType.orElse(writeable.contentType))
      )
    }

    /**
     * Feed the content as the response, using a streamed entity.
     *
     * It will use the given Content-Type, but if is not present, then it fallsback
     * to use the [[Writeable]] contentType.
     *
     * @param content Source providing the content to stream.
     * @param contentLength an optional content length.
     * @param inline If the content should be rendered inline or as attachment.
     * @param fileName Function to retrieve the file name rendered in the {@code Content-Disposition} header. By default the name
     *      of the file is used. The response will also automatically include the MIME type in the {@code Content-Type} header
     *      deducing it from this file name if the {@code implicit fileMimeTypes} includes it or fallback to the content-type of the
     *      {@code implicit writeable} if unknown.
     */
    def streamed[C](content: Source[C, _], contentLength: Option[Long], inline: Boolean, fileName: Option[String])(
        implicit writeable: Writeable[C],
        fileMimeTypes: FileMimeTypes
    ): Result = {
      Result(
        header = header.copy(headers = header.headers ++ Results.contentDispositionHeader(inline, fileName)),
        body = HttpEntity.Streamed(
          content.map(c => writeable.transform(c)),
          contentLength,
          fileName.flatMap(fileMimeTypes.forFileName).orElse(writeable.contentType)
        )
      )
    }

    /**
     * Send an HTTP entity with this status.
     *
     * @param entity The entity to send.
     */
    def sendEntity(entity: HttpEntity): Result = {
      Result(
        header = header,
        body = entity
      )
    }

    /**
     * Send an HTTP entity with this status.
     *
     * @param entity The entity to send.
     * @param inline If the content should be rendered inline or as attachment.
     * @param fileName Function to retrieve the file name rendered in the {@code Content-Disposition} header. By default the name
     *      of the file is used. The response will also automatically include the MIME type in the {@code Content-Type} header
     *      deducing it from this file name if the {@code implicit fileMimeTypes} includes it or fallback to {@code application/octet-stream}
     *      if unknown.
     */
    def sendEntity(entity: HttpEntity, inline: Boolean, fileName: Option[String])(
        implicit fileMimeTypes: FileMimeTypes
    ): Result = {
      Result(
        header = header.copy(headers = header.headers ++ Results.contentDispositionHeader(inline, fileName)),
        body = entity
      ).as(fileName.flatMap(fileMimeTypes.forFileName).getOrElse(play.api.http.ContentTypes.BINARY))
    }
  }

  /** Generates a ‘100 Continue’ result. */
  val Continue = Result(header = ResponseHeader(CONTINUE), body = HttpEntity.NoEntity)

  /** Generates a ‘101 Switching Protocols’ result. */
  val SwitchingProtocols = Result(header = ResponseHeader(SWITCHING_PROTOCOLS), body = HttpEntity.NoEntity)

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

  /** Generates a ‘418 IM_A_TEAPOT’ result. */
  val ImATeapot = new Status(IM_A_TEAPOT)

  /** Generates a ‘422 UNPROCESSABLE_ENTITY’ result. */
  val UnprocessableEntity = new Status(UNPROCESSABLE_ENTITY)

  /** Generates a ‘423 LOCKED’ result. */
  val Locked = new Status(LOCKED)

  /** Generates a ‘424 FAILED_DEPENDENCY’ result. */
  val FailedDependency = new Status(FAILED_DEPENDENCY)

  /** Generates a ‘428 PRECONDITION_REQUIRED’ result. */
  val PreconditionRequired = new Status(PRECONDITION_REQUIRED)

  /** Generates a ‘429 TOO_MANY_REQUESTS’ result. */
  val TooManyRequests = new Status(TOO_MANY_REQUESTS)

  /** Generates a ‘431 REQUEST_HEADER_FIELDS_TOO_LARGE’ result. */
  val RequestHeaderFieldsTooLarge = new Status(REQUEST_HEADER_FIELDS_TOO_LARGE)

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

  /** Generates a ‘511 NETWORK_AUTHENTICATION_REQUIRED’ result. */
  val NetworkAuthenticationRequired = new Status(NETWORK_AUTHENTICATION_REQUIRED)

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
   * @param statusCode HTTP status
   */
  def Redirect(url: String, statusCode: Int): Result = Redirect(url, Map.empty, statusCode)

  /**
   * Generates a redirect simple result.
   *
   * @param url the URL to redirect to
   * @param queryStringParams queryString parameters to add to the queryString
   * @param status HTTP status for redirect, such as SEE_OTHER, MOVED_TEMPORARILY or MOVED_PERMANENTLY
   */
  def Redirect(url: String, queryStringParams: Map[String, Seq[String]] = Map.empty, status: Int = SEE_OTHER) = {
    if (!play.api.http.Status.isRedirect(status)) {
      Results.logger
        .forMode(Mode.Dev)
        .warn(s"You are using status code $status which is not a redirect code!")
    }
    val fullUrl: String = Results.addQueryStringParams(url, queryStringParams)
    Status(status).withHeaders(LOCATION -> fullUrl)
  }

  /**
   * Generates a redirect simple result.
   *
   * @param call Call defining the URL to redirect to, which typically comes from the reverse router
   */
  def Redirect(call: Call): Result = Redirect(call.path)

  /**
   * Generates a redirect simple result.
   *
   * @param call Call defining the URL to redirect to, which typically comes from the reverse router
   * @param status HTTP status for redirect, such as SEE_OTHER, MOVED_TEMPORARILY or MOVED_PERMANENTLY
   */
  def Redirect(call: Call, status: Int): Result = Redirect(call.path, Map.empty, status)
}
