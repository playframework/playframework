/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.crypto

import org.specs2.mutable.Specification
import play.api.http.SecretConfiguration

class CookieSignerSpec extends Specification {

  "signer.sign" should {

    "be able to sign input using Blake2b using the config secret" in {
      val text = "Play Framework 2.0"
      val key = "0123456789abcdef"
      val secretConfiguration = SecretConfiguration(key)
      val signer = new DefaultCookieSigner(secretConfiguration)
      signer.sign(text) must be_==("7328db687e45025bacb3e137edea4f6152dda6081964b267f9d92042bbaacf5ebb0103cb74446597a1a9735d5d81999ad5a43141f9e2621ad3def7180ebc055d")
    }

    "be able to sign input using Blake2b using an explicitly passed in key" in {
      val text = "Play Framework 2.0"
      val key = "different key"
      val secretConfiguration = SecretConfiguration(key)
      val signer = new DefaultCookieSigner(secretConfiguration)
      signer.sign(text, key.getBytes("UTF-8")) must be_==("279857b7d26b4ca9d942aa24efa4b760a9abd3b35129d1794c22bb56c01642259b55bff0d782786285052ea47f80be388f6224e020746c9a2fc7f07f63f79d54")
    }

    "be able to sign input using Blake2b using an explicitly passed in key (same as secret)" in {
      val text = "Play Framework 2.0"
      val key = "0123456789abcdef"
      val secretConfiguration = SecretConfiguration(key)
      val signer = new DefaultCookieSigner(secretConfiguration)
      signer.sign(text, key.getBytes("UTF-8")) must be_==("7328db687e45025bacb3e137edea4f6152dda6081964b267f9d92042bbaacf5ebb0103cb74446597a1a9735d5d81999ad5a43141f9e2621ad3def7180ebc055d")
    }

    "be able to sign input using HMAC-SHA1 using the config secret" in {
      val text = "Play Framework 2.0"
      val key = "0123456789abcdef"
      val secretConfiguration = SecretConfiguration(key, mac = Some("HmacSHA1"))
      val signer = new DefaultCookieSigner(secretConfiguration)
      signer.sign(text) must be_==("94f63b1470ee74e15dc15fd704e26b0df36ef848")
    }

    "be able to sign input using HMAC-SHA1 using an explicitly passed in key" in {
      val text = "Play Framework 2.0"
      val key = "different key"
      val secretConfiguration = SecretConfiguration(key, mac = Some("HmacSHA1"))
      val signer = new DefaultCookieSigner(secretConfiguration)
      signer.sign(text, key.getBytes("UTF-8")) must be_==("470037631bddcbd13bb85d80d531c97a340f836f")
    }

    "be able to sign input using HMAC-SHA1 using an explicitly passed in key (same as secret)" in {
      val text = "Play Framework 2.0"
      val key = "0123456789abcdef"
      val secretConfiguration = SecretConfiguration(key, mac = Some("HmacSHA1"))
      val signer = new DefaultCookieSigner(secretConfiguration)
      signer.sign(text, key.getBytes("UTF-8")) must be_==("94f63b1470ee74e15dc15fd704e26b0df36ef848")
    }

  }

}
