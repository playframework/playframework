/*
 *
 *  * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 *
 */
package play.api.libs.ws.ssl

import java.security.cert.{ CertPathValidatorException, Certificate, X509Certificate }
import java.util.Collections._

import org.joda.time.{ DateTime, Days, Instant }
import org.specs2.mutable._
import play.api.libs.ws.ssl.AlgorithmConstraintsParser._
import play.core.server.ssl.CertificateGenerator

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

    "neither info nor warning on a signature containing sha-1 that expires before 1 June 2016" in {
      val oneHundredAndEightyDays = Days.days(180).toStandardDuration
      val certificate = CertificateGenerator.generateRSAWithSHA1(2048, from = Instant.parse("2015-06-01T12:00:00Z"), duration = oneHundredAndEightyDays)

      var infoCalled = false
      var warningCalled = false
      val checker = new AlgorithmChecker(Set.empty, Set.empty) {
        override def infoOnSunset(x509Cert: X509Certificate, expirationDate: DateTime): Unit = {
          infoCalled = true
        }
        override def warnOnSunset(x509Cert: X509Certificate, expirationDate: DateTime): Unit = {
          warningCalled = true
        }
      }

      checker.check(certificate, emptySet())
      infoCalled must beFalse
      warningCalled must beFalse
    }

    "info on a signature containing sha-1 that expires between 1 June 2016 to 31 December 2016" in {
      val thirtyDays = Days.days(30).toStandardDuration
      val certificate = CertificateGenerator.generateRSAWithSHA1(2048, from = Instant.parse("2016-06-01T12:00:00Z"), duration = thirtyDays)

      var infoCalled = false
      val checker = new AlgorithmChecker(Set.empty, Set.empty) {
        override def infoOnSunset(x509Cert: X509Certificate, expirationDate: DateTime): Unit = {
          infoCalled = true
        }
      }

      checker.check(certificate, emptySet())
      infoCalled must beTrue
    }

    "warn on a signature containing sha-1 that expires after 2017" in {
      val tenYears = Days.days(365 * 10).toStandardDuration
      val certificate = CertificateGenerator.generateRSAWithSHA1(2048, from = Instant.parse("2016-06-01T12:00:00Z"), duration = tenYears)

      var warningCalled = false
      val checker = new AlgorithmChecker(Set.empty, Set.empty) {
        override def warnOnSunset(x509Cert: X509Certificate, expirationDate: DateTime): Unit = {
          warningCalled = true
        }
      }

      checker.check(certificate, emptySet())
      warningCalled must beTrue
    }
  }
}
