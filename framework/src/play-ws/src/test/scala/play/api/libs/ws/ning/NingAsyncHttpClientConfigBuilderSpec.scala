/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ning

import org.specs2.mutable._
import org.specs2.mock._

import play.api.libs.ws.{Defaults => WSDefaults}
import play.api.libs.ws.ssl._

import play.api.libs.ws.ssl.DefaultSSLConfig
import play.api.libs.ws.DefaultWSClientConfig

import javax.net.ssl.{HostnameVerifier, SSLContext, X509TrustManager}
import com.ning.http.client.ProxyServerSelector
import com.ning.http.util.{ProxyUtils, AllowAllHostnameVerifier}
import play.api.libs.ws.ssl.DefaultHostnameVerifier
import org.slf4j.LoggerFactory

class NingAsyncHttpClientConfigBuilderSpec extends Specification with Mockito {

  val defaultConfig = new DefaultWSClientConfig()

  "NingAsyncHttpClientConfigBuilder" should {

    "with basic options" should {

      "provide a basic default client with default settings" in {
        val config = defaultConfig
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        val actual = builder.build()

        actual.getIdleConnectionTimeoutInMs must_== WSDefaults.idleTimeout
        actual.getRequestTimeoutInMs must_== WSDefaults.requestTimeout
        actual.getConnectionTimeoutInMs must_== WSDefaults.requestTimeout
        actual.isRedirectEnabled must_== WSDefaults.followRedirects

        val sslEngine = actual.getSSLEngineFactory.newSSLEngine()
        sslEngine.getEnabledCipherSuites.toSeq must not contain Ciphers.deprecatedCiphers
        sslEngine.getEnabledProtocols.toSeq must not contain Protocols.deprecatedProtocols

        actual.getHostnameVerifier must beAnInstanceOf[DefaultHostnameVerifier]
      }

      "use an explicit idle timeout" in {
        val config = defaultConfig.copy(idleTimeout = Some(42L))
        val builder = new NingAsyncHttpClientConfigBuilder(config)

        val actual = builder.build()
        actual.getIdleConnectionTimeoutInMs must_== 42L
      }

      "use an explicit request timeout" in {
        val config = defaultConfig.copy(requestTimeout = Some(47L))
        val builder = new NingAsyncHttpClientConfigBuilder(config)

        val actual = builder.build()
        actual.getRequestTimeoutInMs must_== 47L
      }

      "use an explicit connection timeout" in {
        val config = defaultConfig.copy(connectionTimeout = Some(99L))
        val builder = new NingAsyncHttpClientConfigBuilder(config)

        val actual = builder.build()
        actual.getConnectionTimeoutInMs must_== 99L
      }

      "use an explicit followRedirects option" in {
        val config = defaultConfig.copy(followRedirects = Some(false))
        val builder = new NingAsyncHttpClientConfigBuilder(config)

        val actual = builder.build()
        actual.isRedirectEnabled must_== false
      }

      "use an explicit proxy if useProxyProperties is true and there are system defined proxy settings" in {
        val config = defaultConfig.copy(useProxyProperties = Some(true))

        // Used in ProxyUtils.createProxyServerSelector
        try {
          System.setProperty(ProxyUtils.PROXY_HOST, "localhost")
          val builder = new NingAsyncHttpClientConfigBuilder(config)
          val actual = builder.build()

          val proxyServerSelector = actual.getProxyServerSelector

          proxyServerSelector must not beNull

          proxyServerSelector must not be_== ProxyServerSelector.NO_PROXY_SELECTOR
        } finally {
          // Unset http.proxyHost
          System.clearProperty(ProxyUtils.PROXY_HOST)
        }
      }
    }

    "with SSL options" should {

      // The ConfigSSLContextBuilder does most of the work here, but there are a couple of things outside of the
      // SSL context proper...

    "with context" should {

      "use the configured trustmanager and keymanager if context not passed in" in {
        // Stick a spy into the SSL config so we can verify that things get called on it that would
        // only be called if it was using the config trust manager...

        val sslConfig = spy(DefaultSSLConfig(default = Some(false)))
        val config = defaultConfig.copy(ssl = Some(sslConfig))
        val builder = new NingAsyncHttpClientConfigBuilder(config)

        org.mockito.Mockito.doReturn(Some("TLS")).when(sslConfig).protocol

        val asyncClientConfig = builder.build()

        // Check that the spy got called...
        org.mockito.Mockito.verify(sslConfig)

        // ...and return a result so specs2 is happy.
        asyncClientConfig.getSSLContext must not beNull
      }

      "use the default with a current certificate" in {
        val tmc = DefaultTrustManagerConfig()
        val config = defaultConfig.copy(ssl = Some(DefaultSSLConfig(default = Some(true), trustManagerConfig = Some(tmc))))
        val builder = new NingAsyncHttpClientConfigBuilder(config)

        val asyncClientConfig = builder.build()
        val sslContext = asyncClientConfig.getSSLContext
        sslContext must beEqualTo(SSLContext.getDefault)
      }

      "log a warning if sslConfig.default is passed in with an weak certificate" in {
        import ch.qos.logback.classic.spi._
        import ch.qos.logback.classic._

        // Pass in a configuration which is guaranteed to fail, by banning RSA, DSA and EC certificates
        val config = defaultConfig.copy(ssl = Some(DefaultSSLConfig(default = Some(true), disabledKeyAlgorithms = Some("RSA, DSA, EC"))))
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        // this only works with test:test, has a different type in test:testQuick and test:testOnly!
        val logger = builder.logger.asInstanceOf[ch.qos.logback.classic.Logger]
        val appender = new ch.qos.logback.core.read.ListAppender[ILoggingEvent]()
        val lc = LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext]
        appender.setContext(lc)
        appender.start()
        logger.addAppender(appender)
        logger.setLevel(Level.WARN)

        builder.build()

        val warnings = appender.list
        warnings.size must beGreaterThan(0)
      }
    }
      "with acceptAnyCertificate" should {

        "still use enabledCipherSuites" in {
          val sslConfig = DefaultSSLConfig(enabledCipherSuites = Some(Seq("SSL_RSA_WITH_RC4_128_MD5")))
          val config = defaultConfig.copy(acceptAnyCertificate = Some(true), ssl = Some(sslConfig))
          val builder = new NingAsyncHttpClientConfigBuilder(config)

          val actual = builder.build()
          val sslEngine = actual.getSSLEngineFactory.newSSLEngine()
          val cipherSuites = sslEngine.getEnabledCipherSuites

          cipherSuites must beEqualTo(Array("SSL_RSA_WITH_RC4_128_MD5"))
        }

        "still use enabledProtocols" in {
          val sslConfig = DefaultSSLConfig(enabledProtocols = Some(Seq("TLSv1")))
          val config = defaultConfig.copy(acceptAnyCertificate = Some(true), ssl = Some(sslConfig))
          val builder = new NingAsyncHttpClientConfigBuilder(config)

          val actual = builder.build()
          val sslEngine = actual.getSSLEngineFactory.newSSLEngine()
          val protocols = sslEngine.getEnabledProtocols

          protocols must beEqualTo(Array("TLSv1"))
        }

        "use an allow all hostname verifier" in {
          val config = defaultConfig.copy(acceptAnyCertificate = Some(true))
          val builder = new NingAsyncHttpClientConfigBuilder(config)

          val actual = builder.build()
          val hostnameVerifier = actual.getHostnameVerifier
          hostnameVerifier must beAnInstanceOf[AllowAllHostnameVerifier]
        }

        "use the loose trust manager" in {
          val config = defaultConfig.copy(acceptAnyCertificate = Some(true))
          val builder = new NingAsyncHttpClientConfigBuilder(config)

          val actual = builder.build()

          // The trust manager is private to SSLContext, so let's just hack into it for the purposes of testing...
          val sslContext = actual.getSSLContext
          val sslContextClass = sslContext.getClass
          val contextSpiField = sslContextClass.getDeclaredField("contextSpi")
          contextSpiField.setAccessible(true)
          val sslContextImpl = contextSpiField.get(sslContext)

          val trustManagerField = sslContextImpl.getClass.getDeclaredField("trustManager")
          trustManagerField.setAccessible(true)
          val trustManager = trustManagerField.get(sslContextImpl).asInstanceOf[X509TrustManager]

          trustManager must beAnInstanceOf[AcceptAnyCertificateTrustManager]
        }
      }
    }

