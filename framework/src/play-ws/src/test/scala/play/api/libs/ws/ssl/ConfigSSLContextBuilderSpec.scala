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
import java.net.Socket
import java.security.cert.CertPathValidatorException


class ConfigSSLContextBuilderSpec extends Specification with Mockito {

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

    "build a key manager" in {
      val info = DefaultSSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val storeType = Some(KeyStore.getDefaultType)
      val filePath = Some(CACERTS)

      val keyStoreConfig = DefaultKeyStoreConfig(storeType, filePath, None, None)

      val keyManager = mock[X509KeyManager]
      keyManagerFactory.getKeyManagers returns Array(keyManager)

      val disabledKeyAlgorithms = Set(AlgorithmConstraint("RSA", Some(LessThan(1024))))
      val actual = builder.buildKeyManager(keyStoreConfig, disabledKeyAlgorithms)
      actual must beAnInstanceOf[X509KeyManager]
    }

    "build a trust manager" in {
      val info = DefaultSSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val storeType = Some(KeyStore.getDefaultType)
      val filePath = Some(CACERTS)

      val trustManager = mock[X509TrustManager]
      trustManagerFactory.getTrustManagers returns Array(trustManager)
      val disabledSignatureAlgorithms = Set(AlgorithmConstraint("md5"))
      val disabledKeyAlgorithms = Set(AlgorithmConstraint("RSA", Some(LessThan(1024))))
      val trustManagerConfig = DefaultTrustManagerConfig()
      val checkRevocation = false
      val revocationLists = None

      val actual = builder.buildCompositeTrustManager(trustManagerConfig,
        checkRevocation,
        revocationLists,
        disabledSignatureAlgorithms,
        disabledKeyAlgorithms)
      actual must beAnInstanceOf[javax.net.ssl.X509TrustManager]
    }

    "build a composite key manager" in {
      val info = DefaultSSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val keyManagerConfig = new DefaultKeyManagerConfig()

      val disabledKeyAlgorithms = Set(AlgorithmConstraint("RSA", Some(LessThan(1024))))
      val actual = builder.buildCompositeKeyManager(keyManagerConfig, disabledKeyAlgorithms)
      actual must beAnInstanceOf[CompositeX509KeyManager]
    }

    "build a composite trust manager" in {
      val info = DefaultSSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val disabledSignatureAlgorithms = Set(AlgorithmConstraint("md5"))
      val disabledKeyAlgorithms = Set(AlgorithmConstraint("RSA", Some(LessThan(1024))))
      val trustManagerConfig = DefaultTrustManagerConfig()
      val checkRevocation = false
      val revocationLists = None

      val actual = builder.buildCompositeTrustManager(trustManagerConfig,
        checkRevocation,
        revocationLists,
        disabledSignatureAlgorithms,
        disabledKeyAlgorithms)
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

      val disabledSignatureAlgorithms = Set(AlgorithmConstraint("md5"))
      val disabledKeyAlgorithms = Set(AlgorithmConstraint("RSA", Some(LessThan(1024))))
      val checkRevocation = false
      val revocationLists = None

      val actual = builder.buildCompositeTrustManager(trustManagerConfig,
        checkRevocation,
        revocationLists,
        disabledSignatureAlgorithms,
        disabledKeyAlgorithms)

      actual must beAnInstanceOf[CompositeX509TrustManager]
      val issuers = actual.getAcceptedIssuers
      issuers.size must beEqualTo(1)
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

    "fail the keystore with a weak certificate " in {
      // RSA 1024 public key
      val data = """-----BEGIN CERTIFICATE-----
                   |MIICcjCCAdugAwIBAgIEKdPHODANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdV
                   |bmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD
                   |VQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3du
                   |MB4XDTE0MDQxMzAxMzgwN1oXDTE0MTAxMDAxMzgwN1owbDEQMA4GA1UEBhMHVW5r
                   |bm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UE
                   |ChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCB
                   |nzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAoZCRpCJyes/P8yGeSSskZ8mtgH+H
                   |yQOnpnXcOko34Ys0mWhjxKfJjSmkUHdSfKarZ2hFmmNv05+1Og02tIIscIUraFyc
                   |ujPsjYU3B1QUeW4duB0l/MJVwBYxpbulZuV9joNWyAyPvyE2Z3F91Ka77/FobqfI
                   |sAbbM+qWzQspU2cCAwEAAaMhMB8wHQYDVR0OBBYEFFdSKbOXk6Of//DrMQksYUmP
                   |WYTmMA0GCSqGSIb3DQEBCwUAA4GBAEVNSa8wtsLArg+4IQXs1P4JUAMPieOC3DjC
                   |ioplN4UHccCXXmpBe2zkCvIxkmRUjfaYfAehHJvJSitUkBupAQSwv3rNo5zIfqCe
                   |NzBCZh8S+IPQZa+gaE8MrLsDDzD0ZoSOT8fx5TSgF8eTUgkKxkX4I51C9B5t2SCB
                   |0Pl6Lah4
                   |-----END CERTIFICATE-----
                 """.stripMargin

      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]

      // This
      val disabledKeyAlgorithms = Set(AlgorithmConstraint("RSA", Some(Equal(1024))))
      //val disabledKeyAlgorithms = Set[AlgorithmConstraint]()
      val tsc = DefaultTrustStoreConfig(storeType = Some("PEM"), data = Some(data), filePath = None)
      val trustManagerConfig = DefaultTrustManagerConfig(trustStoreConfigs = Seq(tsc))

      val info = DefaultSSLConfig(trustManagerConfig = Some(trustManagerConfig))
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val trustStore = builder.trustStoreBuilder(tsc).build()
      val checker:AlgorithmChecker = new AlgorithmChecker(signatureConstraints = Set(), keyConstraints = disabledKeyAlgorithms)

      builder.validateStore(trustStore, checker)
      trustStore.size() must beEqualTo(0)
    }

