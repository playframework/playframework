/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import javax.inject.Inject

import play.api.http.HttpConfiguration
import play.api.http.SecretConfiguration
import play.api.http.SessionConfiguration
import play.api.libs.crypto.CookieSigner
import play.api.libs.crypto.CookieSignerProvider
import play.mvc.Http

import scala.annotation.varargs

/**
 * HTTP Session.
 *
 * Session data are encoded into an HTTP cookie, and can only contain simple `String` values.
 */
case class Session(data: Map[String, String] = Map.empty) {

  /**
   * Optionally returns the session value associated with a key.
   */
  def get(key: String): Option[String] = data.get(key)

  /**
   * Retrieves the session value associated with the given key.
   *
   * @throws NoSuchElementException if no value exists for the key.
   */
  def apply(key: String): String = data(key)

  /**
   * Returns `true` if this session is empty.
   */
  def isEmpty: Boolean = data.isEmpty

  /**
   * Returns a new session with the given key-value pair added.
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
    require(kv._2 != null, s"Session value for ${kv._1} cannot be null")
    copy(data + kv)
  }

  /**
   * Returns a new session with elements added from the given `Iterable`.
   *
   * @param kvs an `Iterable` containing key-value pairs to add.
   */
  def ++(kvs: Iterable[(String, String)]): Session = {
    for ((k, v) <- kvs) require(v != null, s"Session value for $k cannot be null")
    copy(data ++ kvs)
  }

  /**
   * Returns a new session with the given key removed.
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
   * Returns a new session with the given keys removed.
   *
   * For example:
   * {{{
   * session -- Seq("username", "name")
   * }}}
   *
   * @param keys the keys to remove
   * @return the modified session
   */
  def --(keys: Iterable[String]): Session = copy(data -- keys)

  lazy val asJava: Http.Session = new Http.Session(this)
}

/**
 * Helper utilities to manage the Session cookie.
 */
trait SessionCookieBaker extends CookieBaker[Session] with CookieDataCodec {
  def config: SessionConfiguration

  def COOKIE_NAME: String = config.cookieName

  lazy val emptyCookie = new Session

  override val isSigned               = true
  override def secure: Boolean        = config.secure
  override def maxAge: Option[Int]    = config.maxAge.map(_.toSeconds.toInt)
  override def httpOnly: Boolean      = config.httpOnly
  override def path: String           = config.path
  override def domain: Option[String] = config.domain
  override def sameSite               = config.sameSite

  def deserialize(data: Map[String, String]) = new Session(data)

  def serialize(session: Session): Map[String, String] = session.data
}

/**
 * A session cookie that reads in both signed and JWT cookies, and writes out JWT cookies.
 */
class DefaultSessionCookieBaker @Inject() (
    val config: SessionConfiguration,
    val secretConfiguration: SecretConfiguration,
    cookieSigner: CookieSigner
) extends SessionCookieBaker
    with FallbackCookieDataCodec {
  override val jwtCodec: JWTCookieDataCodec           = DefaultJWTCookieDataCodec(secretConfiguration, config.jwt)
  override val signedCodec: UrlEncodedCookieDataCodec = DefaultUrlEncodedCookieDataCodec(isSigned, cookieSigner)

  def this() = this(SessionConfiguration(), SecretConfiguration(), new CookieSignerProvider(SecretConfiguration()).get)
}

/**
 * A session cookie baker that signs the session cookie in the Play 2.5.x style.
 *
 * @param config session configuration
 * @param cookieSigner the cookie signer, typically HMAC-SHA1
 */
class LegacySessionCookieBaker @Inject() (val config: SessionConfiguration, val cookieSigner: CookieSigner)
    extends SessionCookieBaker
    with UrlEncodedCookieDataCodec {
  def this() = this(SessionConfiguration(), new CookieSignerProvider(SecretConfiguration()).get)
}

object Session {
  lazy val emptyCookie = new Session

  def fromJavaSession(javaSession: play.mvc.Http.Session): Session = javaSession.asScala
}
