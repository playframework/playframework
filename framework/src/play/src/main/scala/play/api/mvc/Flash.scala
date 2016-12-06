/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import javax.inject.Inject

import play.api.http.{ FlashConfiguration, HttpConfiguration, SessionConfiguration }
import play.mvc.Http

import scala.collection.JavaConverters._

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

  lazy val asJava: Http.Flash = new Http.Flash(data.asJava)
}

/**
 * Helper utilities to manage the Flash cookie.
 */
trait FlashCookieBaker extends CookieBaker[Flash] {

  def config: FlashConfiguration
  def sessionConfig: SessionConfiguration

  def COOKIE_NAME = config.cookieName

  lazy val emptyCookie = new Flash

  override def path = HttpConfiguration.current.context
  override def secure = config.secure
  override def httpOnly = config.httpOnly
  override def domain = sessionConfig.domain

  override def cookieSigner = play.api.libs.Crypto.cookieSigner

  def deserialize(data: Map[String, String]) = new Flash(data)

  def serialize(flash: Flash) = flash.data

}

class DefaultFlashCookieBaker @Inject() (val config: FlashConfiguration, val sessionConfig: SessionConfiguration)
    extends FlashCookieBaker {
  def this() = this(FlashConfiguration(), SessionConfiguration())
}

object Flash extends FlashCookieBaker {
  def config = HttpConfiguration.current.flash
  def sessionConfig = HttpConfiguration.current.session
  def fromJavaFlash(javaFlash: play.mvc.Http.Flash): Flash = new Flash(javaFlash.asScala.toMap)
}