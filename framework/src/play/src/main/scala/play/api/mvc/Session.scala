/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import javax.inject.Inject

import play.api.http.{ HttpConfiguration, SecretConfiguration, SessionConfiguration }
import play.api.libs.crypto.{ CookieSigner, CookieSignerProvider }
import play.mvc.Http

import scala.collection.JavaConverters._

/**
 * HTTP Session.
 *
 * Session data are encoded into an HTTP cookie, and can only contain simple `String` values.
 */
case class Session(data: Map[String, String] = Map.empty[String, String]) {

  /**
   * Optionally returns the session value associated with a key.
   */
  def get(key: String): Option[String] = data.get(key)

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
  def +(kv: (String, String)): Session = {
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
  def -(key: String): Session = copy(data - key)

  /**
   * Retrieves the session value which is associated with the given key.
   */
  def apply(key: String): String = data(key)

  lazy val asJava: Http.Session = new Http.Session(data.asJava)
}

/**
 * Helper utilities to manage the Session cookie.
 */
trait SessionCookieBaker extends CookieBaker[Session] with CookieDataCodec {

  def config: SessionConfiguration

  def COOKIE_NAME: String = config.cookieName

  lazy val emptyCookie = new Session

  override val isSigned = true
  override def secure: Boolean = config.secure
  override def maxAge: Option[Int] = config.maxAge.map(_.toSeconds.toInt)
  override def httpOnly: Boolean = config.httpOnly
  override def path: String = config.path
  override def domain: Option[String] = config.domain
  override def sameSite = config.sameSite

  def deserialize(data: Map[String, String]) = new Session(data)

  def serialize(session: Session): Map[String, String] = session.data
}

/**
 * A session cookie that reads in both signed and JWT cookies, and writes out JWT cookies.
 */
class DefaultSessionCookieBaker @Inject() (
    val config: SessionConfiguration,
    val secretConfiguration: SecretConfiguration,
    cookieSigner: CookieSigner)
  extends SessionCookieBaker with FallbackCookieDataCodec {

  override val jwtCodec: JWTCookieDataCodec = DefaultJWTCookieDataCodec(secretConfiguration, config.jwt)
  override val signedCodec: UrlEncodedCookieDataCodec = DefaultUrlEncodedCookieDataCodec(isSigned, cookieSigner)

  def this() = this(SessionConfiguration(), SecretConfiguration(), new CookieSignerProvider(SecretConfiguration()).get)
}

/**
 * A session cookie baker that signs the session cookie in the Play 2.5.x style.
 *
 * @param config session configuration
 * @param cookieSigner the cookie signer, typically HMAC-SHA1
 */
class LegacySessionCookieBaker @Inject() (val config: SessionConfiguration, val cookieSigner: CookieSigner) extends SessionCookieBaker with UrlEncodedCookieDataCodec {
  def this() = this(SessionConfiguration(), new CookieSignerProvider(SecretConfiguration()).get)
}

object Session extends CookieBaker[Session] with FallbackCookieDataCodec {

  lazy val emptyCookie = new Session

  def fromJavaSession(javaSession: play.mvc.Http.Session): Session = new Session(javaSession.asScala.toMap)

  @deprecated("Inject play.api.mvc.SessionCookieBaker instead", "2.6.0")
  def config: SessionConfiguration = HttpConfiguration.current.session

  @deprecated("Inject play.api.mvc.SessionCookieBaker instead", "2.6.0")
  override lazy val jwtCodec = DefaultJWTCookieDataCodec(HttpConfiguration.current.secret, config.jwt)

  @deprecated("Inject play.api.mvc.SessionCookieBaker instead", "2.6.0")
  override lazy val signedCodec = DefaultUrlEncodedCookieDataCodec(isSigned, play.api.libs.Crypto.cookieSigner)

  @deprecated("Inject play.api.mvc.SessionCookieBaker instead", "2.6.0")
  override val isSigned: Boolean = true

  @deprecated("Inject play.api.mvc.SessionCookieBaker instead", "2.6.0")
  override def COOKIE_NAME: String = config.cookieName

  @deprecated("Inject play.api.mvc.SessionCookieBaker instead", "2.6.0")
  override def secure: Boolean = config.secure

  @deprecated("Inject play.api.mvc.SessionCookieBaker instead", "2.6.0")
  override def maxAge: Option[Int] = config.maxAge.map(_.toSeconds.toInt)

  @deprecated("Inject play.api.mvc.SessionCookieBaker instead", "2.6.0")
  override def httpOnly: Boolean = config.httpOnly

  @deprecated("Inject play.api.mvc.SessionCookieBaker instead", "2.6.0")
  override def path: String = HttpConfiguration.current.context

  @deprecated("Inject play.api.mvc.SessionCookieBaker instead", "2.6.0")
  override def domain: Option[String] = config.domain

  @deprecated("Inject play.api.mvc.SessionCookieBaker instead", "2.6.0")
  override def sameSite: Option[Cookie.SameSite] = config.sameSite

  @deprecated("Inject play.api.mvc.SessionCookieBaker instead", "2.6.0")
  override def deserialize(data: Map[String, String]) = new Session(data)

  @deprecated("Inject play.api.mvc.SessionCookieBaker instead", "2.6.0")
  override def serialize(session: Session): Map[String, String] = session.data
}
