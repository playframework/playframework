/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.security.cert.X509Certificate
import java.util.Locale

import play.api.http.{ HeaderNames, MediaRange, MediaType }
import play.api.i18n.{ Lang, Messages }
import play.api.libs.typedmap.{ TypedKey, TypedMap }
import play.api.mvc.request._

import scala.annotation.implicitNotFound

/**
 * The HTTP request header. Note that it doesn’t contain the request body yet.
 */
@implicitNotFound("Cannot find any HTTP Request Header here")
trait RequestHeader {
  top =>

  /**
   * The remote connection that made the request.
   */
  def connection: RemoteConnection

  def withConnection(newConnection: RemoteConnection): RequestHeader =
    new RequestHeaderImpl(newConnection, method, target, version, headers, attrs)

  /**
   * The request id. The request id is stored as an attribute indexed by [[play.api.mvc.request.RequestAttrKey.Id]].
   */
  final def id: Long = attrs(RequestAttrKey.Id)

  /**
   * The HTTP method.
   */
  def method: String

  /**
   * Return a new copy of the request with its method changed.
   */
  def withMethod(newMethod: String): RequestHeader =
    new RequestHeaderImpl(connection, newMethod, target, version, headers, attrs)

  /**
   * The target of the HTTP request, i.e. the URI or path that was
   * given on the first line of the request.
   */
  def target: RequestTarget

  /**
   * Return a new copy of the request with its target changed.
   */
  def withTarget(newTarget: RequestTarget): RequestHeader =
    new RequestHeaderImpl(connection, method, newTarget, version, headers, attrs)

  /**
   * The complete request URI, containing both path and query string.
   * The URI is what was on the status line after the request method.
   * E.g. in "GET /foo/bar?q=s HTTP/1.1" the URI should be /foo/bar?q=s.
   * It could be absolute, some clients send absolute URLs, especially proxies,
   * e.g. http://www.example.org/foo/bar?q=s.
   *
   * This method delegates to `target.uriString`.
   */
  final def uri: String = target.uriString

  /**
   * The URI path. This method delegates to `target.path`.
   */
  final def path: String = target.path

  /**
   * The HTTP version.
   */
  def version: String

  /**
   * Return a new copy of the request with its HTTP version changed.
   */
  def withVersion(newVersion: String): RequestHeader =
    new RequestHeaderImpl(connection, method, target, newVersion, headers, attrs)

  /**
   * The parsed query string. This method delegates to `target.queryMap`.
   */
  final def queryString: Map[String, Seq[String]] = target.queryMap

  /**
   * The HTTP headers.
   */
  def headers: Headers

  /**
   * The remote connection that made the request.
   */
  def withHeaders(newHeaders: Headers): RequestHeader =
    new RequestHeaderImpl(connection, method, target, version, newHeaders, attrs)

  /**
   * The client IP address.
   *
   * retrieves the last untrusted proxy
   * from the Forwarded-Headers or the X-Forwarded-*-Headers.
   *
   * This method delegates to `connection.remoteAddressString`.
   */
  final def remoteAddress: String = connection.remoteAddressString

  /**
   * Is the client using SSL? This method delegates to `connection.secure`.
   */
  final def secure: Boolean = connection.secure

  /**
   * The X509 certificate chain presented by a client during SSL requests.  This method is
   * equivalent to `connection.clientCertificateChain`.
   */
  final def clientCertificateChain: Option[Seq[X509Certificate]] = connection.clientCertificateChain

  /**
   * A map of typed attributes associated with the request.
   */
  def attrs: TypedMap

  /**
   * Create a new version of this object with the given attributes attached to it.
   * This replaces any existing attributes.
   *
   * @param newAttrs The new attributes to add.
   * @return The new version of this object with the attributes attached.
   */
  def withAttrs(newAttrs: TypedMap): RequestHeader =
    new RequestHeaderImpl(connection, method, target, version, headers, newAttrs)

