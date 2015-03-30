/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._
import java.security.cert.{ CertPathValidatorException, Certificate }
import java.util.Collections._
import AlgorithmConstraintsParser._

object AlgorithmCheckerSpec extends Specification {

  "AlgorithmChecker" should {

    def checker(sigs: Seq[String], keys: Seq[String]) = {
      new AlgorithmChecker(sigs.map(s => parseAll(expression, s).get).toSet,
        keys.map(s => parseAll(expression, s).get).toSet)
    }

    "pass a good key algorithm (RSA > 1024)" in {
      val certificate: Certificate = CertificateGenerator.generateRSAWithSHA256(2048)
      checker(Nil, Seq("RSA keySize < 1024")).check(certificate, emptySet())
      success
    }

    "fail a weak key algorithm (RSA < 512)" in {
      val certificate: Certificate = CertificateGenerator.generateRSAWithSHA256(512)
      checker(Nil, Seq("RSA keySize < 1024")).check(certificate, emptySet()).must(throwA[CertPathValidatorException])
    }

    "pass a good signature algorithm (SHA256)" in {
      val certificate: Certificate = CertificateGenerator.generateRSAWithSHA256(512)
      checker(Seq("MD5"), Nil).check(certificate, emptySet())
      success
    }

    "fail a bad signature algorithm (MD5)" in {
      val intermediateCert: Certificate = CertificateGenerator.generateRSAWithMD5(2048)
      checker(Seq("MD5"), Nil).check(intermediateCert, emptySet()).must(throwA[CertPathValidatorException])
    }
  }
}
