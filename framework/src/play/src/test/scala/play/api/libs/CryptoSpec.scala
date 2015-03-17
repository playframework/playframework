/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import org.specs2.mutable._
import play.api._

object CryptoSpec extends Specification {

  "Crypto api" should {
    "be able to encrypt/decrypt text using AES algorithm" in {
      val text = "Play Framework 2.0"
      val key = "0123456789abcdef"
      val cryptoConfig = CryptoConfig(key, None, "AES")
      val crypto = new Crypto(cryptoConfig)
      crypto.decryptAES(crypto.encryptAES(text, key), key) must be equalTo text
    }
  }

  "Crypto api" should {
    "be able to encrypt/decrypt text using other AES transformations" in {
      val text = "Play Framework 2.0"
      val key = "0123456789abcdef"
      val cryptoConfig = CryptoConfig(key, None, "AES/CTR/NoPadding")
      val crypto = new Crypto(cryptoConfig)
      crypto.decryptAES(crypto.encryptAES(text, key), key) must be equalTo text
    }
  }

  "Crypto api" should {
    "be able to decrypt text generated using the old transformation methods" in {
      val text = "Play Framework 2.0"
      val key = "0123456789abcdef"
      // old way to encrypt things
      val cipher = Cipher.getInstance("AES")
      val skeySpec = new SecretKeySpec(key.substring(0, 16).getBytes("utf-8"), "AES")
      cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
      val encrypted = Codecs.toHexString(cipher.doFinal(text.getBytes("utf-8")))
      val cryptoConfig = CryptoConfig(key, None, "AES/CTR/NoPadding")
      val crypto = new Crypto(cryptoConfig)
      // should be decryptable
      crypto.decryptAES(encrypted) must be equalTo text
    }
  }

  "Crypto config parser" should {
    "parse the secret" in {
      val Secret = "abcdefghijklmnopqrs"

      def parseSecret(mode: Mode.Mode, secret: Option[String] = None) = {
        new CryptoConfigParser(Environment.simple(mode = mode),
          Configuration.reference ++ Configuration.from(
            secret.map("play.crypto.secret" -> _).toMap +
              ("play.crypto.aes.transformation" -> "AES")
          )).get.secret
      }

      "load a configured secret in prod" in {
        parseSecret(Mode.Prod, Some(Secret)) must_== Secret
      }
      "load a configured secret in dev" in {
        parseSecret(Mode.Dev, Some(Secret)) must_== Secret
      }
      "throw an exception if secret is changeme in prod" in {
        parseSecret(Mode.Prod, Some("changeme")) must throwA[PlayException]
      }
      "throw an exception if no secret in prod" in {
        parseSecret(Mode.Prod) must throwA[PlayException]
      }
      "throw an exception if secret is blank in prod" in {
        parseSecret(Mode.Prod, Some("  ")) must throwA[PlayException]
      }
      "throw an exception if secret is empty in prod" in {
        parseSecret(Mode.Prod, Some("")) must throwA[PlayException]
      }
      "generate a secret if secret is changeme in dev" in {
        parseSecret(Mode.Dev, Some("changeme")) must_!= "changeme"
      }
      "generate a secret if no secret in dev" in {
        parseSecret(Mode.Dev) must_!= ""
      }
      "generate a secret if secret is blank in dev" in {
        parseSecret(Mode.Dev, Some("  ")) must_!= "  "
      }
      "generate a secret if secret is empty in dev" in {
        parseSecret(Mode.Dev, Some("")) must_!= ""
      }
      "generate a stable secret in dev" in {
        parseSecret(Mode.Dev, Some("changeme")) must_== parseSecret(Mode.Dev)
      }
    }
  }

}

