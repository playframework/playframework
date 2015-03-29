/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs

import java.security.{ MessageDigest, SecureRandom }
import javax.crypto._
import javax.crypto.spec.{ IvParameterSpec, SecretKeySpec }
import javax.inject.{ Inject, Provider, Singleton }

import org.apache.commons.codec.binary.{ Base64, Hex }
import org.apache.commons.codec.digest.DigestUtils
import play.api._
import play.api.libs.Crypto.CryptoException

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

  /**
   * Exception thrown by the Crypto APIs.
   * @param message The error message.
   * @param throwable The Throwable associated with the exception.
   */
  class CryptoException(val message: String = null, val throwable: Throwable = null) extends RuntimeException(message, throwable)

  private val cryptoCache = Application.instanceCache[Crypto]
  private def crypto = {
    Play.maybeApplication.fold(
      new Crypto(new CryptoConfigParser(
        Environment.simple(), Configuration.from(Map("play.crypto.aes.transformation" -> "AES/CTR/NoPadding"))
      ).get)
    )(cryptoCache)
  }

  /**
   * Signs the given String with HMAC-SHA1 using the given key.
   *
   * By default this uses the platform default JSSE provider.  This can be overridden by defining
   * `play.crypto.provider` in `application.conf`.
   *
   * @param message The message to sign.
   * @param key The private key to sign with.
   * @return A hexadecimal encoded signature.
   */
  def sign(message: String, key: Array[Byte]): String = crypto.sign(message, key)

  /**
   * Signs the given String with HMAC-SHA1 using the application’s secret key.
   *
   * By default this uses the platform default JSSE provider.  This can be overridden by defining
   * `play.crypto.provider` in `application.conf`.
   *
   * @param message The message to sign.
   * @return A hexadecimal encoded signature.
   */
  def sign(message: String): String = crypto.sign(message)

  /**
   * Sign a token.  This produces a new token, that has this token signed with a nonce.
   *
   * This primarily exists to defeat the BREACH vulnerability, as it allows the token to effectively be random per
   * request, without actually changing the value.
   *
   * @param token The token to sign
   * @return The signed token
   */
  def signToken(token: String): String = crypto.signToken(token)

  /**
   * Extract a signed token that was signed by [[play.api.libs.Crypto.signToken]].
   *
   * @param token The signed token to extract.
   * @return The verified raw token, or None if the token isn't valid.
   */
  def extractSignedToken(token: String): Option[String] = crypto.extractSignedToken(token)

  /**
   * Generate a cryptographically secure token
   */
  def generateToken: String = crypto.generateToken

  /**
   * Generate a signed token
   */
  def generateSignedToken: String = crypto.generateSignedToken

  /**
   * Compare two signed tokens
   */
  def compareSignedTokens(tokenA: String, tokenB: String): Boolean = crypto.compareSignedTokens(tokenA, tokenB)

  /**
   * Constant time equals method.
   *
   * Given a length that both Strings are equal to, this method will always run in constant time.  This prevents
   * timing attacks.
   */
  def constantTimeEquals(a: String, b: String): Boolean = {
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
   * `play.crypto.provider` in `application.conf`.
   *
   * The transformation algorithm used is the provider specific implementation of the `AES` name.  On Oracles JDK,
   * this is `AES/CTR/NoPadding`.  This algorithm is suitable for small amounts of data, typically less than 32
   * bytes, hence is useful for encrypting credit card numbers, passwords etc.  For larger blocks of data, this
   * algorithm may expose patterns and be vulnerable to repeat attacks.
   *
   * The transformation algorithm can be configured by defining `play.crypto.aes.transformation` in
   * `application.conf`.  Although any cipher transformation algorithm can be selected here, the secret key spec used
   * is always AES, so only AES transformation algorithms will work.
   *
   * @param value The String to encrypt.
   * @return An hexadecimal encrypted string.
   */
  def encryptAES(value: String): String = crypto.encryptAES(value)

  /**
   * Encrypt a String with the AES encryption standard and the supplied private key.
   *
   * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
   * `play.crypto.provider` in `application.conf`.
   *
   * The transformation algorithm used is the provider specific implementation of the `AES` name.  On Oracles JDK,
   * this is `AES/CTR/NoPadding`.  This algorithm is suitable for small amounts of data, typically less than 32
   * bytes, hence is useful for encrypting credit card numbers, passwords etc.  For larger blocks of data, this
   * algorithm may expose patterns and be vulnerable to repeat attacks.
   *
   * The transformation algorithm can be configured by defining `play.crypto.aes.transformation` in
   * `application.conf`.  Although any cipher transformation algorithm can be selected here, the secret key spec used
   * is always AES, so only AES transformation algorithms will work.
   *
   * @param value The String to encrypt.
   * @param privateKey The key used to encrypt.
   * @return An hexadecimal encrypted string.
   */
  def encryptAES(value: String, privateKey: String): String = crypto.encryptAES(value, privateKey)

  /**
   * Decrypt a String with the AES encryption standard using the application's secret key.
   *
   * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
   * `play.crypto.provider` in `application.conf`.
   *
   * The transformation used is by default `AES/CTR/NoPadding`.  It can be configured by defining
   * `play.crypto.aes.transformation` in `application.conf`.  Although any cipher transformation algorithm can
   * be selected here, the secret key spec used is always AES, so only AES transformation algorithms will work.
   *
   * @param value An hexadecimal encrypted string.
   * @return The decrypted String.
   */
  def decryptAES(value: String): String = crypto.decryptAES(value)

  /**
   * Decrypt a String with the AES encryption standard.
   *
   * The private key must have a length of 16 bytes.
   *
   * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
   * `play.crypto.provider` in `application.conf`.
   *
   * The transformation used is by default `AES/CTR/NoPadding`.  It can be configured by defining
   * `play.crypto.aes.transformation` in `application.conf`.  Although any cipher transformation algorithm can
   * be selected here, the secret key spec used is always AES, so only AES transformation algorithms will work.
   *
   * @param value An hexadecimal encrypted string.
   * @param privateKey The key used to encrypt.
   * @return The decrypted String.
   */
  def decryptAES(value: String, privateKey: String): String = crypto.decryptAES(value, privateKey)
}

