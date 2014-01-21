/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._

import org.specs2.mock._

import javax.net.ssl._
import java.security.{Principal, PrivateKey, KeyStore}
import java.security.cert.X509Certificate
import java.net.Socket
import org.joda.time.Instant

object ConfigSSLContextBuilderSpec extends Specification with Mockito {

  val CACERTS = s"${System.getProperty("java.home")}/lib/security/cacerts"

  "ConfigSSLContentBuilder" should {

    "should have the right protocol by default" in {
      val info = DefaultSSLConfig()

      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val actual: SSLContext = builder.build
      actual.getProtocol must_== Protocols.recommendedProtocol
    }

    "with protocol" should {

      "should default to Protocols.recommendedProtocols" in {
        val info = DefaultSSLConfig()

        val keyManagerFactory = mock[KeyManagerFactoryWrapper]
        val trustManagerFactory = mock[TrustManagerFactoryWrapper]
        val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

        val actual: SSLContext = builder.build
        actual.getProtocol must_== Protocols.recommendedProtocol
      }

      "should have an explicit protocol if defined" in {
        val info = DefaultSSLConfig(protocol = Some("TLS"))

        val keyManagerFactory = mock[KeyManagerFactoryWrapper]
        val trustManagerFactory = mock[TrustManagerFactoryWrapper]
        val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

        val actual: SSLContext = builder.build
        actual.getProtocol must_== "TLS"
      }
    }

    "with a certificate validator" should {

      "build a certificate validator with explicit disabledAlgorithms" in {
        val info = DefaultSSLConfig(disabledAlgorithms = Some("totally, fake, algorithms"))
        val keyManagerFactory = mock[KeyManagerFactoryWrapper]
        val trustManagerFactory = mock[TrustManagerFactoryWrapper]
        val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

        val actual = builder.buildCertificateValidator(info)
        actual must beAnInstanceOf[CertificateValidator]
        actual.constraints must containTheSameElementsAs(Seq(AlgorithmConstraint("totally"), AlgorithmConstraint("fake"), AlgorithmConstraint("algorithms")))
      }

      "build a certificate validator with defaults" in {
        val info = DefaultSSLConfig()
        val keyManagerFactory = mock[KeyManagerFactoryWrapper]
        val trustManagerFactory = mock[TrustManagerFactoryWrapper]
        val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

        val actual = builder.buildCertificateValidator(info)
        actual must beAnInstanceOf[CertificateValidator]

        val defaultAlgorithms = AlgorithmConstraintsParser.parseAll(AlgorithmConstraintsParser.line, Algorithms.disabledAlgorithms).get
        actual.constraints must containTheSameElementsAs(defaultAlgorithms)
      }
    }

    "build a key manager" in {
      val info = DefaultSSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val disabledAlgorithms = Set(AlgorithmConstraint("md5"))
      val storeType = Some(KeyStore.getDefaultType)
      val filePath = Some(CACERTS)

      val keyStoreConfig = DefaultKeyStoreConfig(storeType, filePath, None, None)

      // XXX replace with mock?
      keyManagerFactory.getKeyManagers returns Array {
        new X509ExtendedKeyManager() {
          def getClientAliases(keyType: String, issuers: Array[Principal]): Array[String] = ???

          def chooseClientAlias(keyType: Array[String], issuers: Array[Principal], socket: Socket): String = ???

          def getServerAliases(keyType: String, issuers: Array[Principal]): Array[String] = ???

          def chooseServerAlias(keyType: String, issuers: Array[Principal], socket: Socket): String = ???

          def getCertificateChain(alias: String): Array[X509Certificate] = ???

          def getPrivateKey(alias: String): PrivateKey = ???
        }
      }

      val actual = builder.buildKeyManager(keyStoreConfig)
      actual must beAnInstanceOf[X509KeyManager]
    }

    "build a trust manager" in {
      val info = DefaultSSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val disabledAlgorithms = Set(AlgorithmConstraint("md5"))
      val storeType = Some(KeyStore.getDefaultType)
      val filePath = Some(CACERTS)
      val trustStoreConfig = DefaultTrustStoreConfig(storeType, filePath, None)

      // XXX replace with mock?
      trustManagerFactory.getTrustManagers returns Array {
        new X509TrustManager {
          def getAcceptedIssuers: Array[X509Certificate] = ???

          def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = ???

          def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = ???
        }
      }
      val actual = builder.buildTrustManager(trustStoreConfig, false)
      actual must beAnInstanceOf[javax.net.ssl.X509TrustManager]
    }

    "build a composite key manager" in {
      val info = DefaultSSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val keyManagerConfig = new DefaultKeyManagerConfig()

      val actual = builder.buildCompositeKeyManager(keyManagerConfig)
      actual must beAnInstanceOf[CompositeX509KeyManager]
    }

    "build a composite trust manager" in {
      val info = DefaultSSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val disabledAlgorithms = Set(AlgorithmConstraint("md5"))
      val trustManagerConfig = DefaultTrustManagerConfig()
      val certificateValidator = new CertificateValidator(disabledAlgorithms, false)

      val actual = builder.buildCompositeTrustManager(trustManagerConfig, certificateValidator, false)
      actual must beAnInstanceOf[CompositeX509TrustManager]
    }

    "build a composite trust manager with data" in {
      val info = DefaultSSLConfig()
      val keyManagerFactory = new DefaultKeyManagerFactoryWrapper(KeyManagerFactory.getDefaultAlgorithm)
      val trustManagerFactory = new DefaultTrustManagerFactoryWrapper(TrustManagerFactory.getDefaultAlgorithm)
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val certificate = CertificateGenerator.generateRSAWithSHA256()
      val certificateData = CertificateGenerator.toPEM(certificate)

      val trustStoreConfig = DefaultTrustStoreConfig(storeType = Some("PEM"), data = Some(certificateData), filePath = None)
      val trustManagerConfig = DefaultTrustManagerConfig(trustStoreConfigs = Seq(trustStoreConfig))
      val disabledAlgorithms = Set(AlgorithmConstraint("md5"))
      val certificateValidator = new CertificateValidator(disabledAlgorithms, false)

      val actual = builder.buildCompositeTrustManager(trustManagerConfig, certificateValidator, false)

      actual must beAnInstanceOf[CompositeX509TrustManager]
      val issuers = actual.getAcceptedIssuers
      issuers.size must beEqualTo(1)
    }

    "throw an exception when an expired certificate is the only cert passed into a trust store" in {
      val info = DefaultSSLConfig()
      val keyManagerFactory = new DefaultKeyManagerFactoryWrapper(KeyManagerFactory.getDefaultAlgorithm)
      val trustManagerFactory = new DefaultTrustManagerFactoryWrapper(TrustManagerFactory.getDefaultAlgorithm)
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val from = new Instant(0)
      val certificate = CertificateGenerator.generateRSAWithSHA256(from = from, duration = 5000)
      val certificateData = CertificateGenerator.toPEM(certificate)
      val trustStoreConfig = DefaultTrustStoreConfig(storeType = Some("PEM"), data = Some(certificateData), filePath = None)
      val trustManagerConfig = DefaultTrustManagerConfig(trustStoreConfigs = Seq(trustStoreConfig))

      val disabledAlgorithms = Set(AlgorithmConstraint("md5"))
      val certificateValidator = new CertificateValidator(disabledAlgorithms, false)

      builder.buildCompositeTrustManager(trustManagerConfig, certificateValidator, false).must(throwAn[IllegalStateException])
    }

    "build a file based keystore builder" in {
      val info = DefaultSSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val storeType = KeyStore.getDefaultType
      val filePath = "derp"

      val actual = builder.fileBuilder(storeType, filePath, None)
      actual must beAnInstanceOf[FileBasedKeyStoreBuilder]
    }

    "build a string based keystore builder" in {
      val info = DefaultSSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val storeType = KeyStore.getDefaultType
      val data = "derp"

      val actual = builder.stringBuilder(storeType, data, None)
      actual must beAnInstanceOf[StringBasedKeyStoreBuilder]
    }

  }

}
