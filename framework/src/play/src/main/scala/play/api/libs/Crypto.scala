/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs

import javax.crypto._
import javax.crypto.spec.SecretKeySpec

import play.api.{ Mode, Play, PlayException }
import java.security.SecureRandom
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.Md5Crypt

/**
 * Cryptographic utilities.
 *
 * These utilities are intended as a convenience, however it is important to read each methods documentation and
 * understand the concepts behind encryption to use this class properly.  Safe encryption is hard, and there is no
 * substitute for an adequate understanding of cryptography.  These methods will not be suitable for all encryption
 * needs.
 *
 * For more information about cryptography, we recommend reading the OWASP Cryptographic Storage Cheatsheet:
 *
 * https://www.owasp.org/index.php/Cryptographic_Storage_Cheat_Sheet
 */
object Crypto {

  private def getConfig(key: String) = Play.maybeApplication.flatMap(_.configuration.getString(key))

  private val Blank = """\s*""".r

  private[play] def secret: String = {
    /*
     * The Play secret.
     *
     * We want to:
     *
     * 1) Encourage the practice of *not* using the same secret in dev and prod.
     * 2) Make it obvious that the secret should be changed.
     * 3) Ensure that in dev mode, the secret stays stable across restarts.
     * 4) Ensure that in dev mode, sessions do not interfere with other applications that may be or have been running
     *   on localhost.  Eg, if I start Play app 1, and it stores a PLAY_SESSION cookie for localhost:9000, then I stop
     *   it, and start Play app 2, when it reads the PLAY_SESSION cookie for localhost:9000, it should not see the
     *   session set by Play app 1.  This can be achieved by using different secrets for the two, since if they are
     *   different, they will simply ignore the session cookie set by the other.
     *
     * To achieve 1 and 2, we will, in Activator templates, set the default secret to be "changeme".  This should make
     * it obvious that the secret needs to be changed and discourage using the same secret in dev and prod.
     *
     * For safety, if the secret is not set, or if it's changeme, and we are in prod mode, then we will fail fatally.
     * This will further enforce both 1 and 2.
     *
     * To achieve 3, if in dev or test mode, if the secret is either changeme or not set, we will generate a secret
     * based on the location of application.conf.  This should be stable across restarts for a given application.
     *
     * To achieve 4, using the location of application.conf to generate the secret should ensure this.
     */

    val app = Play.current

    app.configuration.getString("application.secret") match {
      case (Some("changeme") | Some(Blank()) | None) if app.mode == Mode.Prod =>
        Play.logger.error("The application secret has not been set, and we are in prod mode. Your application is not secure.")
        Play.logger.error("To set the application secret, please read http://playframework.com/documentation/latest/ApplicationSecret")
        throw new PlayException("Configuration error", "Application secret not set")
      case Some("changeme") | Some(Blank()) | None =>
        // Try to generate a stable secret. Security is not the issue here, since this is just for tests and dev mode.
        val applicationConfLocation = app.classloader.getResource("application.conf")
        val secret = if (applicationConfLocation == null) {
          // No application.conf?  Oh well, just use something hard coded.
          "she sells sea shells on the sea shore"
        } else {
          applicationConfLocation.toString
        }
        val md5Secret = Md5Crypt.md5Crypt(secret.getBytes("utf-8"))
        Play.logger.debug(s"Generated dev mode secret ${md5Secret} for app at ${Option(applicationConfLocation).getOrElse("unknown location")}")
        md5Secret
      case Some(s) => s
    }
  }

  private lazy val provider: Option[String] = getConfig("application.crypto.provider")

  private lazy val transformation: String = getConfig("application.crypto.aes.transformation").getOrElse("AES")

  private val random = new SecureRandom()

  /**
   * Signs the given String with HMAC-SHA1 using the given key.
   *
   * By default this uses the platform default JSSE provider.  This can be overridden by defining
   * `application.crypto.provider` in `application.conf`.
   *
   * @param message The message to sign.
   * @param key The private key to sign with.
   * @return A hexadecimal encoded signature.
   */
  def sign(message: String, key: Array[Byte]): String = {
    val mac = provider.map(p => Mac.getInstance("HmacSHA1", p)).getOrElse(Mac.getInstance("HmacSHA1"))
    mac.init(new SecretKeySpec(key, "HmacSHA1"))
    Codecs.toHexString(mac.doFinal(message.getBytes("utf-8")))
  }

  /**
   * Signs the given String with HMAC-SHA1 using the application’s secret key.
   *
   * By default this uses the platform default JSSE provider.  This can be overridden by defining
   * `application.crypto.provider` in `application.conf`.
   *
   * @param message The message to sign.
   * @return A hexadecimal encoded signature.
   */
  def sign(message: String): String = {
    sign(message, secret.getBytes("utf-8"))
  }

  /**
   * Sign a token.  This produces a new token, that has this token signed with a nonce.
   *
   * This primarily exists to defeat the BREACH vulnerability, as it allows the token to effectively be random per
   * request, without actually changing the value.
   *
   * @param token The token to sign
   * @return The signed token
   */
  def signToken(token: String): String = {
    val nonce = System.currentTimeMillis()
    val joined = nonce + "-" + token
    sign(joined) + "-" + joined
  }

