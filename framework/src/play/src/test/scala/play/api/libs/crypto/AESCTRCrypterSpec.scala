/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.crypto

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import org.specs2.mutable.Specification
import play.api.libs.Codecs

class AESCTRCrypterSpec extends Specification {

  val key = "0123456789abcdef"
  val cryptoConfig = CryptoConfig(key, None, "AES")

  "Crypto api using symmetrical encryption" should {

    val crypter = new AESCTRCrypter(cryptoConfig)

    "be able to encrypt/decrypt text using AES algorithm" in {
      val text = "Play Framework 2.0"
      crypter.decryptAES(crypter.encryptAES(text, key), key) must be equalTo text
    }

    "be able to encrypt/decrypt text using other AES transformations" in {
      val text = "Play Framework 2.0"
      crypter.decryptAES(crypter.encryptAES(text, key), key) must be equalTo text
    }

    "be able to decrypt text generated using the old transformation methods" in {
      val text = "Play Framework 2.0"
      val key = "0123456789abcdef"
      // old way to encrypt things
      val cipher = Cipher.getInstance("AES")
      val skeySpec = new SecretKeySpec(key.substring(0, 16).getBytes("utf-8"), "AES")
      cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
      val encrypted = Codecs.toHexString(cipher.doFinal(text.getBytes("utf-8")))
      val cryptoConfig = CryptoConfig(key, None, "AES/CTR/NoPadding")
      // should be decryptable
      crypter.decryptAES(encrypted) must be equalTo text
    }
  }

}
