/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import play.api.http.{ HttpConfiguration, SessionConfiguration }

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
  private def config: SessionConfiguration = HttpConfiguration.current.session

  def COOKIE_NAME = config.cookieName

  val emptyCookie = new Session
  override val isSigned = true
  override def secure = config.secure
  override def maxAge = config.maxAge.map(_.toSeconds.toInt)
  override def httpOnly = config.httpOnly
  override def path = HttpConfiguration.current.context
  override def domain = config.domain
  override def cookieSigner = play.api.libs.Crypto.cookieSigner

  def deserialize(data: Map[String, String]) = new Session(data)

  def serialize(session: Session) = session.data
}