  /**
   * Extract a signed token that was signed by [[play.api.libs.Crypto.signToken]].
   *
   * @param token The signed token to extract.
   * @return The verified raw token, or None if the token isn't valid.
   */
  def extractSignedToken(token: String): Option[String] = {
    token.split("-", 3) match {
      case Array(signature, nonce, raw) if constantTimeEquals(signature, sign(nonce + "-" + raw)) => Some(raw)
      case _ => None
    }
  }

  /**
   * Generate a cryptographically secure token
   */
  def generateToken = {
    val bytes = new Array[Byte](12)
    random.nextBytes(bytes)
    new String(Hex.encodeHex(bytes))
  }

  /**
   * Generate a signed token
   */
  def generateSignedToken = signToken(generateToken)

  /**
   * Compare two signed tokens
   */
  def compareSignedTokens(tokenA: String, tokenB: String) = {
    (for {
      rawA <- extractSignedToken(tokenA)
      rawB <- extractSignedToken(tokenB)
    } yield constantTimeEquals(rawA, rawB)).getOrElse(false)
  }

  /**
   * Constant time equals method.
   *
   * Given a length that both Strings are equal to, this method will always run in constant time.  This prevents
   * timing attacks.
   */
  def constantTimeEquals(a: String, b: String) = {
    if (a.length != b.length) {
      false
    } else {
      var equal = 0
      for (i <- 0 until a.length) {
        equal |= a(i) ^ b(i)
      }
      equal == 0
    }
  }

  /**
   * Encrypt a String with the AES encryption standard using the application's secret key.
   *
   * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
   * `application.crypto.provider` in `application.conf`.
   *
   * The transformation algorithm used is the provider specific implementation of the `AES` name.  On Oracles JDK,
   * this is `AES/ECB/PKCS5Padding`.  This algorithm is suitable for small amounts of data, typically less than 32
   * bytes, hence is useful for encrypting credit card numbers, passwords etc.  For larger blocks of data, this
   * algorithm may expose patterns and be vulnerable to repeat attacks.
   *
   * The transformation algorithm can be configured by defining `application.crypto.aes.transformation` in
   * `application.conf`.  Although any cipher transformation algorithm can be selected here, the secret key spec used
   * is always AES, so only AES transformation algorithms will work.
   *
   * @param value The String to encrypt.
   * @return An hexadecimal encrypted string.
   */
  def encryptAES(value: String): String = {
    encryptAES(value, secret.substring(0, 16))
  }

  /**
   * Encrypt a String with the AES encryption standard and the supplied private key.
   *
   * The private key must have a length of 16 bytes.
   *
   * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
   * `application.crypto.provider` in `application.conf`.
   *
   * The transformation algorithm used is the provider specific implementation of the `AES` name.  On Oracles JDK,
   * this is `AES/ECB/PKCS5Padding`.  This algorithm is suitable for small amounts of data, typically less than 32
   * bytes, hence is useful for encrypting credit card numbers, passwords etc.  For larger blocks of data, this
   * algorithm may expose patterns and be vulnerable to repeat attacks.
   *
   * The transformation algorithm can be configured by defining `application.crypto.aes.transformation` in
   * `application.conf`.  Although any cipher transformation algorithm can be selected here, the secret key spec used
   * is always AES, so only AES transformation algorithms will work.
   *
   * @param value The String to encrypt.
   * @param privateKey The key used to encrypt.
   * @return An hexadecimal encrypted string.
   */
  def encryptAES(value: String, privateKey: String): String = {
    val raw = privateKey.getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = provider.map(p => Cipher.getInstance(transformation, p)).getOrElse(Cipher.getInstance(transformation))
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
    Codecs.toHexString(cipher.doFinal(value.getBytes("utf-8")))
  }

  /**
   * Decrypt a String with the AES encryption standard using the application's secret key.
   *
   * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
   * `application.crypto.provider` in `application.conf`.
   *
   * The transformation used is by default `AES/ECB/PKCS5Padding`.  It can be configured by defining
   * `application.crypto.aes.transformation` in `application.conf`.  Although any cipher transformation algorithm can
   * be selected here, the secret key spec used is always AES, so only AES transformation algorithms will work.
   *
   * @param value An hexadecimal encrypted string.
   * @return The decrypted String.
   */
  def decryptAES(value: String): String = {
    decryptAES(value, secret.substring(0, 16))
  }

  /**
   * Decrypt a String with the AES encryption standard.
   *
   * The private key must have a length of 16 bytes.
   *
   * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
   * `application.crypto.provider` in `application.conf`.
   *
   * The transformation used is by default `AES/ECB/PKCS5Padding`.  It can be configured by defining
   * `application.crypto.aes.transformation` in `application.conf`.  Although any cipher transformation algorithm can
   * be selected here, the secret key spec used is always AES, so only AES transformation algorithms will work.
   *
   * @param value An hexadecimal encrypted string.
   * @param privateKey The key used to encrypt.
   * @return The decrypted String.
   */
  def decryptAES(value: String, privateKey: String): String = {
    val raw = privateKey.getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = provider.map(p => Cipher.getInstance(transformation, p)).getOrElse(Cipher.getInstance(transformation))
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    new String(cipher.doFinal(Codecs.hexStringToByte(value)))
  }

}
