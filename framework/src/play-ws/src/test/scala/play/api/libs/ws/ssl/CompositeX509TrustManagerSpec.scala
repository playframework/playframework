/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._
import org.specs2.mock._

import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

object CompositeX509TrustManagerSpec extends Specification with Mockito {

  "CompositeX509TrustManager" should {

    "with checkClientTrusted" should {

      "throws exception" in {
        val mockTrustManager = mock[X509TrustManager]
        val certificateValidator = mock[CertificateValidator]
        val trustManager = new CompositeX509TrustManager(trustManagers = Seq(mockTrustManager), certificateValidator = certificateValidator)

        val certificate = CertificateGenerator.generateRSAWithMD5()
        val chain = Array[X509Certificate](certificate)
        val authType = ""

        mockTrustManager.checkClientTrusted(chain, authType) throws new CompositeCertificateException("fake", Array())

        trustManager.checkClientTrusted(chain, authType).must(throwA[CompositeCertificateException])
      }

      "does not throw exception" in todo

    }

    "getAcceptedIssuers" in {
      todo
    }

    "checkServerTrusted" in {
      todo
    }

  }
}
