package play.api.mvc {

  import play.api._
  import play.api.http.{ MediaType, MediaRange, HeaderNames }
  import play.api.i18n.Lang
  import play.api.libs.iteratee._
  import play.api.libs.Crypto

  import scala.annotation._
  import scala.util.control.NonFatal
  import scala.util.Try
  import java.net.{ URLDecoder, URLEncoder }

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
     * If the `X-Forwarded-For` header is present, then this method will return the value in that header
     * if either the local address is 127.0.0.1, or if `trustxforwarded` is configured to be true in the
     * application configuration file.
     */
    def remoteAddress: String

    // -- Computed

    /**
     * Helper method to access a queryString parameter.
     */
    def getQueryString(key: String): Option[String] = queryString.get(key).flatMap(_.headOption)

    /**
     * The HTTP host (domain, optionally port)
     */
    lazy val host: String = headers.get(HeaderNames.HOST).getOrElse("")

    /**
     * The HTTP domain
     */
    lazy val domain: String = host.split(':').head

    /**
     * The Request Langs extracted from the Accept-Language header and sorted by preference (preferred first).
     */
    lazy val acceptLanguages: Seq[play.api.i18n.Lang] = {
      val langs = acceptHeader(HeaderNames.ACCEPT_LANGUAGE).map(item => (item._1, Lang.get(item._2)))
      langs.sortWith((a, b) => a._1 > b._1).map(_._2).flatten
    }

    /**
     * @return The media types list of the request’s Accept header, not sorted in any particular order.
     */
    @deprecated("Use acceptedTypes instead", "2.1")
    lazy val accept: Seq[String] = {
      for {
        acceptHeader <- headers.get(HeaderNames.ACCEPT).toSeq
        value <- acceptHeader.split(',')
        contentType <- value.split(';').headOption
      } yield contentType
    }

    /**
     * @return The media types list of the request’s Accept header, sorted by preference (preferred first).
     */
    lazy val acceptedTypes: Seq[play.api.http.MediaRange] = {
      headers.get(HeaderNames.ACCEPT).toSeq.flatMap(MediaRange.parse.apply)
    }

    /**
     * @return The items of an Accept* header, with their q-value.
     */
    private def acceptHeader(headerName: String): Seq[(Double, String)] = {
      for {
        header <- headers.get(headerName).toSeq
        value0 <- header.split(',')
        value = value0.trim
      } yield {
        RequestHeader.qPattern.findFirstMatchIn(value) match {
          case Some(m) => (m.group(1).toDouble, m.before.toString)
          case None => (1.0, value) // “The default value is q=1.”
        }
      }
    }

    /**
     * Check if this request accepts a given media type.
     * @return true if `mimeType` matches the Accept header, otherwise false
     */
    def accepts(mimeType: String): Boolean = {
      acceptedTypes.isEmpty || acceptedTypes.find(_.accepts(mimeType)).isDefined
    }

    /**
     * The HTTP cookies.
     */
    lazy val cookies: Cookies = Cookies(headers.get(play.api.http.HeaderNames.COOKIE))

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
      remoteAddress: String = this.remoteAddress): RequestHeader = {
      val (_id, _tags, _uri, _path, _method, _version, _queryString, _headers, _remoteAddress) = (id, tags, uri, path, method, version, queryString, headers, remoteAddress)
      new RequestHeader {
        val id = _id
        val tags = _tags
        val uri = _uri
        val path = _path
        val method = _method
        val version = _version
        val queryString = _queryString
        val headers = _headers
        val remoteAddress = _remoteAddress
      }
    }

    override def toString = {
      method + " " + uri
    }

  }

  object RequestHeader {
    // “The first "q" parameter (if any) separates the media-range parameter(s) from the accept-params.”
    val qPattern = ";\\s*q=([0-9.]+)".r
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
      def id = self.id
      def tags = self.tags
      def uri = self.uri
      def path = self.path
      def method = self.method
      def version = self.version
      def queryString = self.queryString
      def headers = self.headers
      def remoteAddress = self.remoteAddress
      lazy val body = f(self.body)
    }

  }

  object Request {

    def apply[A](rh: RequestHeader, a: A) = new Request[A] {
      def id = rh.id
      def tags = rh.tags
      def uri = rh.uri
      def path = rh.path
      def method = rh.method
      def version = rh.version
      def queryString = rh.queryString
      def headers = rh.headers
      lazy val remoteAddress = rh.remoteAddress
      def username = None
      val body = a
    }
  }

  /**
   * Wrap an existing request. Useful to extend a request.
   */
  class WrappedRequest[A](request: Request[A]) extends Request[A] {
    def id = request.id
    def tags = request.tags
    def body = request.body
    def headers = request.headers
    def queryString = request.queryString
    def path = request.path
    def uri = request.uri
    def method = request.method
    def version = request.version
    def remoteAddress = request.remoteAddress
  }

  /**
   * Defines a `Call`, which describes an HTTP request and can be used to create links or fill redirect data.
   *
   * These values are usually generated by the reverse router.
   *
   * @param method the request HTTP method
   * @param url the request URL
   */
  case class Call(method: String, url: String) extends play.mvc.Call {

    /**
     * Transform this call to an absolute URL.
     */
    def absoluteURL(secure: Boolean = false)(implicit request: RequestHeader) = {
      "http" + (if (secure) "s" else "") + "://" + request.host + this.url
    }

    /**
     * Transform this call to an WebSocket URL.
     */
    def webSocketURL(secure: Boolean = false)(implicit request: RequestHeader) = {
      "ws" + (if (secure) "s" else "") + "://" + request.host + this.url
    }

    override def toString = url

  }

  /**
   * The HTTP headers set.
   */
  trait Headers {

    /**
     * Optionally returns the first header value associated with a key.
     */
    def get(key: String): Option[String] = getAll(key).headOption

    /**
     * Retrieves the first header value which is associated with the given key.
     */
    def apply(key: String): String = get(key).getOrElse(scala.sys.error("Header doesn't exist"))

    /**
     * Retrieve all header values associated with the given key.
     */
    def getAll(key: String): Seq[String] = {
      data.find({ case (k, v) => k.equalsIgnoreCase(key) }).map(_._2).getOrElse(Nil)
    }

    /**
     * Retrieve all header keys
     */
    def keys: Set[String] = {
      Set.empty ++ data.map(_._1)
    }

    /**
     * Transform the Headers to a Map
     */
    lazy val toMap: Map[String, Seq[String]] = {
      import collection.immutable.TreeMap
      import play.core.utils.CaseInsensitiveOrdered
      TreeMap(data: _*)(CaseInsensitiveOrdered)
    }

    /**
     * The internal data structure here is a sequence of header to sequence of value pairs. Multiple
     * headers with the same name are not expected in the sequence. Instead the same header with multiple values
     * in the order that they appear in the http header is expected.
     */
    protected val data: Seq[(String, Seq[String])]

    /**
     * Transform the Headers to a Map by ignoring multiple values.
     */
    def toSimpleMap: Map[String, String] = {
      val simpleSeq = data.map({ case (k, v) => (k, v.headOption.getOrElse("")) })
      Map.empty ++ simpleSeq
    }

    override def toString = data.toString

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
     * Encodes the data as a `String`.
     */
    def encode(data: Map[String, String]): String = {
      val encoded = data.map {
        case (k, v) => URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
      }.mkString("&")
      if (isSigned)
        Crypto.sign(encoded) + "-" + encoded
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
          if (safeEquals(splitted(0), Crypto.sign(message)))
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
    def decodeFromCookie(cookie: Option[Cookie]): T = {
      cookie.filter(_.name == COOKIE_NAME).map(c => deserialize(decode(c.value))).getOrElse(emptyCookie)
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
    val COOKIE_NAME = Play.maybeApplication.flatMap(_.configuration.getString("session.cookieName")).getOrElse("PLAY_SESSION")
    val emptyCookie = new Session
    override val isSigned = true
    override def secure = Play.maybeApplication.flatMap(_.configuration.getBoolean("session.secure")).getOrElse(false)
    override val maxAge = Play.maybeApplication.flatMap(_.configuration.getInt("session.maxAge"))
    override val httpOnly = Play.maybeApplication.flatMap(_.configuration.getBoolean("session.httpOnly")).getOrElse(true)
    override def path = Play.maybeApplication.flatMap(_.configuration.getString("application.context")).getOrElse("/")
    override def domain = Play.maybeApplication.flatMap(_.configuration.getString("session.domain"))

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

    val COOKIE_NAME = Play.maybeApplication.flatMap(_.configuration.getString("flash.cookieName")).getOrElse("PLAY_FLASH")
    override def path = Play.maybeApplication.flatMap(_.configuration.getString("application.context")).getOrElse("/")

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

    import scala.collection.JavaConverters._

    // We use netty here but just as an API to handle cookies encoding
    import org.jboss.netty.handler.codec.http.{ CookieEncoder, CookieDecoder, DefaultCookie }

    /**
     * Extract cookies from the Set-Cookie header.
     */
    def apply(header: Option[String]) = new Cookies {

      lazy val cookies: Map[String, Cookie] = header.map(Cookies.decode(_)).getOrElse(Seq.empty).groupBy(_.name).mapValues(_.head)

      def get(name: String) = cookies.get(name)
      override def toString = cookies.toString

      def foreach[U](f: (Cookie) => U) {
        cookies.values.foreach(f)
      }
    }

    /**
     * Encodes cookies as a proper HTTP header.
     *
     * @param cookies the Cookies to encode
     * @return a valid Set-Cookie header value
     */
    def encode(cookies: Seq[Cookie]): String = {
      val encoder = new CookieEncoder(true)
      val newCookies = cookies.map { c =>
        encoder.addCookie {
          val nc = new DefaultCookie(c.name, c.value)
          nc.setMaxAge(c.maxAge.getOrElse(Integer.MIN_VALUE))
          nc.setPath(c.path)
          c.domain.map(nc.setDomain(_))
          nc.setSecure(c.secure)
          nc.setHttpOnly(c.httpOnly)
          nc
        }
        encoder.encode()
      }
      newCookies.mkString("; ")
    }

    /**
     * Decodes a Set-Cookie header value as a proper cookie set.
     *
     * @param cookieHeader the Set-Cookie header value
     * @return decoded cookies
     */

    private lazy val decoder = new CookieDecoder()
    def decode(cookieHeader: String): Seq[Cookie] = {
      Try {
        decoder.decode(cookieHeader).asScala.map { c =>
          Cookie(c.getName, c.getValue, if (c.getMaxAge == Integer.MIN_VALUE) None else Some(c.getMaxAge), Option(c.getPath).getOrElse("/"), Option(c.getDomain), c.isSecure, c.isHttpOnly)
        }.toSeq
      }.getOrElse {
        Play.logger.debug(s"Couldn't decode the Cookie header containing: $cookieHeader")
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
    def merge(cookieHeader: String, cookies: Seq[Cookie]): String = {
      encode(cookies ++ decode(cookieHeader))
    }

  }

}
