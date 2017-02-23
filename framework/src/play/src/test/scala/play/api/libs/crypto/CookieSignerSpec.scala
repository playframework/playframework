/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.crypto

import org.specs2.mutable.Specification

class CookieSignerSpec extends Specification {

  "signer.sign" should {

    "be able to sign input using HMAC-SHA1 using the config secret" in {
      val text = "Play Framework 2.0"
      val key = "0123456789abcdef"
      val cryptoConfig = CryptoConfig(key, None, "AES")
      val signer = new HMACSHA1CookieSigner(cryptoConfig)
      signer.sign(text) must be_==("94f63b1470ee74e15dc15fd704e26b0df36ef848")
    }

    "be able to sign input using HMAC-SHA1 using an explicitly passed in key" in {
      val text = "Play Framework 2.0"
      val key = "different key"
      val cryptoConfig = CryptoConfig(key, None, "AES")
      val signer = new HMACSHA1CookieSigner(cryptoConfig)
      signer.sign(text, key.getBytes("UTF-8")) must be_==("470037631bddcbd13bb85d80d531c97a340f836f")
    }

    "be able to sign input using HMAC-SHA1 using an explicitly passed in key (same as secret)" in {
      val text = "Play Framework 2.0"
      val key = "0123456789abcdef"
      val cryptoConfig = CryptoConfig(key, None, "AES")
      val signer = new HMACSHA1CookieSigner(cryptoConfig)
      signer.sign(text, key.getBytes("UTF-8")) must be_==("94f63b1470ee74e15dc15fd704e26b0df36ef848")
    }

  }

}
