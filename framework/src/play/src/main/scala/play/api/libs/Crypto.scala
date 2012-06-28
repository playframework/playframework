package play.api.libs

import java.security._
import javax.crypto._
import javax.crypto.spec.SecretKeySpec

import play.api.Play
import play.api.PlayException

/**
 * Cryptographic utilities.
 */
object Crypto {

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
    Play.maybeApplication.flatMap(_.configuration.getString("application.secret")).map(secret => sign(message, secret.getBytes)).getOrElse {
      throw PlayException("Configuration error", "Missing application.secret")
    }
  }

  /**
  * Encrypt a String with the AES encryption standard using the application secret
  * @param value The String to encrypt
  * @return An hexadecimal encrypted string
  */
  def encryptAES(value: String) : String = {
    Play.maybeApplication.flatMap(_.configuration.getString("application.secret")).map(secret => encryptAES(value, secret.substring(0, 16).getBytes)).getOrElse {
      throw PlayException("Configuration error", "Missing application.secret")
    }
  }
  
  /**
  * Encrypt a String with the AES encryption standard. Key must have a length of 16 bytes
  * @param value The String to encrypt
  * @param key The key used to encrypt
  * @return An hexadecimal encrypted string
  */
  def encryptAES(value : String, key : Array[Byte]) : String = {
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"))
    Codecs.toHexString(cipher.doFinal(value.getBytes("utf-8")))
  }

  /**
  * Decrypt a String with the AES encryption standard using the application secret
  * @param value An hexadecimal encrypted string
  * @return The decrypted String
  */
  def decryptAES(value: String) : String  = {
    Play.maybeApplication.flatMap(_.configuration.getString("application.secret")).map(secret => decryptAES(value, secret.substring(0, 16).getBytes)).getOrElse {
      throw PlayException("Configuration error", "Missing application.secret")
    }
  }
  
  /**
  * Decrypt a String with the AES encryption standard. Key must have a length of 16 bytes
  * @param value An hexadecimal encrypted string
  * @param key The key used to encrypt
  * @return The decrypted String
  */
    def decryptAES(value : String, key : Array[Byte]) : String = {
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"))
    new String(cipher.doFinal(Codecs.fromHexString(value)),"utf-8")
  }
  

}
