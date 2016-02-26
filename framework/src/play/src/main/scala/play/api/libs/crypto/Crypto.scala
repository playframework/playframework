/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.crypto

import java.security.{ MessageDigest, SecureRandom }
import java.time.Clock
import javax.crypto.spec.{ IvParameterSpec, SecretKeySpec }
import javax.crypto.{ Cipher, Mac }
import javax.inject.{ Inject, Provider, Singleton }

import org.apache.commons.codec.binary.{ Base64, Hex }
import org.apache.commons.codec.digest.DigestUtils
import play.api._
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
   * By default this uses the platform default JCE provider.  This can be overridden by defining
   * `play.crypto.provider` in `application.conf`.
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
   * `play.crypto.provider` in `application.conf`.
   *
   * @param message The message to sign.
   * @return A hexadecimal encoded signature.
   */
  def sign(message: String): String
}

/**
 * Cryptographic utilities for generating and validating CSRF tokens.
 *
 * This trait should not be used as a general purpose encryption utility.
 */
trait CSRFTokenSigner {

  /**
   * Sign a token.  This produces a new token, that has this token signed with a nonce.
   *
   * This primarily exists to defeat the BREACH vulnerability, as it allows the token to effectively be random per
   * request, without actually changing the value.
   *
   * @param token The token to sign
   * @return The signed token
   */
  def signToken(token: String): String

  /**
   * Generates a cryptographically secure token.
   */
  def generateToken: String

  /**
   * Generates a signed token.
   */
  def generateSignedToken: String

  /**
   * Extract a signed token that was signed by [[play.api.libs.Crypto.signToken]].
   *
   * @param token The signed token to extract.
   * @return The verified raw token, or None if the token isn't valid.
   */
  def extractSignedToken(token: String): Option[String]

  /**
   * Compare two signed tokens
   */
  def compareSignedTokens(tokenA: String, tokenB: String): Boolean

  /**
   * Constant time equals method.
   *
   * Given a length that both Strings are equal to, this method will always run in constant time.  This prevents
   * timing attacks.
   */
  def constantTimeEquals(a: String, b: String): Boolean
}

/**
 * Encrypts and decrypts strings using AES.
 *
 * Note that the output from AES/CTR is vulnerable to malleability attacks unless signed with a MAC.
 *
 * In addition, because this class is hardcoded to use AES, it cannot be used a general purpose Crypter.
 *
 * It is also not used anywhere internally to the Play code base.
 */
@deprecated(message = "This method is deprecated and will be removed in future versions.", since = "2.5.0")
trait AESCrypter {

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
   * @deprecated This method is deprecated and will be removed in future versions.
   * @param value The String to encrypt.
   * @return An hexadecimal encrypted string.
   */
  @deprecated(message = "This method is deprecated and will be removed in future versions.", since = "2.5.0")
  def encryptAES(value: String): String

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
   * @deprecated This method is deprecated and will be removed in future versions.
   * @param value The String to encrypt.
   * @param privateKey The key used to encrypt.
   * @return An hexadecimal encrypted string.
   */
  @deprecated(message = "This method is deprecated and will be removed in future versions.", since = "2.5.0")
  def encryptAES(value: String, privateKey: String): String

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
   * @deprecated This method is deprecated and will be removed in future versions.
   * @param value An hexadecimal encrypted string.
   * @return The decrypted String.
   */
  @deprecated(message = "This method is deprecated and will be removed in future versions.", since = "2.5.0")
  def decryptAES(value: String): String

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
   * @deprecated This method is deprecated and will be removed in future versions.
   * @param value An hexadecimal encrypted string.
   * @param privateKey The key used to encrypt.
   * @return The decrypted String.
   */
  @deprecated(message = "This method is deprecated and will be removed in future versions.", since = "2.5.0")
  def decryptAES(value: String, privateKey: String): String

}

/**
 * Exception thrown by the Crypto APIs.
 *
 * @param message The error message.
 * @param throwable The Throwable associated with the exception.
 */