  /**
   * Create a new versions of this object with the given attribute attached to it.
   *
   * @param key The new attribute key.
   * @param value  The attribute value.
   * @tparam A The type of value.
   * @return The new version of this object with the new attribute.
   */
  def addAttr[A](key: TypedKey[A], value: A): RequestHeader =
    withAttrs(attrs.updated(key, value))

  /**
   * Create a new versions of this object with the given attribute removed.
   *
   * @param key The key of the attribute to remove.
   * @return The new version of this object with the attribute removed.
   */
  def removeAttr(key: TypedKey[_]): RequestHeader =
    withAttrs(attrs - key)

  // -- Computed

  /**
   * Helper method to access a queryString parameter. This method delegates to `connection.getQueryParameter(key)`.
   *
   * @return The query parameter's value if the parameter is present
   *         and there is only one value. If the parameter is absent
   *         or there is more than one value for that parameter then
   *         `None` is returned.
   */
  def getQueryString(key: String): Option[String] = target.getQueryParameter(key)

  /**
   * True if this request has a body, so we know if we should trigger body parsing. The base implementation simply
   * checks for the Content-Length or Transfer-Encoding headers, but subclasses (such as fake requests) may return
   * true in other cases so the headers need not be updated to reflect the body.
   */
  def hasBody: Boolean = headers.hasBody

  /**
   * The HTTP host (domain, optionally port). This value is derived from the request target, if a hostname is present.
   * If the target doesn't have a host then the `Host` header is used, if present. If that's not present then an
   * empty string is returned.
   */
  lazy val host: String = {
    import RequestHeader.AbsoluteUri
    uri match {
      case AbsoluteUri(proto, hostPort, rest) => hostPort
      case _ => headers.get(HeaderNames.HOST).getOrElse("")
    }
  }

  /**
   * The HTTP domain. The domain part of the request's [[host]].
   */
  lazy val domain: String = host.split(':').head

  /**
   * The Request Langs extracted from the Accept-Language header and sorted by preference (preferred first).
   */
  lazy val acceptLanguages: Seq[play.api.i18n.Lang] = {
    val langs = RequestHeader.acceptHeader(headers, HeaderNames.ACCEPT_LANGUAGE).map(item => (item._1, Lang.get(item._2)))
    langs.sortWith((a, b) => a._1 > b._1).flatMap(_._2)
  }

  /**
   * @return The media types list of the request’s Accept header, sorted by preference (preferred first).
   */
  lazy val acceptedTypes: Seq[play.api.http.MediaRange] = {
    headers.get(HeaderNames.ACCEPT).toSeq.flatMap(MediaRange.parse.apply)
  }

  /**
   * Check if this request accepts a given media type.
   *
   * @return true if `mimeType` matches the Accept header, otherwise false
   */
  def accepts(mimeType: String): Boolean = {
    acceptedTypes.isEmpty || acceptedTypes.exists(_.accepts(mimeType))
  }

  /**
   * The HTTP cookies. The request's cookies are stored in an attribute indexed by
   * [[play.api.mvc.request.RequestAttrKey.Cookies]]. The attribute uses a Cell to store the cookies,
   * to allow them to be evaluated on-demand.
   */
  def cookies: Cookies = attrs(RequestAttrKey.Cookies).value

  /**
   * Parses the `Session` cookie and returns the `Session` data. The request's session cookie is stored in an attribute indexed by
   * [[play.api.mvc.request.RequestAttrKey.Session]]. The attribute uses a [[play.api.mvc.request.Cell]] to store the session cookie, to allow it to be evaluated on-demand.
   */
  def session: Session = attrs(RequestAttrKey.Session).value

  /**
   * Parses the `Flash` cookie and returns the `Flash` data. The request's flash cookie is stored in an attribute indexed by
   * [[play.api.mvc.request.RequestAttrKey.Flash]]. The attribute uses a [[play.api.mvc.request.Cell]] to store the flash, to allow it to be evaluated on-demand.
   */
  def flash: Flash = attrs(RequestAttrKey.Flash).value

  /**
   * Returns the raw query string. This method delegates to `connection.rawQueryString`.
   */
  def rawQueryString: String = target.queryString