    "validate the keystore with a weak certificate " in {
      // RSA 1024 public key
      val data = """-----BEGIN CERTIFICATE-----
                   |MIICcjCCAdugAwIBAgIEKdPHODANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdV
                   |bmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD
                   |VQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3du
                   |MB4XDTE0MDQxMzAxMzgwN1oXDTE0MTAxMDAxMzgwN1owbDEQMA4GA1UEBhMHVW5r
                   |bm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UE
                   |ChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCB
                   |nzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAoZCRpCJyes/P8yGeSSskZ8mtgH+H
                   |yQOnpnXcOko34Ys0mWhjxKfJjSmkUHdSfKarZ2hFmmNv05+1Og02tIIscIUraFyc
                   |ujPsjYU3B1QUeW4duB0l/MJVwBYxpbulZuV9joNWyAyPvyE2Z3F91Ka77/FobqfI
                   |sAbbM+qWzQspU2cCAwEAAaMhMB8wHQYDVR0OBBYEFFdSKbOXk6Of//DrMQksYUmP
                   |WYTmMA0GCSqGSIb3DQEBCwUAA4GBAEVNSa8wtsLArg+4IQXs1P4JUAMPieOC3DjC
                   |ioplN4UHccCXXmpBe2zkCvIxkmRUjfaYfAehHJvJSitUkBupAQSwv3rNo5zIfqCe
                   |NzBCZh8S+IPQZa+gaE8MrLsDDzD0ZoSOT8fx5TSgF8eTUgkKxkX4I51C9B5t2SCB
                   |0Pl6Lah4
                   |-----END CERTIFICATE-----
                 """.stripMargin

      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]

      // This
      val disabledKeyAlgorithms = Set(AlgorithmConstraint("RSA", Some(LessThan(1024))))
      //val disabledKeyAlgorithms = Set[AlgorithmConstraint]()
      val tsc = DefaultTrustStoreConfig(storeType = Some("PEM"), data = Some(data), filePath = None)
      val trustManagerConfig = DefaultTrustManagerConfig(trustStoreConfigs = Seq(tsc))

      val info = DefaultSSLConfig(trustManagerConfig = Some(trustManagerConfig))
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val trustStore = builder.trustStoreBuilder(tsc).build()
      val checker:AlgorithmChecker = new AlgorithmChecker(signatureConstraints = Set(), keyConstraints = disabledKeyAlgorithms)

      {
        builder.validateStore(trustStore, checker)
      }.must(not(throwAn[CertPathValidatorException]))
    }

  }

}
