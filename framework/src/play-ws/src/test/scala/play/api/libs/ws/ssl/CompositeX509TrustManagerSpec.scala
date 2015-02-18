/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._
import org.specs2.mock.Mockito

import javax.net.ssl.X509TrustManager
import java.security.cert.{ CertificateException, X509Certificate }

object CompositeX509TrustManagerSpec extends Specification with Mockito {

  "CompositeX509TrustManager" should {

    "with checkClientTrusted" should {

      "throws exception" in {
        val mockTrustManager1 = mock[X509TrustManager]
        val mockTrustManager2 = mock[X509TrustManager]
        val algorithmChecker = new AlgorithmChecker(Set(), Set())
        val trustManager = new CompositeX509TrustManager(trustManagers = Seq(mockTrustManager1, mockTrustManager2), algorithmChecker)

        val certificate = CertificateGenerator.generateRSAWithSHA256()
        val chain = Array[X509Certificate](certificate)
        val authType = ""

        mockTrustManager1.checkClientTrusted(chain, authType) throws new CertificateException("fake1")
        mockTrustManager2.checkClientTrusted(chain, authType) throws new CertificateException("fake2")

        trustManager.checkClientTrusted(chain, authType).must(throwA[CompositeCertificateException].like {
          case e: CompositeCertificateException =>
            val sourceExceptions = e.getSourceExceptions
            sourceExceptions(0).getMessage must be_==("fake1")
            sourceExceptions(1).getMessage must be_==("fake2")
        })
      }

      "returns true" in {
        val mockTrustManager = mock[X509TrustManager]
        val algorithmChecker = new AlgorithmChecker(Set(), Set())
        val trustManager = new CompositeX509TrustManager(trustManagers = Seq(mockTrustManager), algorithmChecker)

        val certificate = CertificateGenerator.generateRSAWithSHA256()
        val chain = Array[X509Certificate](certificate)
        val authType = ""

        trustManager.checkClientTrusted(chain, authType) must not(throwA[Throwable].like {
          case e: CompositeCertificateException =>
            val sourceExceptions = e.getSourceExceptions
            sourceExceptions(0).getMessage must be_==("fake")
        })
      }

      "returns true eventually" in {
        val mockTrustManager1 = mock[X509TrustManager]
        val mockTrustManager2 = mock[X509TrustManager]
        val algorithmChecker = new AlgorithmChecker(Set(), Set())
        val trustManager = new CompositeX509TrustManager(trustManagers = Seq(mockTrustManager1, mockTrustManager2), algorithmChecker)

        val certificate = CertificateGenerator.generateRSAWithSHA256()
        val chain = Array[X509Certificate](certificate)
        val authType = ""

        mockTrustManager1.checkClientTrusted(chain, authType) throws new CertificateException("fake1")
        mockTrustManager2.checkClientTrusted(chain, authType)

        trustManager.checkClientTrusted(chain, authType) must not(throwA[Throwable])
      }
    }

    "getAcceptedIssuers" should {
      "work fine" in {
        val mockTrustManager = mock[X509TrustManager]
        val algorithmChecker = new AlgorithmChecker(Set(), Set())
        val trustManager = new CompositeX509TrustManager(trustManagers = Seq(mockTrustManager), algorithmChecker)
        val certificate = CertificateGenerator.generateRSAWithSHA256()
        mockTrustManager.getAcceptedIssuers returns Array[X509Certificate](certificate)

        val acceptedIssuers = trustManager.getAcceptedIssuers
        acceptedIssuers(0) must_== certificate
      }

      "throw exception when input exception" in {
        val mockTrustManager = mock[X509TrustManager]
        val algorithmChecker = new AlgorithmChecker(Set(), Set())
        val trustManager = new CompositeX509TrustManager(trustManagers = Seq(mockTrustManager), algorithmChecker)
        mockTrustManager.getAcceptedIssuers throws new RuntimeException("fake")

        trustManager.getAcceptedIssuers.must(throwA[CompositeCertificateException].like {
          case e: CompositeCertificateException =>
            val sourceExceptions = e.getSourceExceptions
            sourceExceptions(0).getMessage must be_==("fake")
        })
      }
    }

    "checkServerTrusted" should {

      "work fine" in {
        val mockTrustManager = mock[X509TrustManager]
        val algorithmChecker = new AlgorithmChecker(Set(), Set())
        val trustManager = new CompositeX509TrustManager(trustManagers = Seq(mockTrustManager), algorithmChecker)
        val certificate = CertificateGenerator.generateRSAWithSHA256()
        val chain = Array[X509Certificate](certificate)
        val authType = ""

        trustManager.checkServerTrusted(chain, authType) must not(throwA[Throwable])
      }

      "throw an exception when nothing works" in {
        val mockTrustManager1 = mock[X509TrustManager]
        val mockTrustManager2 = mock[X509TrustManager]
        val algorithmChecker = new AlgorithmChecker(Set(), Set())
        val trustManager = new CompositeX509TrustManager(trustManagers = Seq(mockTrustManager1, mockTrustManager2), algorithmChecker)

        val certificate = CertificateGenerator.generateRSAWithSHA256()
        val chain = Array[X509Certificate](certificate)
        val authType = ""

        mockTrustManager1.checkServerTrusted(chain, authType) throws new CertificateException("fake1")
        mockTrustManager2.checkServerTrusted(chain, authType) throws new CertificateException("fake2")

        trustManager.checkServerTrusted(chain, authType).must(throwA[CompositeCertificateException].like {
          case e: CompositeCertificateException =>
            val sourceExceptions = e.getSourceExceptions
            sourceExceptions(0).getMessage must be_==("fake1")
            sourceExceptions(1).getMessage must be_==("fake2")
        })
      }
    }
  }
}
