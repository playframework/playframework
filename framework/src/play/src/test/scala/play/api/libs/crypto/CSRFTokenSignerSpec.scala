/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.crypto

import java.time.{ Clock, Instant, ZoneId }

import org.specs2.mutable._
import play.api.http.SecretConfiguration

class CSRFTokenSignerSpec extends Specification {

  val key = "0123456789abcdef"
  val secretConfiguration = SecretConfiguration(key, None)
  val clock = Clock.fixed(Instant.ofEpochMilli(0L), ZoneId.systemDefault)
  val signer = new DefaultCookieSigner(secretConfiguration)
  val tokenSigner = new DefaultCSRFTokenSigner(signer, clock)

  "tokenSigner.generateToken" should {
    "be successful" in {
      val token = tokenSigner.generateToken
      token.length must beEqualTo(24)
    }
  }

  "tokenSigner.signToken" should {
    "be successful" in {
      val token: String = "0FFFFFFFFFFFFFFFFFFFFF24"
      token.length must be_==(24)
      val signedToken = tokenSigner.signToken(token)
      signedToken must beEqualTo("77adb3c3dfe5ee567556b259549a4ddfa6797c05-0-0FFFFFFFFFFFFFFFFFFFFF24")
    }
  }

  "tokenSigner.compareSignedTokens" should {
    "be successful" in {
      val token1: String = "b3ba23c672b5e115b0c44335544dbf42934f70f5-1445022964749-0FFFFFFFFFFFFFFFFFFFFF24"
      val token2: String = "b3ba23c672b5e115b0c44335544dbf42934f70f5-1445022964749-0FFFFFFFFFFFFFFFFFFFFF24"
      val actual = tokenSigner.compareSignedTokens(token1, token2)
      actual must beTrue
    }
  }

}

