/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._

import org.specs2.mock._

import java.security.{Principal, PrivateKey, KeyStore}
import javax.net.ssl._
import scala.Some
import java.security.cert.X509Certificate
import java.net.Socket
import play.api.libs.ws.ssl.AlgorithmConstraint
import play.api.libs.ws.ssl.AlgorithmConstraint

object ConfigSSLContextBuilderSpec extends Specification with Mockito {

  val CACERTS = s"${System.getProperty("java.home")}/lib/security/cacerts"

  "ConfigSSLContentBuilder" should {

    // builder.build() -> SSLContext
    "build" in todo

    "should have the right protocol by default" in {
      val info = DefaultSSLConfig()

      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val actual: SSLContext = builder.build
      actual.getProtocol must_== (Protocols.recommendedProtocol)
    }

    "should have an explicit protocol" in {
      val info = DefaultSSLConfig()

      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val actual: SSLContext = builder.build()
      actual.getProtocol must_== Protocols.recommendedProtocol
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

      val actual = builder.buildKeyManager(disabledAlgorithms, keyStoreConfig)
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

      trustManagerFactory.getTrustManagers returns Array {
        new X509TrustManager {
          def getAcceptedIssuers: Array[X509Certificate] = ???

          def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = ???

          def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = ???
        }
      }
      val actual = builder.buildTrustManager(disabledAlgorithms, trustStoreConfig)
      actual must beAnInstanceOf[javax.net.ssl.X509TrustManager]
    }

    "build a composite key manager" in {
      val info = DefaultSSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val disabledAlgorithms = Set(AlgorithmConstraint("md5"))
      val keyManagerConfig = new DefaultKeyManagerConfig()

      val actual = builder.buildCompositeKeyManager(disabledAlgorithms, keyManagerConfig)
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

      val actual = builder.buildCompositeTrustManager(disabledAlgorithms, trustManagerConfig, certificateValidator)
      actual must beAnInstanceOf[CompositeX509TrustManager]
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
