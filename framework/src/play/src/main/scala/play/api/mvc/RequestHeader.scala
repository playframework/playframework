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
  @deprecated("Use attributes instead (e.g. attr(), getAttr(), withAttr(), etc)", "2.6.0")
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

  /**
   * Get an attribute by its key or throw an exception if it's not
   * present.
   *
   * Use this method if the attribute is expected to be present. If the
   * attribute is optional then use [[getAttr()]] instead.
   *
   * @param key The attribute key.
   * @tparam A The type of attribute.
   * @return The attribute value, if it exists.
   * @throws NoSuchElementException If the attribute doesn't exist.
   */
  def attr[A](key: TypedKey[A]): A

  /**
   * Get an optional attribute by its key.
   *
   * Use this method if the attribute is optional. If the attribute is
   * always present then use [[attr()]] instead.
   *
   * @param key The attribute key.
   * @tparam A The type of attribute.
   * @return `Some` attribute value, if it exists, or `None` otherwise.
   */
  def getAttr[A](key: TypedKey[A]): Option[A]

  /**
   * Check if this object contains an attribute.
   *
   * @param key The attribute key to check for.
   * @return True if the object contains the attribute, false otherwise.
   */
  def containsAttr(key: TypedKey[_]): Boolean

  /**
   * Create a new version of this object with an attribute added to it.
   *
   * @param key The attribute key.
   * @param value The new attribute value.
   * @tparam A The type of attribute.
   * @return The new version of this object with the attribute added.
   */
  def withAttr[A](key: TypedKey[A], value: A): RequestHeader

  /**
   * Create a new version of this object with several attributes added to it.
   *
   * @param entries The new attributes to add.
   * @return The new version of this object with the attributes added.
   */
  def withAttrs(entries: TypedEntry[_]*): RequestHeader

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
   * Attach a body to this header.
   *
   * @param body The body to attach.
   * @tparam A The type of the body.
   * @return A new request with the body attached to the header.
   */
  def withBody[A](body: A): Request[A] = new RequestHeaderWithBody[A](this, body)

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
      attrMap = TypedMap.empty
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
 * @param attrMap A map of the RequestHeader's typed attributes.
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
    override protected val attrMap: TypedMap) extends RequestHeader with WithAttrMap[RequestHeader] {

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
    attrMap: TypedMap) = {
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
      attrMap = attrMap
    )
  }

  override lazy val remoteAddress: String = remoteAddressFunc()
  override lazy val secure: Boolean = secureFunc()

  override protected def withAttrMap(newAttrMap: TypedMap): RequestHeaderImpl = {
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
      attrMap = newAttrMap
    )
  }
}

/**
 * Mixin to help build a RequestHeader that has attributes. This mixin
 * assumes the attributes are stored in a TypedMap.
 *
 * @tparam Repr The type of object to return when a copy is made.
 */
private[play] trait WithAttrMap[+Repr <: RequestHeader] {
  self: RequestHeader =>

  /**
   * The TypeMap holding this RequestHeader's attributes.
   */
  protected def attrMap: TypedMap

  /**
   * Create a new instance with a new set of attributes.
   */
  protected def withAttrMap(newAttrMap: TypedMap): Repr

  // Mixin methods

  override def attr[A](key: TypedKey[A]): A =
    attrMap.apply(key)
  override def getAttr[A](key: TypedKey[A]): Option[A] =
    attrMap.get(key)
  override def withAttr[A](key: TypedKey[A], value: A): Repr =
    withAttrMap(attrMap.updated(key, value))
  override def containsAttr(key: TypedKey[_]): Boolean =
    attrMap.contains(key)
  override def withAttrs(entries: TypedEntry[_]*): Repr =
    withAttrMap(attrMap.+(entries: _*))
}

/**
 * A Request formed from a RequestHeader and a body value. Most methods are delegated
 * to the RequestHeader. This means that creating this object is very cheap because it
 * doesn't evaluate any of the RequestHeader methods when it's constructed.
 */
private[play] class RequestHeaderWithBody[+A](
    private[RequestHeaderWithBody] val rh: RequestHeader,
    override val body: A) extends Request[A] {
  override def id = rh.id
  override def tags = rh.tags
  override def headers = rh.headers
  override def queryString = rh.queryString
  override def path = rh.path
  override def uri = rh.uri
  override def method = rh.method
  override def version = rh.version
  override def remoteAddress = rh.remoteAddress
  override def secure = rh.secure
  override def clientCertificateChain = rh.clientCertificateChain

  override def withBody[B](newBody: B): Request[B] = this match {
    case wrapper: RequestHeaderWithBody[_] => new RequestHeaderWithBody(wrapper.rh, newBody)
    case _ => new RequestHeaderWithBody(rh, newBody)
  }
  override def withAttr[B](key: TypedKey[B], value: B): Request[A] = new RequestHeaderWithBody(rh.withAttr(key, value), body)
  override def withAttrs(entries: TypedEntry[_]*): Request[A] = new RequestHeaderWithBody(rh.withAttrs(entries: _*), body)
  override def getAttr[A](key: TypedKey[A]): Option[A] = rh.getAttr(key)
  override def attr[A](key: TypedKey[A]): A = rh.attr(key)
  override def containsAttr(key: TypedKey[_]): Boolean = rh.containsAttr(key)
}

/**
 * A Request formed from a RequestHeader and a new set of attributes. Most methods are delegated
 * to the RequestHeader. This means that creating this object is very cheap because it
 * doesn't evaluate any of the RequestHeader methods when it's constructed.
 */
private[play] class RequestHeaderWithAttributes(rh: RequestHeader, override val attrMap: TypedMap)
    extends RequestHeader with WithAttrMap[RequestHeader] {
  override def id = rh.id
  override def tags = rh.tags
  override def headers = rh.headers
  override def queryString = rh.queryString
  override def path = rh.path
  override def uri = rh.uri
  override def method = rh.method
  override def version = rh.version
  override def remoteAddress = rh.remoteAddress
  override def secure = rh.secure
  override def clientCertificateChain = rh.clientCertificateChain
  override protected def withAttrMap(newAttrMap: TypedMap): RequestHeader = new RequestHeaderWithAttributes(rh, newAttrMap)
}
