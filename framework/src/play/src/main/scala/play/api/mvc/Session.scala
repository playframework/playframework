/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
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
trait SessionCookieBaker extends CookieBaker[Session] {

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
  override val signedCodec: SignedCookieDataCodec = DefaultSignedCookieDataCodec(isSigned, cookieSigner)

  def this() = this(SessionConfiguration(), SecretConfiguration(), new CookieSignerProvider(SecretConfiguration()).get)
}

/**
 * A session cookie baker that signs the session cookie in the Play 2.5.x style.
 *
 * @param config session configuration
 * @param cookieSigner the cookie signer, typically HMAC-SHA1
 */
class LegacySessionCookieBaker @Inject() (val config: SessionConfiguration, val cookieSigner: CookieSigner) extends SessionCookieBaker with SignedCookieDataCodec {
  def this() = this(SessionConfiguration(), new CookieSignerProvider(SecretConfiguration()).get)
}

@deprecated("Inject [[play.api.mvc.SessionCookieBaker]] instead", "2.6.0")
object Session extends SessionCookieBaker with FallbackCookieDataCodec {
  def config: SessionConfiguration = HttpConfiguration.current.session
  def fromJavaSession(javaSession: play.mvc.Http.Session): Session = new Session(javaSession.asScala.toMap)
  override def path: String = HttpConfiguration.current.context

  override lazy val jwtCodec = DefaultJWTCookieDataCodec(HttpConfiguration.current.secret, config.jwt)
  override lazy val signedCodec = DefaultSignedCookieDataCodec(isSigned, play.api.libs.Crypto.cookieSigner)
}
