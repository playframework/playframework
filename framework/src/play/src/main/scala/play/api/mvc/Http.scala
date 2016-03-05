/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc {

  import java.security.cert.X509Certificate
  import java.util.Locale
import java.util.Locale.LanguageRange

import play.api._
  import play.api.http._
  import play.api.i18n.Lang
  import play.api.libs.crypto.CookieSigner
  import play.core.utils.CaseInsensitiveOrdered

  import scala.annotation._
  import scala.collection.immutable.{ TreeMap, TreeSet }
  import scala.util.control.NonFatal
  import scala.util.Try
  import java.net.{ URI, URLDecoder, URLEncoder }

  private[mvc] object GlobalStateHttpConfiguration {
    def httpConfiguration: HttpConfiguration = HttpConfiguration.current
  }

  import GlobalStateHttpConfiguration._

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

    // -- Computed

    /**
     * Helper method to access a queryString parameter.
     */
    def getQueryString(key: String): Option[String] = queryString.get(key).flatMap(_.headOption)

    /**
     * The HTTP host (domain, optionally port)
     */
    lazy val host: String = {
      val u = new URI(uri)
      (u.getHost, u.getPort) match {
        case (h, p) if h != null && p > 0 => s"$h:$p"
        case (h, _) if h != null => h
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
      val (_id, _tags, _uri, _path, _method, _version, _queryString, _headers, _remoteAddress, _secure, _clientCertificateChain) = (id, tags, uri, path, method, version, queryString, headers, () => remoteAddress, () => secure, clientCertificateChain)
      new RequestHeader {
        override val id = _id
        override val tags = _tags
        override val uri = _uri
        override val path = _path
        override val method = _method
        override val version = _version
        override val queryString = _queryString
        override val headers = _headers
        override lazy val remoteAddress = _remoteAddress()
        override lazy val secure = _secure()
        override val clientCertificateChain = _clientCertificateChain
      }
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

  private[play] class RequestHeaderImpl(
      override val id: Long,
      override val tags: Map[String, String],
      override val uri: String,
      override val path: String,
      override val method: String,
      override val version: String,
      override val queryString: Map[String, Seq[String]],
      override val headers: Headers,
      override val remoteAddress: String,
      override val secure: Boolean,
      override val clientCertificateChain: Option[Seq[X509Certificate]]) extends RequestHeader {
  }

  /**
   * The complete HTTP request.
   *
   * @tparam A the body content type.
   */
  @implicitNotFound("Cannot find any HTTP Request here")
  trait Request[+A] extends RequestHeader {
    self =>

    /**
     * The body content.
     */
    def body: A

    /**
     * Transform the request body.
     */
    def map[B](f: A => B): Request[B] = new Request[B] {
      override def id = self.id
      override def tags = self.tags
      override def uri = self.uri
      override def path = self.path
      override def method = self.method
      override def version = self.version
      override def queryString = self.queryString
      override def headers = self.headers
      override def remoteAddress = self.remoteAddress
      override def secure = self.secure
      override def clientCertificateChain = self.clientCertificateChain

      override lazy val body = f(self.body)
    }

  }

  /** Used by Java wrapper */
  private[play] class RequestImpl[A](
      override val body: A,
      override val id: Long,
      override val tags: Map[String, String],
      override val uri: String,
      override val path: String,
      override val method: String,
      override val version: String,
      override val queryString: Map[String, Seq[String]],
      override val headers: Headers,
      override val remoteAddress: String,
      override val secure: Boolean,
      override val clientCertificateChain: Option[Seq[X509Certificate]]) extends Request[A] {
  }

  object Request {

    def apply[A](rh: RequestHeader, a: A) = new Request[A] {
      override def id = rh.id
      override def tags = rh.tags
      override def uri = rh.uri
      override def path = rh.path
      override def method = rh.method
      override def version = rh.version
      override def queryString = rh.queryString
      override def headers = rh.headers
      override lazy val remoteAddress = rh.remoteAddress
      override lazy val secure = rh.secure
      override val clientCertificateChain = rh.clientCertificateChain
      override val body = a
    }
  }

  /**
   * Wrap an existing request. Useful to extend a request.
   */
  class WrappedRequest[+A](request: Request[A]) extends Request[A] {
    override def id = request.id
    override def tags = request.tags
    override def body = request.body
    override def headers = request.headers
    override def queryString = request.queryString
    override def path = request.path
    override def uri = request.uri
    override def method = request.method
    override def version = request.version
    override def remoteAddress = request.remoteAddress
    override def secure = request.secure
    override def clientCertificateChain = request.clientCertificateChain
  }

  /**
   * Defines a `Call`, which describes an HTTP request and can be used to create links or fill redirect data.
   *
   * These values are usually generated by the reverse router.
   *
   * @param method the request HTTP method
   * @param url the request URL
   */
  case class Call(method: String, url: String, fragment: String = null) extends play.mvc.Call {

    override def unique(): Call = copy(url = uniquify(url))

    override def withFragment(fragment: String): Call = copy(fragment = fragment)

    /**
     * Transform this call to an absolute URL.
     *
     * {{{
     * import play.api.mvc.{ Call, RequestHeader }
     *
     * implicit val req: RequestHeader = myRequest
     * val url: String = Call("GET", "/url").absoluteURL()
     * // == "http://\$host/url", or "https://\$host/url" if secure
     * }}}
     */
    def absoluteURL()(implicit request: RequestHeader): String =
      absoluteURL(request.secure)

    /**
     * Transform this call to an absolute URL.
     */
    def absoluteURL(secure: Boolean)(implicit request: RequestHeader): String =
      "http" + (if (secure) "s" else "") + "://" + request.host + this.url + this.appendFragment

    /**
     * Transform this call to an WebSocket URL.
     *
     * {{{
     * import play.api.mvc.{ Call, RequestHeader }
     *
     * implicit val req: RequestHeader = myRequest
     * val url: String = Call("GET", "/url").webSocketURL()
     * // == "ws://\$host/url", or "wss://\$host/url" if secure
     * }}}
     */
    def webSocketURL()(implicit request: RequestHeader): String =
      webSocketURL(request.secure)

    /**
     * Transform this call to an WebSocket URL.
     */
    def webSocketURL(secure: Boolean)(implicit request: RequestHeader): String = "ws" + (if (secure) "s" else "") + "://" + request.host + this.url

  }

  /**
   * The HTTP headers set.
   *
   * @param _headers The sequence of values. This value is protected and mutable
   * since subclasses might initially set it to a `null` value and then initialize
   * it lazily.
   */
  class Headers(protected var _headers: Seq[(String, String)]) {

    /**
     * The headers as a sequence of name-value pairs.
     */
    def headers: Seq[(String, String)] = _headers

    /**
     * Append the given headers
     */
    def add(headers: (String, String)*) = new Headers(this.headers ++ headers)

    /**
     * Retrieves the first header value which is associated with the given key.
     */
    def apply(key: String): String = get(key).getOrElse(scala.sys.error("Header doesn't exist"))

    override def equals(other: Any) = {
      other.isInstanceOf[Headers] &&
        toMap == other.asInstanceOf[Headers].toMap
    }

    /**
     * Optionally returns the first header value associated with a key.
     */
    def get(key: String): Option[String] = getAll(key).headOption

    /**
     * Retrieve all header values associated with the given key.
     */
    def getAll(key: String): Seq[String] = toMap.getOrElse(key, Nil)

    override def hashCode = {
      toMap.map {
        case (name, value) =>
          name.toLowerCase(Locale.ENGLISH) -> value
      }.hashCode()
    }

    /**
     * Retrieve all header keys
     */
    def keys: Set[String] = toMap.keySet

    /**
     * Remove any headers with the given keys
     */
    def remove(keys: String*) = {
      val keySet = TreeSet(keys: _*)(CaseInsensitiveOrdered)
      new Headers(headers.filterNot { case (name, _) => keySet(name) })
    }

    /**
     * Append the given headers, replacing any existing headers having the same keys
     */
    def replace(headers: (String, String)*) = remove(headers.map(_._1): _*).add(headers: _*)

    /**
     * Transform the Headers to a Map
     */
    lazy val toMap: Map[String, Seq[String]] = {
      val map = headers.groupBy(_._1.toLowerCase(Locale.ENGLISH)).map {
        case (_, headers) =>
          // choose the case of first header as canonical
          headers.head._1 -> headers.map(_._2)
      }
      TreeMap(map.toSeq: _*)(CaseInsensitiveOrdered)
    }

    /**
     * Transform the Headers to a Map by ignoring multiple values.
     */
    lazy val toSimpleMap: Map[String, String] = toMap.mapValues(_.headOption.getOrElse(""))

    override def toString = headers.toString()

  }

  object Headers {

    def apply(headers: (String, String)*) = new Headers(headers)

  }

  /**
   * Trait that should be extended by the Cookie helpers.
   */
  trait CookieBaker[T <: AnyRef] {

    /**
     * The cookie name.
     */
    def COOKIE_NAME: String

    /**
     * Default cookie, returned in case of error or if missing in the HTTP headers.
     */
    def emptyCookie: T

    /**
     * `true` if the Cookie is signed. Defaults to false.
     */
    def isSigned: Boolean = false

    /**
     * `true` if the Cookie should have the httpOnly flag, disabling access from Javascript. Defaults to true.
     */
    def httpOnly = true

    /**
     * The cookie expiration date in seconds, `None` for a transient cookie
     */
    def maxAge: Option[Int] = None

    /**
     * The cookie domain. Defaults to None.
     */
    def domain: Option[String] = None

    /**
     * `true` if the Cookie should have the secure flag, restricting usage to https. Defaults to false.
     */
    def secure = false

    /**
     *  The cookie path.
     */
    def path = "/"

    /**
     * The cookie signer.
     */
    def cookieSigner: CookieSigner

    /**
     * Encodes the data as a `String`.
     */
    def encode(data: Map[String, String]): String = {
      val encoded = data.map {
        case (k, v) => URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
      }.mkString("&")
      if (isSigned)
        cookieSigner.sign(encoded) + "-" + encoded
      else
        encoded
    }

    /**
     * Decodes from an encoded `String`.
     */
    def decode(data: String): Map[String, String] = {

      def urldecode(data: String) = {
        data
          .split("&")
          .map(_.split("=", 2))
          .map(p => URLDecoder.decode(p(0), "UTF-8") -> URLDecoder.decode(p(1), "UTF-8"))
          .toMap
      }

      // Do not change this unless you understand the security issues behind timing attacks.
      // This method intentionally runs in constant time if the two strings have the same length.
      // If it didn't, it would be vulnerable to a timing attack.
      def safeEquals(a: String, b: String) = {
        if (a.length != b.length) {
          false
        } else {
          var equal = 0
          for (i <- Array.range(0, a.length)) {
            equal |= a(i) ^ b(i)
          }
          equal == 0
        }
      }

      try {
        if (isSigned) {
          val splitted = data.split("-", 2)
          val message = splitted.tail.mkString("-")
          if (safeEquals(splitted(0), cookieSigner.sign(message)))
            urldecode(message)
          else
            Map.empty[String, String]
        } else urldecode(data)
      } catch {
        // fail gracefully is the session cookie is corrupted
        case NonFatal(_) => Map.empty[String, String]
      }
    }

    /**
     * Encodes the data as a `Cookie`.
     */
    def encodeAsCookie(data: T): Cookie = {
      val cookie = encode(serialize(data))
      Cookie(COOKIE_NAME, cookie, maxAge, path, domain, secure, httpOnly)
    }

    /**
     * Decodes the data from a `Cookie`.
     */
    def decodeCookieToMap(cookie: Option[Cookie]): Map[String, String] = {
      serialize(decodeFromCookie(cookie))
    }

    /**
     * Decodes the data from a `Cookie`.
     */
    def decodeFromCookie(cookie: Option[Cookie]): T = if (cookie.isEmpty) emptyCookie else {
      val extractedCookie: Cookie = cookie.get
      if (extractedCookie.name != COOKIE_NAME) emptyCookie /* can this happen? */ else {
        deserialize(decode(extractedCookie.value))
      }
    }

    def discard = DiscardingCookie(COOKIE_NAME, path, domain, secure)

    /**
     * Builds the cookie object from the given data map.
     *
     * @param data the data map to build the cookie object
     * @return a new cookie object
     */
    protected def deserialize(data: Map[String, String]): T

    /**
     * Converts the given cookie object into a data map.
     *
     * @param cookie the cookie object to serialize into a map
     * @return a new `Map` storing the key-value pairs for the given cookie
     */
    protected def serialize(cookie: T): Map[String, String]

  }

  /**
   * HTTP Session.
   *
   * Session data are encoded into an HTTP cookie, and can only contain simple `String` values.
   */
  case class Session(data: Map[String, String] = Map.empty[String, String]) {

    /**
     * Optionally returns the session value associated with a key.
     */
    def get(key: String) = data.get(key)

    /**
     * Returns `true` if this session is empty.
     */
    def isEmpty: Boolean = data.isEmpty

    /**
     * Adds a value to the session, and returns a new session.
     *
     * For example:
     * {{{
     * session + ("username" -> "bob")
     * }}}
     *
     * @param kv the key-value pair to add
     * @return the modified session
     */
    def +(kv: (String, String)) = {
      require(kv._2 != null, "Cookie values cannot be null")
      copy(data + kv)
    }

    /**
     * Removes any value from the session.
     *
     * For example:
     * {{{
     * session - "username"
     * }}}
     *
     * @param key the key to remove
     * @return the modified session
     */
    def -(key: String) = copy(data - key)

    /**
     * Retrieves the session value which is associated with the given key.
     */
    def apply(key: String) = data(key)

  }

  /**
   * Helper utilities to manage the Session cookie.
   */
  object Session extends CookieBaker[Session] {
    private def config: SessionConfiguration = httpConfiguration.session

    def COOKIE_NAME = config.cookieName

    val emptyCookie = new Session
    override val isSigned = true
    override def secure = config.secure
    override def maxAge = config.maxAge.map(_.toSeconds.toInt)
    override def httpOnly = config.httpOnly
    override def path = httpConfiguration.context
    override def domain = config.domain
    override def cookieSigner = play.api.libs.Crypto.crypto

    def deserialize(data: Map[String, String]) = new Session(data)

    def serialize(session: Session) = session.data
  }

  /**
   * HTTP Flash scope.
   *
   * Flash data are encoded into an HTTP cookie, and can only contain simple `String` values.
   */
  case class Flash(data: Map[String, String] = Map.empty[String, String]) {

    /**
     * Optionally returns the flash value associated with a key.
     */
    def get(key: String) = data.get(key)

    /**
     * Returns `true` if this flash scope is empty.
     */
    def isEmpty: Boolean = data.isEmpty

    /**
     * Adds a value to the flash scope, and returns a new flash scope.
     *
     * For example:
     * {{{
     * flash + ("success" -> "Done!")
     * }}}
     *
     * @param kv the key-value pair to add
     * @return the modified flash scope
     */
    def +(kv: (String, String)) = {
      require(kv._2 != null, "Cookie values cannot be null")
      copy(data + kv)
    }

    /**
     * Removes a value from the flash scope.
     *
     * For example:
     * {{{
     * flash - "success"
     * }}}
     *
     * @param key the key to remove
     * @return the modified flash scope
     */
    def -(key: String) = copy(data - key)

    /**
     * Retrieves the flash value that is associated with the given key.
     */
    def apply(key: String) = data(key)

  }

  /**
   * Helper utilities to manage the Flash cookie.
   */
  object Flash extends CookieBaker[Flash] {
    private def config: FlashConfiguration = httpConfiguration.flash
    private def sessionConfig: SessionConfiguration = httpConfiguration.session

    def COOKIE_NAME = config.cookieName

    override def path = httpConfiguration.context
    override def secure = config.secure
    override def httpOnly = config.httpOnly
    override def domain = sessionConfig.domain
    override def cookieSigner = play.api.libs.Crypto.crypto

    val emptyCookie = new Flash

    def deserialize(data: Map[String, String]) = new Flash(data)

    def serialize(flash: Flash) = flash.data

  }

  /**
   * An HTTP cookie.
   *
   * @param name the cookie name
   * @param value the cookie value
   * @param maxAge the cookie expiration date in seconds, `None` for a transient cookie, or a value less than 0 to expire a cookie now
   * @param path the cookie path, defaulting to the root path `/`
   * @param domain the cookie domain
   * @param secure whether this cookie is secured, sent only for HTTPS requests
   * @param httpOnly whether this cookie is HTTP only, i.e. not accessible from client-side JavaScipt code
   */
  case class Cookie(name: String, value: String, maxAge: Option[Int] = None, path: String = "/", domain: Option[String] = None, secure: Boolean = false, httpOnly: Boolean = true)

  /**
   * A cookie to be discarded.  This contains only the data necessary for discarding a cookie.
   *
   * @param name the name of the cookie to discard
   * @param path the path of the cookie, defaults to the root path
   * @param domain the cookie domain
   * @param secure whether this cookie is secured
   */
  case class DiscardingCookie(name: String, path: String = "/", domain: Option[String] = None, secure: Boolean = false) {
    def toCookie = Cookie(name, "", Some(-86400), path, domain, secure)
  }

  /**
   * The HTTP cookies set.
   */
  trait Cookies extends Traversable[Cookie] {

    /**
     * Optionally returns the cookie associated with a key.
     */
    def get(name: String): Option[Cookie]

    /**
     * Retrieves the cookie that is associated with the given key.
     */
    def apply(name: String): Cookie = get(name).getOrElse(scala.sys.error("Cookie doesn't exist"))
  }

  /**
   * Helper utilities to encode Cookies.
   */
  object Cookies {
    private def config: CookiesConfiguration = httpConfiguration.cookies

    /**
     * Play doesn't support multiple values per header, so has to compress cookies into one header. The problem is,
     * Set-Cookie doesn't support being compressed into one header, the reason being that the separator character for
     * header values, comma, is used in the dates in the Expires attribute of a cookie value. So we synthesise our own
     * separator, that we use here, and before we send the cookie back to the client.
     */
    val SetCookieHeaderSeparator = ";;"
    val SetCookieHeaderSeparatorRegex = SetCookieHeaderSeparator.r

    import scala.collection.JavaConverters._

    // We use netty here but just as an API to handle cookies encoding
    import play.core.netty.utils.DefaultCookie

    private val logger = Logger(this.getClass)

    def fromSetCookieHeader(header: Option[String]): Cookies = header match {
      case Some(headerValue) => fromMap(
        decodeSetCookieHeader(headerValue)
          .groupBy(_.name)
          .mapValues(_.head)
      )
      case None => fromMap(Map.empty)
    }

    def fromCookieHeader(header: Option[String]): Cookies = header match {
      case Some(headerValue) => fromMap(
        decodeCookieHeader(headerValue)
          .groupBy(_.name)
          .mapValues(_.head)
      )
      case None => fromMap(Map.empty)
    }

    private def fromMap(cookies: Map[String, Cookie]): Cookies = new Cookies {
      def get(name: String) = cookies.get(name)
      override def toString = cookies.toString

      def foreach[U](f: (Cookie) => U) {
        cookies.values.foreach(f)
      }
    }

    /**
     * Encodes cookies as a Set-Cookie HTTP header.
     *
     * @param cookies the Cookies to encode
     * @return a valid Set-Cookie header value
     */
    def encodeSetCookieHeader(cookies: Seq[Cookie]): String = {
      val encoder = config.serverEncoder
      val newCookies = cookies.map { c =>
        val nc = new DefaultCookie(c.name, c.value)
        nc.setMaxAge(c.maxAge.getOrElse(Integer.MIN_VALUE))
        nc.setPath(c.path)
        c.domain.foreach(nc.setDomain)
        nc.setSecure(c.secure)
        nc.setHttpOnly(c.httpOnly)
        encoder.encode(nc)
      }
      newCookies.mkString(SetCookieHeaderSeparator)
    }

    /**
     * Encodes cookies as a Set-Cookie HTTP header.
     *
     * @param cookies the Cookies to encode
     * @return a valid Set-Cookie header value
     */
    def encodeCookieHeader(cookies: Seq[Cookie]): String = {
      val encoder = config.clientEncoder
      encoder.encode(cookies.map { cookie =>
        new DefaultCookie(cookie.name, cookie.value)
      }.asJava)
    }

    /**
     * Decodes a Set-Cookie header value as a proper cookie set.
     *
     * @param cookieHeader the Set-Cookie header value
     * @return decoded cookies
     */
    def decodeSetCookieHeader(cookieHeader: String): Seq[Cookie] = {
      Try {
        val decoder = config.clientDecoder
        SetCookieHeaderSeparatorRegex.split(cookieHeader).toSeq.flatMap { cookieString =>
          Option(decoder.decode(cookieString.trim)).map(cookie =>
            Cookie(
              cookie.name,
              cookie.value,
              if (cookie.maxAge == Integer.MIN_VALUE) None else Some(cookie.maxAge),
              Option(cookie.path).getOrElse("/"),
              Option(cookie.domain),
              cookie.isSecure,
              cookie.isHttpOnly
            ))
        }
      }.getOrElse {
        logger.debug(s"Couldn't decode the Cookie header containing: $cookieHeader")
        Seq.empty
      }
    }

    /**
     * Decodes a Cookie header value as a proper cookie set.
     *
     * @param cookieHeader the Cookie header value
     * @return decoded cookies
     */
    def decodeCookieHeader(cookieHeader: String): Seq[Cookie] = {
      Try {
        config.serverDecoder.decode(cookieHeader).asScala.map { cookie =>
          Cookie(
            cookie.name,
            cookie.value
          )
        }.toSeq
      }.getOrElse {
        logger.debug(s"Couldn't decode the Cookie header containing: $cookieHeader")
        Nil
      }
    }

    /**
     * Merges an existing Set-Cookie header with new cookie values
     *
     * @param cookieHeader the existing Set-Cookie header value
     * @param cookies the new cookies to encode
     * @return a valid Set-Cookie header value
     */
    def mergeSetCookieHeader(cookieHeader: String, cookies: Seq[Cookie]): String = {
      val tupledCookies = (decodeSetCookieHeader(cookieHeader) ++ cookies).map { c =>
        // See rfc6265#section-4.1.2
        // Secure and http-only attributes are not considered when testing if
        // two cookies are overlapping.
        (c.name, c.path, c.domain.map(_.toLowerCase(Locale.ENGLISH))) -> c
      }
      // Put cookies in a map
      // Note: Seq.toMap do not preserve order
      val uniqCookies = scala.collection.immutable.ListMap(tupledCookies: _*)
      encodeSetCookieHeader(uniqCookies.values.toSeq)
    }

    /**
     * Merges an existing Cookie header with new cookie values
     *
     * @param cookieHeader the existing Cookie header value
     * @param cookies the new cookies to encode
     * @return a valid Cookie header value
     */
    def mergeCookieHeader(cookieHeader: String, cookies: Seq[Cookie]): String = {
      val tupledCookies = (decodeCookieHeader(cookieHeader) ++ cookies).map(cookie => cookie.name -> cookie)
      // Put cookies in a map
      // Note: Seq.toMap do not preserve order
      val uniqCookies = scala.collection.immutable.ListMap(tupledCookies: _*)
      encodeCookieHeader(uniqCookies.values.toSeq)
    }
  }
}
