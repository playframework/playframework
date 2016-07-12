/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import java.security.cert.X509Certificate

import play.api.http.{ HeaderNames, MediaRange, MediaType }
import play.api.i18n.Lang
import play.api.libs.typedmap.{ TypedEntry, TypedKey, TypedMap }

import scala.annotation.implicitNotFound

/**
 * The HTTP request header. Note that it doesn’t contain the request body yet.
 */
@implicitNotFound("Cannot find any HTTP Request Header here")
trait RequestHeader {

  /**
   * The request ID.
   */
  def id: Long

  /**
   * The request Tags.
   */
  def tags: Map[String, String]

  /**
   * The complete request URI, containing both path and query string.
   * The URI is what was on the status line after the request method.
   * E.g. in "GET /foo/bar?q=s HTTP/1.1" the URI should be /foo/bar?q=s.
   * It could be absolute, some clients send absolute URLs, especially proxies.
   */
  def uri: String

  /**
   * The URI path.
   */
  def path: String

  /**
   * The HTTP method.
   */
  def method: String

  /**
   * The HTTP version.
   */
  def version: String

  /**
   * The parsed query string.
   */
  def queryString: Map[String, Seq[String]]

  /**
   * The HTTP headers.
   */
  def headers: Headers

  /**
   * The client IP address.
   *
   * retrieves the last untrusted proxy
   * from the Forwarded-Headers or the X-Forwarded-*-Headers.
   *
   *
   */
  def remoteAddress: String

  /**
   * Is the client using SSL?
   */
  def secure: Boolean

  /**
   * The X509 certificate chain presented by a client during SSL requests.
   */
  def clientCertificateChain: Option[Seq[X509Certificate]]

  def apply[A](key: TypedKey[A]): A
  def get[A](key: TypedKey[A]): Option[A]
  def updated[A](key: TypedKey[A], value: A): RequestHeader
  def contains(key: TypedKey[_]): Boolean
  def +(entry: TypedEntry[_]): RequestHeader
  def +(entry: TypedEntry[_], entries: TypedEntry[_]*): RequestHeader

  // -- Computed

  /**
   * Helper method to access a queryString parameter.
   */
  def getQueryString(key: String): Option[String] = queryString.get(key).flatMap(_.headOption)

  /**
   * True if this request has a body, so we know if we should trigger body parsing. The base implementation simply
   * checks for the Content-Length or Transfer-Encoding headers, but subclasses (such as fake requests) may return
   * true in other cases so the headers need not be updated to reflect the body.
   */
  def hasBody: Boolean = {
    import HeaderNames._
    headers.get(CONTENT_LENGTH).isDefined || headers.get(TRANSFER_ENCODING).isDefined
  }

  /**
   * The HTTP host (domain, optionally port)
   */
  lazy val host: String = {
    val AbsoluteUri = """(?is)^(https?)://([^/]+)(/.*|$)""".r
    uri match {
      case AbsoluteUri(proto, hostPort, rest) => hostPort
      case _ => headers.get(HeaderNames.HOST).getOrElse("")
    }
  }

  /**
   * The HTTP domain
   */
  lazy val domain: String = host.split(':').head

  /**
   * The Request Langs extracted from the Accept-Language header and sorted by preference (preferred first).
   */
  lazy val acceptLanguages: Seq[play.api.i18n.Lang] = {
    val langs = RequestHeader.acceptHeader(headers, HeaderNames.ACCEPT_LANGUAGE).map(item => (item._1, Lang.get(item._2)))
    langs.sortWith((a, b) => a._1 > b._1).map(_._2).flatten
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
    acceptedTypes.isEmpty || acceptedTypes.find(_.accepts(mimeType)).isDefined
  }

  /**
   * The HTTP cookies.
   */
  lazy val cookies: Cookies = Cookies.fromCookieHeader(headers.get(play.api.http.HeaderNames.COOKIE))

  /**
   * Parses the `Session` cookie and returns the `Session` data.
   */
  lazy val session: Session = Session.decodeFromCookie(cookies.get(Session.COOKIE_NAME))

  /**
   * Parses the `Flash` cookie and returns the `Flash` data.
   */
  lazy val flash: Flash = Flash.decodeFromCookie(cookies.get(Flash.COOKIE_NAME))

