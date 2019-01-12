/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.net.{ URLDecoder, URLEncoder }
import java.nio.charset.StandardCharsets
import java.util.{ Base64, Date, Locale }
import javax.inject.Inject

import io.jsonwebtoken.Jwts
import play.api.MarkerContexts.SecurityMarkerContext
import play.api._
import play.api.http._
import play.api.inject.{ SimpleModule, bind }
import play.api.libs.crypto.{ CookieSigner, CookieSignerProvider }
import play.api.mvc.Cookie.SameSite
import play.libs.Scala
import play.mvc.Http.{ Cookie => JCookie }

import scala.collection.immutable.ListMap
import scala.util.Try
import scala.util.control.NonFatal

/**
 * An HTTP cookie.
 *
 * @param name the cookie name
 * @param value the cookie value
 * @param maxAge the cookie expiration date in seconds, `None` for a transient cookie, or a value 0 or less to expire a cookie now
 * @param path the cookie path, defaulting to the root path `/`
 * @param domain the cookie domain
 * @param secure whether this cookie is secured, sent only for HTTPS requests
 * @param httpOnly whether this cookie is HTTP only, i.e. not accessible from client-side JavaScript code
 */
case class Cookie(
    name: String,
    value: String,
    maxAge: Option[Int] = None,
    path: String = "/",
    domain: Option[String] = None,
    secure: Boolean = false,
    httpOnly: Boolean = true,
    sameSite: Option[Cookie.SameSite] = None
) {
  lazy val asJava = {
    new JCookie(name, value, maxAge.map(i => Integer.valueOf(i)).orNull, path, domain.orNull,
      secure, httpOnly, sameSite.map(_.asJava).orNull)
  }
}

object Cookie {

  private val logger = Logger(this.getClass)

  sealed abstract class SameSite(val value: String) {
    private def matches(v: String): Boolean = value equalsIgnoreCase v

    def asJava: play.mvc.Http.Cookie.SameSite = play.mvc.Http.Cookie.SameSite.parse(value).get
  }
  object SameSite {
    private[play] val values: Seq[SameSite] = Seq(Strict, Lax)
    def parse(value: String): Option[SameSite] = values.find(_ matches value)
    case object Strict extends SameSite("Strict")
    case object Lax extends SameSite("Lax")
  }

  /**
   * Check the prefix of this cookie and make sure it matches the rules.
   *
   * @return the original cookie if it is valid, else a new cookie that has the proper attributes set.
   */
  def validatePrefix(cookie: Cookie): Cookie = {
    val SecurePrefix = "__Secure-"
    val HostPrefix = "__Host-"
    @inline def warnIfNotSecure(prefix: String): Unit = {
      if (!cookie.secure) {
        logger.warn(s"$prefix prefix is used for cookie but Secure flag not set! Setting now. Cookie is: $cookie")(SecurityMarkerContext)
      }
    }

    if (cookie.name startsWith SecurePrefix) {
      warnIfNotSecure(SecurePrefix)
      cookie.copy(secure = true)
    } else if (cookie.name startsWith HostPrefix) {
      warnIfNotSecure(HostPrefix)
      if (cookie.path != "/") {
        logger.warn(s"""$HostPrefix is used on cookie but Path is not "/"! Setting now. Cookie is: $cookie""")(SecurityMarkerContext)
      }
      cookie.copy(secure = true, path = "/")
    } else {
      cookie
    }
  }

  import scala.concurrent.duration._

  /**
   * The cookie's Max-Age, in seconds, when we expire the cookie.
   *
   * When Max-Age = 0, Expires is set to 0 epoch time for compatibility with older browsers.
   */
  val DiscardedMaxAge: Int = 0
}

/**
 * A cookie to be discarded.  This contains only the data necessary for discarding a cookie.
 *
 * @param name the name of the cookie to discard
 * @param path the path of the cookie, defaults to the root path
 * @param domain the cookie domain
 * @param secure whether this cookie is secured
 */