    "with hostname verifier" should {
      "use the default hostname verifier" in {
        val sslConfig = DefaultSSLConfig(hostnameVerifierClass = None)
        val config = defaultConfig.copy(ssl = Some(sslConfig))
        val builder = new NingAsyncHttpClientConfigBuilder(config)

        val asyncConfig = builder.build()
        asyncConfig.getHostnameVerifier must beAnInstanceOf[DefaultHostnameVerifier]
      }

      "use an explicit hostname verifier" in {
        val sslConfig = DefaultSSLConfig(hostnameVerifierClass = Some(classOf[AllowAllHostnameVerifier].asInstanceOf[Class[HostnameVerifier]]))
        val config = defaultConfig.copy(ssl = Some(sslConfig))
        val builder = new NingAsyncHttpClientConfigBuilder(config)

        val asyncConfig = builder.build()
        asyncConfig.getHostnameVerifier must beAnInstanceOf[AllowAllHostnameVerifier]
      }

      "should disable the hostname verifier if loose.disableHostnameVerification is defined" in {
        val sslConfig = DefaultSSLConfig(loose = Some(DefaultSSLLooseConfig(disableHostnameVerification = Some(true))))
        val config = defaultConfig.copy(ssl = Some(sslConfig))
        val builder = new NingAsyncHttpClientConfigBuilder(config)

        val asyncConfig = builder.build()
        asyncConfig.getHostnameVerifier must beAnInstanceOf[AllowAllHostnameVerifier]
      }
    }

