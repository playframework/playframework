package play.api.libs

import org.specs2.mutable._
import play.api.libs._

object CryptoSpec extends Specification {

  "Crypto api" should {
    "be able to encrypt/decrypt text using AES algorithm" in {
      val text = "Play Framework 2.0"
      val key  = "0123456789abcdef" 
      Crypto.decryptAES(Crypto.encryptAES(text, key), key) must be equalTo text
    }
  }

}

