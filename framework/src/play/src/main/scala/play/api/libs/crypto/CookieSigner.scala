/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.crypto

import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.{ Inject, Provider, Singleton }

import ove.crypto.digest.Blake2b
import play.api.http.SecretConfiguration
import play.api.libs.Codecs

/**
 * Authenticates a cookie by returning a message authentication code (MAC).
 *
 * This trait should not be used as a general purpose MAC utility.
 */
trait CookieSigner {

  /**
   * Signs (MAC) the given String using the given secret key.
   *
   * @param message The message to sign.
   * @param key     The private key to sign with.
   * @return A hexadecimal encoded signature.
   */
  def sign(message: String, key: Array[Byte]): String

  /**
   * Signs (MAC) the given String using the application’s secret key.
   *
   * @param message The message to sign.
   * @return A hexadecimal encoded signature.
   */
  def sign(message: String): String
}

@Singleton
class CookieSignerProvider @Inject() (secretConfiguration: SecretConfiguration) extends Provider[CookieSigner] {
  lazy val get: CookieSigner = new DefaultCookieSigner(secretConfiguration)
}

/**
 * The default cookie signer.
 *
 * This is configured for <a href="https://blake2.net/">BLAKE2b in keyed mode</a> out of the
 * box if secretConfiguration.mac is "blake2b" or None, but will use JCA algorithm with a Mac otherwise.
 */
class DefaultCookieSigner(secretConfiguration: SecretConfiguration) extends CookieSigner {

  def mac: String = secretConfiguration.mac.getOrElse("blake2b")

  /**
   * Signs the given String using the given key and the provided algorithm.
   *
   * @param message The message to sign.
   * @param key The private key to sign with.
   * @return A encoded signature.
   */
  def sign(message: String, key: Array[Byte]): String = {
    mac match {
      case "blake2b" =>
        val mac = Blake2b.Mac.newInstance(key)
        Codecs.toHexString(mac.digest(message.getBytes(StandardCharsets.UTF_8)))

      case algorithm =>
        val mac = secretConfiguration.provider.fold(Mac.getInstance(algorithm))(p => Mac.getInstance(algorithm, p))
        mac.init(new SecretKeySpec(key, algorithm))
        Codecs.toHexString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)))
    }
  }

  /**
   * Signs the given String using the application’s secret key.
   *
   * @param message The message to sign.
   * @return A encoded signature.
   */
  def sign(message: String): String = {
    sign(message, secretConfiguration.secret.getBytes(StandardCharsets.UTF_8))
  }

}