class CryptoException(val message: String = null, val throwable: Throwable = null) extends RuntimeException(message, throwable)

@Singleton
class CookieSignerProvider @Inject() (config: CryptoConfig) extends Provider[CookieSigner] {
  lazy val get: CookieSigner = new HMACSHA1CookieSigner(config)
}

/**
 * Uses an HMAC-SHA1 for signing cookies.
 */
class HMACSHA1CookieSigner @Inject() (config: CryptoConfig) extends CookieSigner {

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

}

@Singleton
class CSRFTokenSignerProvider @Inject() (signer: CookieSigner) extends Provider[CSRFTokenSigner] {
  lazy val get: CSRFTokenSigner = new DefaultCSRFTokenSigner(signer, Clock.systemUTC())
}

/**
 * This class is used for generating random tokens for CSRF.
 */
class DefaultCSRFTokenSigner @Inject() (signer: CookieSigner, clock: Clock) extends CSRFTokenSigner {

  // If you're running on an older version of Windows, you may be using
  // SHA1PRNG.  So immediately calling nextBytes with a seed length
  // of 440 bits (NIST SP800-90A) will do a more than decent
  // self-seeding for a SHA-1 based PRNG.
  private val random = new SecureRandom()
  random.nextBytes(new Array[Byte](55))

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
    val nonce = clock.millis()
    val joined = nonce + "-" + token
    signer.sign(joined) + "-" + joined
  }

  /**
   * Extract a signed token that was signed by [[CSRFTokenSigner.signToken]].
   *
   * @param token The signed token to extract.
   * @return The verified raw token, or None if the token isn't valid.
   */
  def extractSignedToken(token: String): Option[String] = {
    token.split("-", 3) match {
      case Array(signature, nonce, raw) if constantTimeEquals(signature, signer.sign(nonce + "-" + raw)) => Some(raw)
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
    } yield CSRFTokenSigner.constantTimeEquals(rawA, rawB)).getOrElse(false)
  }

  /**
   * Constant time equals method.
   *
   * Given a length that both Strings are equal to, this method will always run in constant time.  This prevents
   * timing attacks.
   */
  override def constantTimeEquals(a: String, b: String): Boolean = CSRFTokenSigner.constantTimeEquals(a, b)
}

object CSRFTokenSigner {

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
}

@Singleton
class AESCrypterProvider @Inject() (config: CryptoConfig) extends Provider[AESCrypter] {
  lazy val get = new AESCTRCrypter(config)
}

/**
 * Symmetric encryption using AES/CTR/NoPadding.
 */
class AESCTRCrypter @Inject() (config: CryptoConfig) extends AESCrypter {

  def encryptAES(value: String): String = {
    encryptAES(value, config.secret)
  }

  @deprecated("This method will be removed in future versions ", "2.5.0")
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

  @deprecated("This method will be removed in future versions ", "2.5.0")
  def decryptAES(value: String): String = {
    decryptAES(value, config.secret)
  }

  @deprecated("This method will be removed in future versions ", "2.5.0")
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

/**
 * Configuration for Crypto
 *
 * @param secret The application secret
 * @param aesTransformation The AES transformation to use
 * @param provider The crypto provider to use
 */
case class CryptoConfig(secret: String,
  provider: Option[String] = None,
  @deprecated("This field is deprecated and will be removed in future versions", "2.5.0") aesTransformation: String = "AES/CTR/NoPadding")

@Singleton
class CryptoConfigParser @Inject() (environment: Environment, configuration: Configuration) extends Provider[CryptoConfig] {

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
    val secret = config.getDeprecated[Option[String]]("play.crypto.secret", "application.secret") match {
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

    val provider = config.get[Option[String]]("play.crypto.provider")
    val transformation = config.get[String]("play.crypto.aes.transformation")

    CryptoConfig(secret, provider, transformation)
  }
  private val Blank = """\s*""".r
  private val logger = Logger(classOf[CryptoConfigParser])
}
