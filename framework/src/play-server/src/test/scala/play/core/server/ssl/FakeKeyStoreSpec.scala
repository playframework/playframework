/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.ssl

import org.specs2.mutable.Specification

class FakeKeyStoreSpec extends Specification {

  "FakeKeyStore construction" should {
    "return true on MD5 cert" in {
      val weakCert = CertificateGenerator.generateRSAWithMD5()
      val actual = FakeKeyStore.certificateTooWeak(weakCert)
      actual must beTrue
    }

    "return false on SHA256withRSA" in {
      val strongCert = CertificateGenerator.generateRSAWithSHA256()
      val actual = FakeKeyStore.certificateTooWeak(strongCert)
      actual must beFalse
    }
  }
}