  /**
   * Returns the raw query string.
   */
  lazy val rawQueryString: String = uri.split('?').drop(1).mkString("?")

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
   * Convenience method for adding a single tag to this request
   *
   * @return the tagged request
   */
  def withTag(tagName: String, tagValue: String): RequestHeader = {
    copy(tags = tags + (tagName -> tagValue))
  }

  /**
   * Copy the request.
   */
  def copy(
    id: Long = this.id,
    tags: Map[String, String] = this.tags,
    uri: String = this.uri,
    path: String = this.path,
    method: String = this.method,
    version: String = this.version,
    queryString: Map[String, Seq[String]] = this.queryString,
    headers: Headers = this.headers,
    remoteAddress: => String = this.remoteAddress,
    secure: => Boolean = this.secure,
    clientCertificateChain: Option[Seq[X509Certificate]] = this.clientCertificateChain): RequestHeader = {
    new RequestHeaderImpl(
      id = id,
      tags = tags,
      uri = uri,
      path = path,
      method = method,
      version = version,
      queryString = queryString,
      headers = headers,
      remoteAddressFunc = () => remoteAddress,
      secureFunc = () => secure,
      clientCertificateChain = clientCertificateChain,
      properties = TypedMap.empty
    )
  }

  override def toString = {
    method + " " + uri
  }

}

object RequestHeader {
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
 *
 * @param remoteAddressFunc A function that evaluates to the remote address.
 * @param secureFunc A function that evaluates to the security status.
 * @param properties A map of the RequestHeader's typed properties.
 */
private[play] class RequestHeaderImpl(
    override val id: Long,
    override val tags: Map[String, String],
    override val uri: String,
    override val path: String,
    override val method: String,
    override val version: String,
    override val queryString: Map[String, Seq[String]],
    override val headers: Headers,
    remoteAddressFunc: () => String,
    secureFunc: () => Boolean,
    override val clientCertificateChain: Option[Seq[X509Certificate]],
    override protected val properties: TypedMap) extends RequestHeader with WithPropertiesMap[RequestHeader] {

  def this(
    id: Long,
    tags: Map[String, String],
    uri: String,
    path: String,
    method: String,
    version: String,
    queryString: Map[String, Seq[String]],
    headers: Headers,
    remoteAddress: String,
    secure: Boolean,
    clientCertificateChain: Option[Seq[X509Certificate]],
    properties: TypedMap) = {
    this(
      id = id,
      tags = tags,
      uri = uri,
      path = path,
      method = method,
      version = version,
      queryString = queryString,
      headers = headers,
      remoteAddressFunc = () => remoteAddress,
      secureFunc = () => secure,
      clientCertificateChain = clientCertificateChain,
      properties = properties
    )
  }

  override lazy val remoteAddress: String = remoteAddressFunc()
  override lazy val secure: Boolean = secureFunc()

  override protected def withProperties(newProperties: TypedMap): RequestHeaderImpl = {
    new RequestHeaderImpl(
      id = id,
      tags = tags,
      uri = uri,
      path = path,
      method = method,
      version = version,
      queryString = queryString,
      headers = headers,
      remoteAddressFunc = () => remoteAddress,
      secureFunc = () => secure,
      clientCertificateChain = clientCertificateChain,
      properties = newProperties
    )
  }
}

/**
 * Mixin to help build a RequestHeader that has properties. This mixin
 * assumes the properties are stored in a TypedMap.
 *
 * @tparam Repr The type of object to return when a copy is made.
 */
private[play] trait WithPropertiesMap[+Repr <: RequestHeader] {
  self: RequestHeader =>

  /**
   * The TypeMap holding this RequestHeader's properties.
   */
  protected def properties: TypedMap

  /**
   * Create a new instance with a new set of properties.
   */
  protected def withProperties(newProperties: TypedMap): Repr

  // Mixin methods

  override def apply[A](key: TypedKey[A]): A =
    properties.apply(key)
  override def get[A](key: TypedKey[A]): Option[A] =
    properties.get(key)
  override def contains(key: TypedKey[_]): Boolean =
    properties.contains(key)
  override def updated[A](key: TypedKey[A], value: A): Repr =
    withProperties(properties.updated(key, value))
  override def +(entry: TypedEntry[_]): Repr =
    withProperties(properties.+(entry))
  override def +(entry: TypedEntry[_], entries: TypedEntry[_]*): Repr =
    withProperties(properties.+(entry, entries: _*))
}