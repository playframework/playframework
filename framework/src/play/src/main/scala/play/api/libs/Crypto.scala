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

}