case class DiscardingCookie(name: String, path: String = "/", domain: Option[String] = None, secure: Boolean = false) {
  def toCookie = Cookie(name, "", Some(Cookie.DiscardedMaxAge), path, domain, secure, false)
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
object Cookies extends CookieHeaderEncoding {

  // Use global state for cookie header configuration
  @deprecated("Inject play.api.mvc.CookieHeaderEncoding instead", "2.6.0")
  override protected def config: CookiesConfiguration = HttpConfiguration.current.cookies

  def apply(cookies: Seq[Cookie]): Cookies = new Cookies {
    lazy val cookiesByName = cookies.groupBy(_.name).mapValues(_.head)

    override def get(name: String) = cookiesByName.get(name)

    override def foreach[U](f: Cookie => U) = cookies.foreach(f)
  }

}

/**
 * Logic for encoding and decoding `Cookie` and `Set-Cookie` headers.
 */
trait CookieHeaderEncoding {

  import play.core.cookie.encoding.DefaultCookie

  private implicit val markerContext = SecurityMarkerContext

  protected def config: CookiesConfiguration

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

    def foreach[U](f: (Cookie) => U): Unit = {
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
    val newCookies = cookies.map { cookie =>
      val c = Cookie.validatePrefix(cookie)
      val nc = new DefaultCookie(c.name, c.value)
      nc.setMaxAge(c.maxAge.getOrElse(Integer.MIN_VALUE))
      nc.setPath(c.path)
      c.domain.foreach(nc.setDomain)
      nc.setSecure(c.secure)
      nc.setHttpOnly(c.httpOnly)
      nc.setSameSite(c.sameSite.map(_.value).orNull)
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
    if (cookieHeader.isEmpty) {
      // fail fast if there are no existing cookies
      Seq.empty
    } else {
      Try {
        val decoder = config.clientDecoder
        val newCookies = for {
          cookieString <- SetCookieHeaderSeparatorRegex.split(cookieHeader).toSeq
          cookie <- Option(decoder.decode(cookieString.trim))
        } yield Cookie(
          cookie.name,
          cookie.value,
          if (cookie.maxAge == Integer.MIN_VALUE) None else Some(cookie.maxAge),
          Option(cookie.path).getOrElse("/"),
          Option(cookie.domain),
          cookie.isSecure,
          cookie.isHttpOnly,
          Option(cookie.sameSite).flatMap(SameSite.parse)
        )
        newCookies.map(Cookie.validatePrefix)
      } getOrElse {
        logger.debug(s"Couldn't decode the Cookie header containing: $cookieHeader")
        Seq.empty
      }
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
    val rawCookies = decodeSetCookieHeader(cookieHeader) ++ cookies
    val mergedCookies: Seq[Cookie] = CookieHeaderMerging.mergeSetCookieHeaderCookies(rawCookies)
    encodeSetCookieHeader(mergedCookies)
  }

  /**
   * Merges an existing Cookie header with new cookie values
   *
   * @param cookieHeader the existing Cookie header value
   * @param cookies the new cookies to encode
   * @return a valid Cookie header value
   */
  def mergeCookieHeader(cookieHeader: String, cookies: Seq[Cookie]): String = {
    val rawCookies = decodeCookieHeader(cookieHeader) ++ cookies
    val mergedCookies: Seq[Cookie] = CookieHeaderMerging.mergeCookieHeaderCookies(rawCookies)
    encodeCookieHeader(mergedCookies)
  }
}

/**
 * The default implementation of `CookieHeaders`.
 */
class DefaultCookieHeaderEncoding @Inject() (
    override protected val config: CookiesConfiguration = CookiesConfiguration()) extends CookieHeaderEncoding

/**
 * Utilities for merging individual cookie values in HTTP cookie headers.
 */
object CookieHeaderMerging {

  /**
   * Merge the elements in a sequence so that there is only one occurrence of
   * elements when mapped by a discriminator function.
   */
  private def mergeOn[A, B](input: Traversable[A], f: A => B): Seq[A] = {
    val withMergeValue: Seq[(B, A)] = input.toSeq.map(el => (f(el), el))
    ListMap(withMergeValue: _*).values.toSeq
  }

  /**
   * Merges the cookies contained in a `Set-Cookie` header so that there's
   * only one cookie for each name/path/domain triple.
   */
  def mergeSetCookieHeaderCookies(unmerged: Traversable[Cookie]): Seq[Cookie] = {
    // See rfc6265#section-4.1.2
    // Secure and http-only attributes are not considered when testing if
    // two cookies are overlapping.
    mergeOn(unmerged, (c: Cookie) => (c.name, c.path, c.domain.map(_.toLowerCase(Locale.ENGLISH))))
  }

  /**
   * Merges the cookies contained in a `Cookie` header so that there's
   * only one cookie for each name.
   */
  def mergeCookieHeaderCookies(unmerged: Traversable[Cookie]): Seq[Cookie] = {
    mergeOn(unmerged, (c: Cookie) => c.name)
  }
}

/**
 * Trait that should be extended by the Cookie helpers.
 */
trait CookieBaker[T <: AnyRef] { self: CookieDataCodec =>

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
  def path: String = "/"

  /**
   * The value of the SameSite attribute of the cookie. Defaults to no SameSite.
   */
  def sameSite: Option[Cookie.SameSite] = None

  /**
   * Encodes the data as a `Cookie`.
   */
  def encodeAsCookie(data: T): Cookie = {
    val cookie = encode(serialize(data))
    Cookie(COOKIE_NAME, cookie, maxAge, path, domain, secure, httpOnly, sameSite)
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
 * This trait encodes and decodes data to a string used as cookie value.
 */
trait CookieDataCodec {

  /**
   * Encodes the data as a `String`.
   */
  def encode(data: Map[String, String]): String

  /**
   * Decodes from an encoded `String`.
   */
  def decode(data: String): Map[String, String]
}

/**
 * This trait writes out cookies as url encoded safe text format, optionally prefixed with a
 * signed code.
 */
trait UrlEncodedCookieDataCodec extends CookieDataCodec {

  private val logger = Logger(this.getClass)

  /**
   * The cookie signer.
   */
  def cookieSigner: CookieSigner

  def isSigned: Boolean

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

    def urldecode(data: String): Map[String, String] = {
      // In some cases we've seen clients ignore the Max-Age and Expires on a cookie, and fail to properly clear the
      // cookie. This can cause the client to send an empty cookie back to us after we've attempted to clear it. So
      // just decode empty cookies to an empty map. See https://github.com/playframework/playframework/issues/7680.
      if (data.isEmpty) {
        Map.empty[String, String]
      } else {
        data.split("&").flatMap { pair =>
          pair.span(_ != '=') match { // "foo=bar".span(_ != '=') -> (foo,=bar)
            case (_, "") => // Skip invalid
              Option.empty[(String, String)]

            case (encName, encVal) =>
              Some(URLDecoder.decode(encName, "UTF-8") -> URLDecoder.decode(
                encVal.tail, "UTF-8"))

          }
        }(scala.collection.breakOut)
      }
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
        else {
          logger.warn("Cookie failed message authentication check")(SecurityMarkerContext)
          Map.empty[String, String]
        }
      } else urldecode(data)
    } catch {
      // fail gracefully is the session cookie is corrupted
      case NonFatal(e) =>
        logger.warn("Could not decode cookie", e)(SecurityMarkerContext)
        Map.empty[String, String]
    }
  }
}

/**
 * JWT cookie encoding and decoding functionality
 */
trait JWTCookieDataCodec extends CookieDataCodec {

  private val logger = play.api.Logger(getClass)

  def secretConfiguration: SecretConfiguration

  def jwtConfiguration: JWTConfiguration

  private lazy val formatter = new JWTCookieDataCodec.JWTFormatter(secretConfiguration, jwtConfiguration, clock)

  /**
   * Encodes the data as a `String`.
   */
  override def encode(data: Map[String, String]): String = {
    val dataMap = Map(jwtConfiguration.dataClaim -> Jwts.claims(Scala.asJava(data)))
    formatter.format(dataMap)
  }

  /**
   * Decodes from an encoded `String`.
   */
  override def decode(encodedString: String): Map[String, String] = {
    import io.jsonwebtoken._

    import scala.collection.JavaConverters._
    import scala.collection.breakOut

    try {
      // Get all the claims
      val claimMap = formatter.parse(encodedString)

      // Pull out the JWT data claim and only return that.
      val data = claimMap(jwtConfiguration.dataClaim).asInstanceOf[java.util.Map[String, AnyRef]]
      data.asScala.map{ case (k, v) => (k, v.toString) }(breakOut)
    } catch {
      case e: IllegalStateException =>
        // Used in the case where the header algorithm does not match.
        logger.error(e.getMessage)
        Map.empty

      // We want to warn specifically about premature and expired JWT,
      // because they depend on clock skew and can cause silent user error
      // if production servers get out of sync
      case e: PrematureJwtException =>
        val id = e.getClaims.getId
        logger.warn(s"decode: premature JWT found! id = $id, message = ${e.getMessage}")(SecurityMarkerContext)
        Map.empty

      case e: ExpiredJwtException =>
        val id = e.getClaims.getId
        logger.warn(s"decode: expired JWT found! id = $id, message = ${e.getMessage}")(SecurityMarkerContext)
        Map.empty

      case e: io.jsonwebtoken.SignatureException =>
        // Thrown when an invalid cookie signature is found -- this can be confusing to end users
        // so give a special logging message to indicate problem.

        logger.warn(s"decode: cookie has invalid signature! message = ${e.getMessage}")(SecurityMarkerContext)
        val devLogger = logger.forMode(Mode.Dev)
        devLogger.info(
          "The JWT signature in the cookie does not match the locally computed signature with the server. "
            + "This usually indicates the browser has a leftover cookie from another Play application, so clearing "
            + "cookies may resolve this error message.")
        Map.empty

      case NonFatal(e) =>
        logger.warn(s"decode: could not decode JWT: ${e.getMessage}", e)(SecurityMarkerContext)
        Map.empty
    }
  }

  /** The unique id of the JWT, if any. */
  protected def uniqueId(): Option[String] = Some(JWTCookieDataCodec.JWTIDGenerator.generateId())

  /** The clock used for checking expires / not before code */
  protected def clock: java.time.Clock = java.time.Clock.systemUTC()
}

object JWTCookieDataCodec {

  /**
   * Maps to and from JWT claims.  This class is more basic than the JWT
   * cookie signing, because it exposes all claims, not just the "data" ones.
   *
   * @param secretConfiguration the secret used for signing JWT
   * @param jwtConfiguration the configuration for JWT
   * @param clock the system clock
   */
  private[play] class JWTFormatter(
      secretConfiguration: SecretConfiguration,
      jwtConfiguration: JWTConfiguration,
      clock: java.time.Clock) {
    import io.jsonwebtoken._
    import scala.collection.JavaConverters._

    private val jwtClock = new io.jsonwebtoken.Clock {
      override def now(): Date = java.util.Date.from(clock.instant())
    }

    private val base64EncodedSecret: String = {
      Base64.getEncoder.encodeToString(
        secretConfiguration.secret.getBytes(StandardCharsets.UTF_8)
      )
    }

    /**
     * Parses encoded JWT against configuration, returns all JWT claims.
     *
     * @param encodedString the signed and encoded JWT.
     * @return the map of claims
     */
    def parse(encodedString: String): Map[String, AnyRef] = {
      val jws: Jws[Claims] = Jwts.parser()
        .setClock(jwtClock)
        .setSigningKey(base64EncodedSecret)
        .setAllowedClockSkewSeconds(jwtConfiguration.clockSkew.toSeconds)
        .parseClaimsJws(encodedString)

      val headerAlgorithm = jws.getHeader.getAlgorithm
      if (headerAlgorithm != jwtConfiguration.signatureAlgorithm) {
        val id = jws.getBody.getId
        val msg = s"Invalid header algorithm $headerAlgorithm in JWT $id"
        throw new IllegalStateException(msg)
      }

      jws.getBody.asScala.toMap
    }

    /**
     * Formats the input claims to a JWT string, and adds extra date related claims.
     *
     * @param claims all the claims to be added to JWT.
     * @return the signed, encoded JWT with extra date related claims
     */
    def format(claims: Map[String, AnyRef]): String = {
      val builder = Jwts.builder()
      val now = jwtClock.now()

      // Add the claims one at a time because it saves problems with mutable maps
      // under the implementation...
      claims.foreach {
        case (k, v) =>
          builder.claim(k, v)
      }

      // https://tools.ietf.org/html/rfc7519#section-4.1.4
      jwtConfiguration.expiresAfter.map { duration =>
        val expirationDate = new Date(now.getTime + duration.toMillis)
        builder.setExpiration(expirationDate)
      }

      builder.setNotBefore(now) // https://tools.ietf.org/html/rfc7519#section-4.1.5
      builder.setIssuedAt(now) // https://tools.ietf.org/html/rfc7519#section-4.1.6

      // Sign and compact into a string...
      val sigAlg = SignatureAlgorithm.valueOf(jwtConfiguration.signatureAlgorithm)
      builder.signWith(sigAlg, base64EncodedSecret).compact()
    }
  }

  /** Utility object to generate random nonces for JWT from SecureRandom */
  private[play] object JWTIDGenerator {
    private val sr = new java.security.SecureRandom()
    def generateId(): String = {
      new java.math.BigInteger(130, sr).toString(32)
    }
  }
}

/**
 * A trait that identifies the cookie encoding and uses the appropriate codec, for
 * upgrading from a signed cookie encoding to a JWT cookie encoding.
 */
trait FallbackCookieDataCodec extends CookieDataCodec {

  def jwtCodec: JWTCookieDataCodec

  def signedCodec: UrlEncodedCookieDataCodec

  def encode(data: Map[String, String]): String = {
    jwtCodec.encode(data)
  }

  def decode(encodedData: String): Map[String, String] = {
    // Per https://github.com/playframework/playframework/pull/7053#issuecomment-285220730
    encodedData match {
      case signedEncoding if signedEncoding.contains('=') =>
        //  It's a legacy session with at least one value.
        signedCodec.decode(signedEncoding)

      case jwtEncoding if jwtEncoding.contains('.') =>
        // It's a JWT session.
        jwtCodec.decode(jwtEncoding)

      case emptyLegacyEncoding =>
        // It's an empty legacy session.
        signedCodec.decode(emptyLegacyEncoding)
    }
  }
}

case class DefaultUrlEncodedCookieDataCodec(
    isSigned: Boolean,
    cookieSigner: CookieSigner
) extends UrlEncodedCookieDataCodec

case class DefaultJWTCookieDataCodec @Inject() (
    secretConfiguration: SecretConfiguration,
    jwtConfiguration: JWTConfiguration
) extends JWTCookieDataCodec

/**
 * A cookie module that uses JWT as the cookie encoding, falling back to URL encoding.
 */
class CookiesModule extends SimpleModule((env, conf) => {
  Seq(
    bind[CookieSigner].toProvider[CookieSignerProvider],
    bind[SessionCookieBaker].to[DefaultSessionCookieBaker],
    bind[FlashCookieBaker].to[DefaultFlashCookieBaker]
  )
})

/**
 * A cookie module that uses the urlencoded cookie encoding.
 */
class LegacyCookiesModule extends SimpleModule((env, conf) => {
  Seq(
    bind[CookieSigner].toProvider[CookieSignerProvider],
    bind[SessionCookieBaker].to[LegacySessionCookieBaker],
    bind[FlashCookieBaker].to[LegacyFlashCookieBaker]
  )
})
