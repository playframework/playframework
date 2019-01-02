/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.crypto

import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.{ Inject, Provider, Singleton }

import play.api.http.SecretConfiguration
import play.api.libs.Codecs
import play.libs.crypto

/**
 * Authenticates a cookie by returning a message authentication code (MAC).
 *
 * This trait should not be used as a general purpose MAC utility.
 */
trait CookieSigner {

  /**
   * Signs (MAC) the given String using the given secret key.
   *
   * By default this uses the platform default JCE provider.  This can be overridden by defining
   * `play.http.secret.provider` in `application.conf`.
   *
   * @param message The message to sign.
   * @param key     The private key to sign with.
   * @return A hexadecimal encoded signature.
   */
  def sign(message: String, key: Array[Byte]): String

  /**
   * Signs (MAC) the given String using the application’s secret key.
   *
   * By default this uses the platform default JCE provider.  This can be overridden by defining
   * `play.http.secret.provider` in `application.conf`.
   *
   * @param message The message to sign.
   * @return A hexadecimal encoded signature.
   */
  def sign(message: String): String

  /**
   * @return the Java version for this cookie signer.
   */
  def asJava: play.libs.crypto.CookieSigner = {
    new crypto.DefaultCookieSigner(this)
  }
}

@Singleton
class CookieSignerProvider @Inject() (secretConfiguration: SecretConfiguration) extends Provider[CookieSigner] {
  lazy val get: CookieSigner = new DefaultCookieSigner(secretConfiguration)
}

/**
 * Uses an HMAC-SHA1 for signing cookies.
 */
class DefaultCookieSigner @Inject() (secretConfiguration: SecretConfiguration) extends CookieSigner {

  private lazy val HmacSHA1 = "HmacSHA1"

  /**
   * Signs the given String with HMAC-SHA1 using the given key.
   *
   * By default this uses the platform default JSSE provider.  This can be overridden by defining
   * `play.http.secret.provider` in `application.conf`.
   *
   * @param message The message to sign.
   * @param key The private key to sign with.
   * @return A hexadecimal encoded signature.
   */
  def sign(message: String, key: Array[Byte]): String = {
    val mac = secretConfiguration.provider.fold(Mac.getInstance(HmacSHA1))(p => Mac.getInstance(HmacSHA1, p))
    mac.init(new SecretKeySpec(key, HmacSHA1))
    Codecs.toHexString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)))
  }

  /**
   * Signs the given String with HMAC-SHA1 using the application’s secret key.
   *
   * By default this uses the platform default JSSE provider.  This can be overridden by defining
   * `play.http.secret.provider` in `application.conf`.
   *
   * @param message The message to sign.
   * @return A hexadecimal encoded signature.
   */
  def sign(message: String): String = {
    sign(message, secretConfiguration.secret.getBytes(StandardCharsets.UTF_8))
  }

}

