package play.api.libs

import java.security._
import javax.crypto._
import javax.crypto.spec.SecretKeySpec

import play.api.Play
import play.api.PlayException
import play.api.Configuration.Config

/** Cryptographic utilities. */
object Crypto {

  /** Signs a string using the given key. */
  def sign(message: String, key: Array[Byte]): String = {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(new SecretKeySpec(key, "HmacSHA1"))
    Codecs.toHexString(mac.doFinal(message.getBytes("utf-8")))
  }

  /** Signs the given string using the application’s secret key. */
  def sign(message: String): String = {
    Play.maybeApplication.map(_.configuration.get("application.secret") match {
      case Some(Config(key, value, file)) => sign(message, value.getBytes)
      case _ => throw PlayException("Configuration error", "Missing application.secret")
    }).getOrElse(throw PlayException("Configuration error", "Application does not exist"))
  }
}