  /**
   * The media type of this request.  Same as contentType, except returns a fully parsed media type with parameters.
   */
  lazy val mediaType: Option[MediaType] = headers.get(HeaderNames.CONTENT_TYPE).flatMap(MediaType.parse.apply)

  /**
   * Returns the value of the Content-Type header (without the parameters (eg charset))
   */
  lazy val contentType: Option[String] = mediaType.map(mt => mt.mediaType + "/" + mt.mediaSubType)

  /**
   * Returns the charset of the request for text-based body
   */
  lazy val charset: Option[String] = for {
    mt <- mediaType
    param <- mt.parameters.find(_._1.equalsIgnoreCase("charset"))
    charset <- param._2
  } yield charset

  /**
   * Attach a body to this header.
   *
   * @param body The body to attach.
   * @tparam A The type of the body.
   * @return A new request with the body attached to the header.
   */
  def withBody[A](body: A): Request[A] =
    new RequestImpl[A](connection, method, target, version, headers, attrs, body)

  /**
   * Create a new versions of this object with the given transient language set.
   * The transient language will be taken into account when using [[play.api.i18n.MessagesApi.preferred()]] (It will take precedence over any other language).
   *
   * @param lang The language to use.
   * @return The new version of this object with the given transient language set.
   */
  def withTransientLang(lang: Lang): RequestHeader =
    addAttr(Messages.Attrs.CurrentLang, lang)

  /**
   * Create a new versions of this object with the given transient language set.
   * The transient language will be taken into account when using [[play.api.i18n.MessagesApi.preferred()]] (It will take precedence over any other language).
   *
   * @param code The language to use.
   * @return The new version of this object with the given transient language set.
   */
  def withTransientLang(code: String): RequestHeader =
    withTransientLang(Lang(code))

  /**
   * Create a new versions of this object with the given transient language set.
   * The transient language will be taken into account when using [[play.api.i18n.MessagesApi.preferred()]] (It will take precedence over any other language).
   *
   * @param locale The language to use.
   * @return The new version of this object with the given transient language set.
   */
  def withTransientLang(locale: Locale): RequestHeader =
    withTransientLang(Lang(locale))

  /**
   * Create a new versions of this object with the given transient language removed.
   *
   * @return The new version of this object with the transient language removed.
   */
  def withoutTransientLang(): RequestHeader =
    removeAttr(Messages.Attrs.CurrentLang)

  /**
   * The transient language will be taken into account when using [[play.api.i18n.MessagesApi.preferred()]] (It will take precedence over any other language).
   *
   * @return The current transient language of this request.
   */
  def transientLang(): Option[Lang] =
    attrs.get(Messages.Attrs.CurrentLang)

  override def toString: String = {
    method + " " + uri
  }

  def asJava: play.mvc.Http.RequestHeader = new play.core.j.RequestHeaderImpl(this)
}

object RequestHeader {
  private val AbsoluteUri = """(?is)^(https?)://([^/]+)(/.*|$)""".r

  // “The first "q" parameter (if any) separates the media-range parameter(s) from the accept-params.”
  val qPattern = ";\\s*q=([0-9.]+)".r

  /**
   * @return The items of an Accept* header, with their q-value.
   */
  private[play] def acceptHeader(headers: Headers, headerName: String): Seq[(Double, String)] = {
    for {
      header <- headers.get(headerName).toList
      value0 <- header.split(',')
      value = value0.trim
    } yield {
      RequestHeader.qPattern.findFirstMatchIn(value) match {
        case Some(m) => (m.group(1).toDouble, m.before.toString)
        case None => (1.0, value) // “The default value is q=1.”
      }
    }
  }
}

/**
 * A standard implementation of a RequestHeader.
 */
private[play] class RequestHeaderImpl(
    override val connection: RemoteConnection,
    override val method: String,
    override val target: RequestTarget,
    override val version: String,
    override val headers: Headers,
    override val attrs: TypedMap) extends RequestHeader
