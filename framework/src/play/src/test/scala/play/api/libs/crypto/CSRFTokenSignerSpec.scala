/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.crypto

import java.time.{ Clock, Instant, ZoneId }

import org.specs2.mutable._
import play.api.http.SecretConfiguration

class CSRFTokenSignerSpec extends Specification {

  val key = "0123456789abcdef"
  val secretConfiguration = SecretConfiguration(key)
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
      signedToken must beEqualTo("169b4bd49930f3a91232fe14e38952bfe3840022c387b12e25aad7e8603d2ba356a99732a2151c25178d3bcf68fb99acd63f2e47717bbf2ee564e1f8cda3fc82-0-0FFFFFFFFFFFFFFFFFFFFF24")
    }
  }

  "tokenSigner.compareSignedTokens" should {
    "be successful" in {
      val token1: String = "cddd64086f143c0c0d4f5a934533efe01744a183b2a30cc6f26bac1635023674f935e292838383a0b0ccc777e3874ba910756c67f6fc48654a169226b370bf26-1445022964749-0FFFFFFFFFFFFFFFFFFFFF24"
      val token2: String = "cddd64086f143c0c0d4f5a934533efe01744a183b2a30cc6f26bac1635023674f935e292838383a0b0ccc777e3874ba910756c67f6fc48654a169226b370bf26-1445022964749-0FFFFFFFFFFFFFFFFFFFFF24"
      val actual = tokenSigner.compareSignedTokens(token1, token2)
      actual must beTrue
    }
  }

}

