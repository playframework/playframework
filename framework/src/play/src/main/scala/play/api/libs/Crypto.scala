package play.api.libs

import javax.crypto._
import javax.crypto.spec.SecretKeySpec

import play.api.Play
import play.api.PlayException

/**
 * Cryptographic utilities.
 */
object Crypto {

  private def secret: Option[String] = Play.maybeApplication.flatMap(_.configuration.getString("application.secret"))

  /**
   * Signs the given String with HMAC-SHA1 using the given key.
   */
  def sign(message: String, key: Array[Byte]): String = {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(new SecretKeySpec(key, "HmacSHA1"))
    Codecs.toHexString(mac.doFinal(message.getBytes("utf-8")))
  }

  /**
   * Signs the given String with HMAC-SHA1 using the application’s secret key.
   */
  def sign(message: String): String = {
    secret.map(secret => sign(message, secret.getBytes("utf-8"))).getOrElse {
      throw new PlayException("Configuration error", "Missing application.secret")
    }
  }

  /**
   * Encrypt a String with the AES encryption standard using the application secret
   * @param value The String to encrypt
   * @return An hexadecimal encrypted string
   */
  def encryptAES(value: String): String = {
    secret.map(secret => encryptAES(value, secret.substring(0, 16))).getOrElse {
      throw new PlayException("Configuration error", "Missing application.secret")
    }
  }

  /**
   * Encrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
   * @param value The String to encrypt
   * @param privateKey The key used to encrypt
   * @return An hexadecimal encrypted string
   */
  def encryptAES(value: String, privateKey: String): String = {
    val raw = privateKey.getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
    Codecs.toHexString(cipher.doFinal(value.getBytes("utf-8")))
  }

  /**
   * Decrypt a String with the AES encryption standard using the application secret
   * @param value An hexadecimal encrypted string
   * @return The decrypted String
   */
  def decryptAES(value: String): String = {
    secret.map(secret => decryptAES(value, secret.substring(0, 16))).getOrElse {
      throw new PlayException("Configuration error", "Missing application.secret")
    }
  }

  /**
   * Decrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
   * @param value An hexadecimal encrypted string
   * @param privateKey The key used to encrypt
   * @return The decrypted String
   */
  def decryptAES(value: String, privateKey: String): String = {
    val raw = privateKey.getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    new String(cipher.doFinal(Codecs.hexStringToByte(value)))
  }

}
