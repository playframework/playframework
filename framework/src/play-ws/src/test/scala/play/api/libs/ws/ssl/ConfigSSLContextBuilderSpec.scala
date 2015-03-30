/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import java.security._
import java.security.cert.CertPathValidatorException
import javax.net.ssl._

import org.specs2.mock._
import org.specs2.mutable._
import play.core.server.ssl.FakeKeyStore

class ConfigSSLContextBuilderSpec extends Specification with Mockito {

  val CACERTS = s"${System.getProperty("java.home")}/lib/security/cacerts"

  "ConfigSSLContentBuilder" should {

    "should have the right protocol by default" in {
      val info = SSLConfig()

      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val actual: SSLContext = builder.build
      actual.getProtocol must_== Protocols.recommendedProtocol
    }

    "with protocol" should {

      "should default to Protocols.recommendedProtocols" in {
        val info = SSLConfig()

        val keyManagerFactory = mock[KeyManagerFactoryWrapper]
        val trustManagerFactory = mock[TrustManagerFactoryWrapper]
        val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

        val actual: SSLContext = builder.build
        actual.getProtocol must_== Protocols.recommendedProtocol
      }

      "should have an explicit protocol if defined" in {
        val info = SSLConfig(protocol = "TLS")

        val keyManagerFactory = mock[KeyManagerFactoryWrapper]
        val trustManagerFactory = mock[TrustManagerFactoryWrapper]
        val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

        val actual: SSLContext = builder.build
        actual.getProtocol must_== "TLS"
      }
    }

    "build a key manager" in {
      val info = SSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val keyStore = KeyStore.getInstance("PKCS12")
      val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
      keyPairGenerator.initialize(2048) // 2048 is the NIST acceptable key length until 2030
      val keyPair = keyPairGenerator.generateKeyPair()
      val cert = FakeKeyStore.createSelfSignedCertificate(keyPair)
      val password = "changeit" // cannot have a null password for PKCS12 in 1.6
      keyStore.load(null, password.toCharArray)
      keyStore.setKeyEntry("playgenerated", keyPair.getPrivate, password.toCharArray, Array(cert))

      val tempFile = java.io.File.createTempFile("privatekeystore", ".p12")
      val out = new java.io.FileOutputStream(tempFile)
      try {
        keyStore.store(out, password.toCharArray)
      } finally {
        out.close()
      }
      val filePath = tempFile.getAbsolutePath
      val keyStoreConfig = KeyStoreConfig(storeType = "PKCS12", data = None, filePath = Some(filePath), password = Some(password))

      val keyManager = mock[X509KeyManager]
      keyManagerFactory.getKeyManagers returns Array(keyManager)

      val disabledKeyAlgorithms = Set(AlgorithmConstraint("RSA", Some(LessThan(1024))))
      val algorithmChecker = new AlgorithmChecker(Set(), keyConstraints = disabledKeyAlgorithms)
      val actual = builder.buildKeyManager(keyStoreConfig, algorithmChecker)
      actual must beAnInstanceOf[X509KeyManager]
    }

    "build a trust manager" in {
      val info = SSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val trustManager = mock[X509TrustManager]
      trustManagerFactory.getTrustManagers returns Array(trustManager)
      val disabledSignatureAlgorithms = Set(AlgorithmConstraint("md5"))
      val disabledKeyAlgorithms = Set(AlgorithmConstraint("RSA", Some(LessThan(1024))))
      val algorithmChecker = new AlgorithmChecker(disabledSignatureAlgorithms, disabledKeyAlgorithms)

      val trustManagerConfig = TrustManagerConfig()
      val checkRevocation = false
      val revocationLists = None

      val actual = builder.buildCompositeTrustManager(trustManagerConfig, checkRevocation, revocationLists, algorithmChecker)
      actual must beAnInstanceOf[javax.net.ssl.X509TrustManager]
    }

    "build a composite key manager" in {
      val info = SSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val keyManagerConfig = new KeyManagerConfig()

      val disabledKeyAlgorithms = Set(AlgorithmConstraint("RSA", Some(LessThan(1024))))
      val algorithmChecker = new AlgorithmChecker(Set(), disabledKeyAlgorithms)

      val actual = builder.buildCompositeKeyManager(keyManagerConfig, algorithmChecker)
      actual must beAnInstanceOf[CompositeX509KeyManager]
    }

    "build a composite trust manager" in {
      val info = SSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val disabledSignatureAlgorithms = Set(AlgorithmConstraint("md5"))
      val disabledKeyAlgorithms = Set(AlgorithmConstraint("RSA", Some(LessThan(1024))))
      val algorithmChecker = new AlgorithmChecker(disabledSignatureAlgorithms, disabledKeyAlgorithms)

      val trustManagerConfig = TrustManagerConfig()
      val checkRevocation = false
      val revocationLists = None

      val actual = builder.buildCompositeTrustManager(trustManagerConfig,
        checkRevocation,
        revocationLists, algorithmChecker)
      actual must beAnInstanceOf[CompositeX509TrustManager]
    }

    "build a composite trust manager with data" in {
      val info = SSLConfig()
      val keyManagerFactory = new DefaultKeyManagerFactoryWrapper(KeyManagerFactory.getDefaultAlgorithm)
      val trustManagerFactory = new DefaultTrustManagerFactoryWrapper(TrustManagerFactory.getDefaultAlgorithm)
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val certificate = CertificateGenerator.generateRSAWithSHA256()
      val certificateData = CertificateGenerator.toPEM(certificate)

      val trustStoreConfig = TrustStoreConfig(storeType = "PEM", data = Some(certificateData), filePath = None)
      val trustManagerConfig = TrustManagerConfig(trustStoreConfigs = Seq(trustStoreConfig))

      val disabledSignatureAlgorithms = Set(AlgorithmConstraint("md5"))
      val disabledKeyAlgorithms = Set(AlgorithmConstraint("RSA", Some(LessThan(1024))))
      val algorithmChecker = new AlgorithmChecker(disabledSignatureAlgorithms, disabledKeyAlgorithms)

      val checkRevocation = false
      val revocationLists = None

      val actual = builder.buildCompositeTrustManager(trustManagerConfig, checkRevocation, revocationLists, algorithmChecker)

      actual must beAnInstanceOf[CompositeX509TrustManager]
      val issuers = actual.getAcceptedIssuers
      issuers.size must beEqualTo(1)
    }

    "build a file based keystore builder" in {
      val info = SSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val storeType = KeyStore.getDefaultType
      val filePath = "derp"

      val actual = builder.fileBuilder(storeType, filePath, None)
      actual must beAnInstanceOf[FileBasedKeyStoreBuilder]
    }

    "build a string based keystore builder" in {
      val info = SSLConfig()
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val data = "derp"

      val actual = builder.stringBuilder(data)
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
      val tsc = TrustStoreConfig(storeType = "PEM", data = Some(data), filePath = None)
      val trustManagerConfig = TrustManagerConfig(trustStoreConfigs = Seq(tsc))

      val info = SSLConfig(trustManagerConfig = trustManagerConfig)
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val trustStore = builder.trustStoreBuilder(tsc).build()
      val checker: AlgorithmChecker = new AlgorithmChecker(signatureConstraints = Set(), keyConstraints = disabledKeyAlgorithms)

      builder.validateStore(trustStore, checker)
      trustStore.size() must beEqualTo(0)
    }

    "validate success of the keystore with a private key" in {
      val keyStore = KeyStore.getInstance("PKCS12")

      // Generate the key pair
      val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
      keyPairGenerator.initialize(2048) // 2048 is the NIST acceptable key length until 2030
      val keyPair = keyPairGenerator.generateKeyPair()

      // Generate a self signed certificate
      val cert = FakeKeyStore.createSelfSignedCertificate(keyPair)

      val password = "changeit" // null passwords throw exception in 1.6
      keyStore.load(null, password.toCharArray)
      keyStore.setKeyEntry("playgenerated", keyPair.getPrivate, password.toCharArray, Array(cert))

      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]

      val ksc = KeyStoreConfig(filePath = Some("path"), password = Some(password))
      val keyManagerConfig = KeyManagerConfig(keyStoreConfigs = Seq(ksc))

      val info = SSLConfig(keyManagerConfig = keyManagerConfig)
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)
      builder.validateStoreContainsPrivateKeys(ksc, keyStore) must beTrue
    }

    "validate a failure of the keystore without a private key" in {
      // must be JKS, PKCS12 does not support trusted certificate entries in 1.6 at least
      // KeyStoreException: : TrustedCertEntry not supported  (PKCS12KeyStore.java:620)
      // val keyStore = KeyStore.getInstance("PKCS12")
      val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)

      // Generate the key pair
      val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
      keyPairGenerator.initialize(2048) // 2048 is the NIST acceptable key length until 2030
      val keyPair = keyPairGenerator.generateKeyPair()

      // Generate a self signed certificate
      val cert = FakeKeyStore.createSelfSignedCertificate(keyPair)

      val password = "changeit" // null passwords throw exception in 1.6 in PKCS12
      keyStore.load(null, password.toCharArray)
      // Don't add the private key here, instead add a public cert only.
      keyStore.setCertificateEntry("playgeneratedtrusted", cert)

      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]

      val ksc = KeyStoreConfig(filePath = Some("path"), password = Some(password))
      val keyManagerConfig = KeyManagerConfig(keyStoreConfigs = Seq(ksc))

      val info = SSLConfig(keyManagerConfig = keyManagerConfig)
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      builder.validateStoreContainsPrivateKeys(ksc, keyStore) must beFalse
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
      val tsc = TrustStoreConfig(storeType = "PEM", data = Some(data), filePath = None)
      val trustManagerConfig = TrustManagerConfig(trustStoreConfigs = Seq(tsc))

      val info = SSLConfig(trustManagerConfig = trustManagerConfig)
      val builder = new ConfigSSLContextBuilder(info, keyManagerFactory, trustManagerFactory)

      val trustStore = builder.trustStoreBuilder(tsc).build()
      val checker: AlgorithmChecker = new AlgorithmChecker(signatureConstraints = Set(), keyConstraints = disabledKeyAlgorithms)

      {
        builder.validateStore(trustStore, checker)
      }.must(not(throwAn[CertPathValidatorException]))
    }

    "warnOnPKCS12EmptyPasswordBug returns true when a PKCS12 keystore has a null or empty password" in {
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]

      val ksc = KeyStoreConfig(storeType = "PKCS12", filePath = Some("path"))
      val keyManagerConfig = KeyManagerConfig(keyStoreConfigs = Seq(ksc))
      val sslConfig = SSLConfig(keyManagerConfig = keyManagerConfig)

      val builder = new ConfigSSLContextBuilder(sslConfig, keyManagerFactory, trustManagerFactory)

      builder.warnOnPKCS12EmptyPasswordBug(ksc) must beTrue
    }

    "warnOnPKCS12EmptyPasswordBug returns false when a PKCS12 keystore has a password" in {
      val keyManagerFactory = mock[KeyManagerFactoryWrapper]
      val trustManagerFactory = mock[TrustManagerFactoryWrapper]

      val ksc = KeyStoreConfig(storeType = "PKCS12", filePath = Some("path"), password = Some("password"))
      val keyManagerConfig = KeyManagerConfig(keyStoreConfigs = Seq(ksc))
      val sslConfig = SSLConfig(keyManagerConfig = keyManagerConfig)

      val builder = new ConfigSSLContextBuilder(sslConfig, keyManagerFactory, trustManagerFactory)

      builder.warnOnPKCS12EmptyPasswordBug(ksc) must beFalse
    }
  }

}
