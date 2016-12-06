/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import java.net.{ URLDecoder, URLEncoder }
import java.util.Locale
import javax.inject.Inject

import play.api._
import play.api.http._
import play.api.libs.crypto.CookieSigner
import play.mvc.Http.{ Cookie => JCookie }

import scala.collection.immutable.ListMap
import scala.util.Try
import scala.util.control.NonFatal

/**
 * An HTTP cookie.
 *
 * @param name the cookie name
 * @param value the cookie value
 * @param maxAge the cookie expiration date in seconds, `None` for a transient cookie, or a value less than 0 to expire a cookie now
 * @param path the cookie path, defaulting to the root path `/`
 * @param domain the cookie domain
 * @param secure whether this cookie is secured, sent only for HTTPS requests
 * @param httpOnly whether this cookie is HTTP only, i.e. not accessible from client-side JavaScript code
 */
case class Cookie(name: String, value: String, maxAge: Option[Int] = None, path: String = "/",
    domain: Option[String] = None, secure: Boolean = false, httpOnly: Boolean = true) {
  lazy val asJava = {
    new JCookie(name, value, maxAge.map(i => new Integer(i)).orNull, path, domain.orNull, secure, httpOnly)
  }
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
object Cookies extends CookieHeaderEncoding {

  // Use global state for cookie header configuration
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
    if (cookieHeader.isEmpty) {
      // fail fast if there are no existing cookies
      Seq.empty
    } else {
      Try {
        val decoder = config.clientDecoder
        for {
          cookieString <- SetCookieHeaderSeparatorRegex.split(cookieHeader).toSeq
          cookie <- Option(decoder.decode(cookieString.trim))
        } yield Cookie(
          cookie.name,
          cookie.value,
          if (cookie.maxAge == Integer.MIN_VALUE) None else Some(cookie.maxAge),
          Option(cookie.path).getOrElse("/"),
          Option(cookie.domain),
          cookie.isSecure,
          cookie.isHttpOnly
        )
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