@Singleton
class CryptoConfigParser @Inject() (environment: Environment, configuration: Configuration) extends Provider[CryptoConfig] {

  private val Blank = """\s*""".r

  private val logger = Logger(classOf[CryptoConfigParser])

  lazy val get = {

    val config = PlayConfig(configuration)

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
    val secret = config.getOptionalDeprecated[String]("play.crypto.secret", "application.secret") match {
      case (Some("changeme") | Some(Blank()) | None) if environment.mode == Mode.Prod =>
        logger.error("The application secret has not been set, and we are in prod mode. Your application is not secure.")
        logger.error("To set the application secret, please read http://playframework.com/documentation/latest/ApplicationSecret")
        throw new PlayException("Configuration error", "Application secret not set")
      case Some("changeme") | Some(Blank()) | None =>
        val appConfLocation = environment.resource("application.conf")
        // Try to generate a stable secret. Security is not the issue here, since this is just for tests and dev mode.
        val secret = appConfLocation.fold(
          // No application.conf?  Oh well, just use something hard coded.
          "she sells sea shells on the sea shore"
        )(_.toString)
        val md5Secret = DigestUtils.md5Hex(secret)
        logger.debug(s"Generated dev mode secret $md5Secret for app at ${appConfLocation.getOrElse("unknown location")}")
        md5Secret
      case Some(s) => s
    }

    val provider = config.getOptional[String]("play.crypto.provider")
    val transformation = config.get[String]("play.crypto.aes.transformation")

    CryptoConfig(secret, provider, transformation)
  }
}

/**
 * Configuration for Crypto
 *
 * @param secret The application secret
 * @param aesTransformation The AES transformation to use
 * @param provider The crypto provider to use
 */
