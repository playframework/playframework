/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._
import java.security.cert.{CertPathValidatorException, Certificate}
import java.util.Collections._
import AlgorithmConstraintsParser._

object AlgorithmCheckerSpec extends Specification {

  "AlgorithmChecker" should {

    "pass a good algorithm (SHA 256)" in {
      val disabledAlgorithms = parseAll(line, "RSA keySize < 1024").get.toSet
      val checker = new AlgorithmChecker(disabledAlgorithms)
      val certificate:Certificate = CertificateGenerator.generateRSAWithSHA256(2048)

      checker.check(certificate, emptySet())

      success
    }

    "fail a weak algorithm (RSA with 1024 keysize)" in {
      val disabledAlgorithms = parseAll(line, "RSA keySize < 1024").get.toSet
      val checker = new AlgorithmChecker(disabledAlgorithms)

      val certificate:Certificate = CertificateGenerator.generateRSAWithSHA256(512)

      checker.check(certificate, emptySet()).must(throwA[CertPathValidatorException])
    }

    "fail a bad algorithm (MD5)" in {
      val disabledAlgorithms = parseAll(line, "MD5").get.toSet
      val checker = new AlgorithmChecker(disabledAlgorithms)

      val certificate:Certificate = CertificateGenerator.generateRSAWithMD5(2048)

      checker.check(certificate, emptySet()).must(throwA[CertPathValidatorException])
    }

  }

}