    "with protocols" should {

      "provide recommended protocols if not specified" in {
        val sslConfig = DefaultSSLConfig()
        val config = defaultConfig.copy(ssl = Some(sslConfig))
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        val existingProtocols = Array("TLSv1.2", "TLSv1.1", "TLSv1")

        val actual = builder.configureProtocols(existingProtocols, sslConfig)

        actual.toSeq must containTheSameElementsAs(Protocols.recommendedProtocols)
      }

      "provide explicit protocols if specified" in {
        val sslConfig = DefaultSSLConfig(enabledProtocols = Some(Seq("derp", "baz", "quux")))
        val config = defaultConfig.copy(ssl = Some(sslConfig))
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        val existingProtocols = Array("quux", "derp", "baz")

        val actual = builder.configureProtocols(existingProtocols, sslConfig)

        actual.toSeq must containTheSameElementsAs(Seq("derp", "baz", "quux"))
      }

      "throw exception on deprecated protocols from explicit list" in {
        val deprecatedProtocol = Protocols.deprecatedProtocols.head

        // the enabled protocol list has a deprecated protocol in it.
        val enabledProtocols = Array(deprecatedProtocol, "goodOne", "goodTwo")
        val sslConfig = DefaultSSLConfig(enabledProtocols = Some(enabledProtocols))
        val config = defaultConfig.copy(ssl = Some(sslConfig))

        val builder = new NingAsyncHttpClientConfigBuilder(config)

        // The existing protocols is larger than the enabled list, and out of order.
        val existingProtocols = Array("goodTwo", "badOne", "badTwo", deprecatedProtocol, "goodOne")

        builder.configureProtocols(existingProtocols, sslConfig).must(throwAn[IllegalStateException])
      }

      "not throw exception on deprecated protocols from list if allowWeakProtocols is enabled" in {
        val deprecatedProtocol = Protocols.deprecatedProtocols.head

        // the enabled protocol list has a deprecated protocol in it.
        val enabledProtocols = Array(deprecatedProtocol, "goodOne", "goodTwo")
        val sslConfig = DefaultSSLConfig(enabledProtocols = Some(enabledProtocols), loose = Some(DefaultSSLLooseConfig(allowWeakProtocols = Some(true))))
        val config = defaultConfig.copy(ssl = Some(sslConfig))

        val builder = new NingAsyncHttpClientConfigBuilder(config)

        // The existing protocols is larger than the enabled list, and out of order.
        val existingProtocols = Array("goodTwo", "badOne", "badTwo", deprecatedProtocol, "goodOne")

        val actual = builder.configureProtocols(existingProtocols, sslConfig)

        // We should only have the list in order, including the deprecated protocol.
        actual.toSeq must containTheSameElementsAs(Seq(deprecatedProtocol, "goodOne", "goodTwo"))
      }
    }

    "with ciphers" should {

      "provide explicit ciphers if specified" in {
        val enabledCiphers = Seq("goodone", "goodtwo")
        val sslConfig = DefaultSSLConfig(enabledCipherSuites = Some(enabledCiphers))
        val config = defaultConfig.copy(ssl = Some(sslConfig))
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        val existingCiphers = Array("goodone", "goodtwo", "goodthree")

        val actual = builder.configureCipherSuites(existingCiphers, sslConfig)

        actual.toSeq must containTheSameElementsAs(Seq("goodone", "goodtwo"))
      }

      "throw exception on deprecated ciphers from the explicit cipher list" in {
        val enabledCiphers = Seq(Ciphers.deprecatedCiphers.head, "goodone", "goodtwo")

        val sslConfig = DefaultSSLConfig(enabledCipherSuites = Some(enabledCiphers))
        val config = defaultConfig.copy(ssl = Some(sslConfig))
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        val existingCiphers = enabledCiphers.toArray

        builder.configureCipherSuites(existingCiphers, sslConfig).must(throwAn[IllegalStateException])
      }

      "not throw exception on deprecated ciphers if allowWeakCiphers is enabled" in {
        // User specifies list with deprecated ciphers...
        val enabledCiphers = Seq("badone", "goodone", "goodtwo")

        val sslConfig = DefaultSSLConfig(enabledCipherSuites = Some(enabledCiphers), loose = Some(DefaultSSLLooseConfig(allowWeakCiphers = Some(true))))
        val config = defaultConfig.copy(ssl = Some(sslConfig))
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        val existingCiphers = Array("badone", "goodone", "goodtwo")

        val actual = builder.configureCipherSuites(existingCiphers, sslConfig)

        actual.toSeq must containTheSameElementsAs(Seq("badone", "goodone", "goodtwo"))
      }

    }
  }
}