case class CryptoConfig(
  secret: String,
  provider: Option[String] = None,
  aesTransformation: String = "AES/CTR/NoPadding")

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
@Singleton
class Crypto @Inject() (config: CryptoConfig) {

  private val random = new SecureRandom()

  /**
   * Signs the given String with HMAC-SHA1 using the given key.
   *
   * By default this uses the platform default JSSE provider.  This can be overridden by defining
   * `play.crypto.provider` in `application.conf`.
   *
   * @param message The message to sign.
   * @param key The private key to sign with.
   * @return A hexadecimal encoded signature.
   */
  def sign(message: String, key: Array[Byte]): String = {
    val mac = config.provider.fold(Mac.getInstance("HmacSHA1"))(p => Mac.getInstance("HmacSHA1", p))
    mac.init(new SecretKeySpec(key, "HmacSHA1"))
    Codecs.toHexString(mac.doFinal(message.getBytes("utf-8")))
  }

  /**
   * Signs the given String with HMAC-SHA1 using the application’s secret key.
   *
   * By default this uses the platform default JSSE provider.  This can be overridden by defining
   * `play.crypto.provider` in `application.conf`.
   *
   * @param message The message to sign.
   * @return A hexadecimal encoded signature.
   */
  def sign(message: String): String = {
    sign(message, config.secret.getBytes("utf-8"))
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
  def generateToken: String = {
    val bytes = new Array[Byte](12)
    random.nextBytes(bytes)
    new String(Hex.encodeHex(bytes))
  }

  /**
   * Generate a signed token
   */
  def generateSignedToken: String = signToken(generateToken)

  /**
   * Compare two signed tokens
   */
  def compareSignedTokens(tokenA: String, tokenB: String): Boolean = {
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
  def constantTimeEquals(a: String, b: String): Boolean = {
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
   * `play.crypto.provider` in `application.conf`.
   *
   * The transformation algorithm used is the provider specific implementation of the `AES` name.  On Oracles JDK,
   * this is `AES/CTR/NoPadding`.  This algorithm is suitable for small amounts of data, typically less than 32
   * bytes, hence is useful for encrypting credit card numbers, passwords etc.  For larger blocks of data, this
   * algorithm may expose patterns and be vulnerable to repeat attacks.
   *
   * The transformation algorithm can be configured by defining `play.crypto.aes.transformation` in
   * `application.conf`.  Although any cipher transformation algorithm can be selected here, the secret key spec used
   * is always AES, so only AES transformation algorithms will work.
   *
   * @param value The String to encrypt.
   * @return An hexadecimal encrypted string.
   */
  def encryptAES(value: String): String = {
    encryptAES(value, config.secret)
  }

  /**
   * Encrypt a String with the AES encryption standard and the supplied private key.
   *
   *
   * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
   * `play.crypto.provider` in `application.conf`.
   *
   * The transformation algorithm used is the provider specific implementation of the `AES` name.  On Oracles JDK,
   * this is `AES/CTR/NoPadding`.  This algorithm is suitable for small amounts of data, typically less than 32
   * bytes, hence is useful for encrypting credit card numbers, passwords etc.  For larger blocks of data, this
   * algorithm may expose patterns and be vulnerable to repeat attacks.
   *
   * The transformation algorithm can be configured by defining `play.crypto.aes.transformation` in
   * `application.conf`.  Although any cipher transformation algorithm can be selected here, the secret key spec used
   * is always AES, so only AES transformation algorithms will work.
   *
   * @param value The String to encrypt.
   * @param privateKey The key used to encrypt.
   * @return A Base64 encrypted string.
   */
  def encryptAES(value: String, privateKey: String): String = {
    val skeySpec = secretKeyWithSha256(privateKey, "AES")
    val cipher = getCipherWithConfiguredProvider(config.aesTransformation)
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
    val encryptedValue = cipher.doFinal(value.getBytes("utf-8"))
    // return a formatted, versioned encrypted string
    // '2-*' represents an encrypted payload with an IV
    // '1-*' represents an encrypted payload without an IV
    Option(cipher.getIV()) match {
      case Some(iv) => s"2-${Base64.encodeBase64String(iv ++ encryptedValue)}"
      case None => s"1-${Base64.encodeBase64String(encryptedValue)}"
    }
  }

  /**
   * Generates the SecretKeySpec, given the private key and the algorithm.
   */
  private def secretKeyWithSha256(privateKey: String, algorithm: String) = {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    messageDigest.update(privateKey.getBytes("utf-8"))
    // max allowed length in bits / (8 bits to a byte)
    val maxAllowedKeyLength = Cipher.getMaxAllowedKeyLength(algorithm) / 8
    val raw = messageDigest.digest().slice(0, maxAllowedKeyLength)
    new SecretKeySpec(raw, algorithm)
  }

  /**
   * Gets a Cipher with a configured provider, and a configurable AES transformation method.
   */
  private def getCipherWithConfiguredProvider(transformation: String): Cipher = {
    config.provider.fold(Cipher.getInstance(transformation)) { p =>
      Cipher.getInstance(transformation, p)
    }
  }

  /**
   * Decrypt a String with the AES encryption standard using the application's secret key.
   *
   * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
   * `play.crypto.provider` in `application.conf`.
   *
   * The transformation used is by default `AES/CTR/NoPadding`.  It can be configured by defining
   * `play.crypto.aes.transformation` in `application.conf`.  Although any cipher transformation algorithm can
   * be selected here, the secret key spec used is always AES, so only AES transformation algorithms will work.
   *
   * @param value An hexadecimal encrypted string.
   * @return The decrypted String.
   */
  def decryptAES(value: String): String = {
    decryptAES(value, config.secret)
  }

  /**
   * Decrypt a String with the AES encryption standard.
   *
   * The private key must have a length of 16 bytes.
   *
   * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
   * `play.crypto.provider` in `application.conf`.
   *
   * The transformation used is by default `AES/CTR/NoPadding`.  It can be configured by defining
   * `play.crypto.aes.transformation` in `application.conf`.  Although any cipher transformation algorithm can
   * be selected here, the secret key spec used is always AES, so only AES transformation algorithms will work.
   *
   * @param value An hexadecimal encrypted string.
   * @param privateKey The key used to encrypt.
   * @return The decrypted String.
   */
  def decryptAES(value: String, privateKey: String): String = {
    val seperator = "-"
    val sepIndex = value.indexOf(seperator)
    if (sepIndex < 0) {
      decryptAESVersion0(value, privateKey)
    } else {
      val version = value.substring(0, sepIndex)
      val data = value.substring(sepIndex + 1, value.length())
      version match {
        case "1" => {
          decryptAESVersion1(data, privateKey)
        }
        case "2" => {
          decryptAESVersion2(data, privateKey)
        }
        case _ => {
          throw new CryptoException("Unknown version")
        }
      }
    }
  }

  /** Backward compatible AES ECB mode decryption support. */
  private def decryptAESVersion0(value: String, privateKey: String): String = {
    val raw = privateKey.substring(0, 16).getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = getCipherWithConfiguredProvider("AES")
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    new String(cipher.doFinal(Codecs.hexStringToByte(value)))
  }

  /** V1 decryption algorithm (No IV). */
  private def decryptAESVersion1(value: String, privateKey: String): String = {
    val data = Base64.decodeBase64(value)
    val skeySpec = secretKeyWithSha256(privateKey, "AES")
    val cipher = getCipherWithConfiguredProvider(config.aesTransformation)
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    new String(cipher.doFinal(data), "utf-8")
  }

  /** V2 decryption algorithm (IV present). */
  private def decryptAESVersion2(value: String, privateKey: String): String = {
    val data = Base64.decodeBase64(value)
    val skeySpec = secretKeyWithSha256(privateKey, "AES")
    val cipher = getCipherWithConfiguredProvider(config.aesTransformation)
    val blockSize = cipher.getBlockSize
    val iv = data.slice(0, blockSize)
    val payload = data.slice(blockSize, data.size)
    cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(iv))
    new String(cipher.doFinal(payload), "utf-8")
  }
}
