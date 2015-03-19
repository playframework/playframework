/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ning

import com.typesafe.config.ConfigFactory
import org.specs2.mutable._
import org.specs2.mock._
import org.specs2.time.NoTimeConversions
import play.api.Environment

import play.api.libs.ws.WSClientConfig
import play.api.libs.ws.ssl._

import javax.net.ssl.SSLContext
import com.ning.http.client.ProxyServerSelector
import com.ning.http.util.{ ProxyUtils, AllowAllHostnameVerifier }
import play.api.libs.ws.ssl.DefaultHostnameVerifier
import org.slf4j.LoggerFactory

import play.api.test.WithApplication

import scala.concurrent.duration._

object NingConfigSpec extends Specification with Mockito with NoTimeConversions {

  val defaultWsConfig = new WSClientConfig()
  val defaultConfig = new NingWSClientConfig(defaultWsConfig)

  "NingConfigSpec" should {

    def parseThis(input: String)(implicit app: play.api.Application) = {
      val config = play.api.Configuration(ConfigFactory.parseString(input)
        .withFallback(ConfigFactory.defaultReference()))
      val parser = new NingWSClientConfigParser(defaultWsConfig, config, app.injector.instanceOf[Environment])
      parser.parse()
    }

    "parse ws ning section" in new WithApplication {
      val actual = parseThis("""
                               |play.ws.ning.allowPoolingConnection = false
                               |play.ws.ning.allowSslConnectionPool = false
                               |play.ws.ning.ioThreadMultiplier = 5
                               |play.ws.ning.maxConnectionsPerHost = 3
                               |play.ws.ning.maxConnectionsTotal = 6
                               |play.ws.ning.maxNumberOfRedirects = 0
                               |play.ws.ning.maxRequestRetry = 99
                               |play.ws.ning.removeQueryParamsOnRedirect = true
                               |play.ws.ning.requestCompressionLevel = 999
                               |play.ws.ning.useRawUrl = true
                             """.stripMargin)

      actual.allowPoolingConnection must beFalse
      actual.allowSslConnectionPool must beFalse
      actual.ioThreadMultiplier must_== 5
      actual.maxConnectionsPerHost must_== 3
      actual.maxConnectionsTotal must_== 6
      actual.maxNumberOfRedirects must_== 0
      actual.maxRequestRetry must_== 99
      actual.removeQueryParamsOnRedirect must beTrue
      actual.useRawUrl must beTrue
    }

    "with basic options" should {

      "provide a basic default client with default settings" in {
        val config = defaultConfig
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        val actual = builder.build()

        actual.getIdleConnectionTimeoutInMs must_== defaultWsConfig.idleTimeout.toMillis
        actual.getRequestTimeoutInMs must_== defaultWsConfig.requestTimeout.toMillis
        actual.getConnectionTimeoutInMs must_== defaultWsConfig.connectionTimeout.toMillis
        actual.isRedirectEnabled must_== defaultWsConfig.followRedirects

        val sslEngine = actual.getSSLEngineFactory.newSSLEngine()
        sslEngine.getEnabledCipherSuites.toSeq must not contain Ciphers.deprecatedCiphers
        sslEngine.getEnabledProtocols.toSeq must not contain Protocols.deprecatedProtocols

        actual.getHostnameVerifier must beAnInstanceOf[DefaultHostnameVerifier]
      }

      "use an explicit idle timeout" in {
        val wsConfig = defaultWsConfig.copy(idleTimeout = 42.millis)
        val config = defaultConfig.copy(wsClientConfig = wsConfig)
        val builder = new NingAsyncHttpClientConfigBuilder(config)

        val actual = builder.build()
        actual.getIdleConnectionTimeoutInMs must_== 42L
      }

      "use an explicit request timeout" in {
        val wsConfig = defaultWsConfig.copy(requestTimeout = 47.millis)
        val config = defaultConfig.copy(wsClientConfig = wsConfig)
        val builder = new NingAsyncHttpClientConfigBuilder(config)

        val actual = builder.build()
        actual.getRequestTimeoutInMs must_== 47L
      }

      "use an explicit connection timeout" in {
        val wsConfig = defaultWsConfig.copy(connectionTimeout = 99.millis)
        val config = defaultConfig.copy(wsClientConfig = wsConfig)
        val builder = new NingAsyncHttpClientConfigBuilder(config)

        val actual = builder.build()
        actual.getConnectionTimeoutInMs must_== 99L
      }

      "use an explicit followRedirects option" in {
        val wsConfig = defaultWsConfig.copy(followRedirects = false)
        val config = defaultConfig.copy(wsClientConfig = wsConfig)
        val builder = new NingAsyncHttpClientConfigBuilder(config)

        val actual = builder.build()
        actual.isRedirectEnabled must_== false
      }

      "use an explicit proxy if useProxyProperties is true and there are system defined proxy settings" in {
        val wsConfig = defaultWsConfig.copy(useProxyProperties = true)
        val config = defaultConfig.copy(wsClientConfig = wsConfig)

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

    "with ning options" should {

      "allow setting ning allowPoolingConnection" in {
        val config = defaultConfig.copy(allowPoolingConnection = false)
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        val actual = builder.build()
        actual.getAllowPoolingConnection must_== false
      }

      "allow setting ning allowSslConnectionPool" in {
        val config = defaultConfig.copy(allowSslConnectionPool = false)
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        val actual = builder.build()
        actual.isSslConnectionPoolEnabled must_== false
      }

      "allow setting ning ioThreadMultiplier" in {
        val config = defaultConfig.copy(ioThreadMultiplier = 5)
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        val actual = builder.build()
        actual.getIoThreadMultiplier must_== 5
      }

      "allow setting ning maximumConnectionsPerHost" in {
        val config = defaultConfig.copy(maxConnectionsPerHost = 3)
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        val actual = builder.build()
        actual.getMaxConnectionPerHost must_== 3
      }

      "allow setting ning maximumConnectionsTotal" in {
        val config = defaultConfig.copy(maxConnectionsPerHost = 6)
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        val actual = builder.build()
        actual.getMaxConnectionPerHost must_== 6
      }

      "allow setting ning maximumConnectionsTotal" in {
        val config = defaultConfig.copy(maxNumberOfRedirects = 0)
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        val actual = builder.build()
        actual.getMaxRedirects must_== 0
      }

      "allow setting ning maxRequestRetry" in {
        val config = defaultConfig.copy(maxRequestRetry = 99)
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        val actual = builder.build()
        actual.getMaxRequestRetry must_== 99
      }

      "allow setting ning removeQueryParamsOnRedirect" in {
        val config = defaultConfig.copy(removeQueryParamsOnRedirect = true)
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        val actual = builder.build()
        actual.isRemoveQueryParamOnRedirect must_== true
      }

      "allow setting ning useRawUrl" in {
        val config = defaultConfig.copy(useRawUrl = true)
        val builder = new NingAsyncHttpClientConfigBuilder(config)
        val actual = builder.build()
        actual.isUseRawUrl must_== true
      }
    }

    "with SSL options" should {

      // The ConfigSSLContextBuilder does most of the work here, but there are a couple of things outside of the
      // SSL context proper...

      "with context" should {

        "use the configured trustmanager and keymanager if context not passed in" in {
          // Stick a spy into the SSL config so we can verify that things get called on it that would
          // only be called if it was using the config trust manager...

          val sslConfig = spy(SSLConfig(default = false))
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new NingAsyncHttpClientConfigBuilder(config)

          org.mockito.Mockito.doReturn("TLS").when(sslConfig).protocol

          val asyncClientConfig = builder.build()

          // Check that the spy got called...
          org.mockito.Mockito.verify(sslConfig)

          // ...and return a result so specs2 is happy.
          asyncClientConfig.getSSLContext must not beNull
        }

        "use the default with a current certificate" in {
          val tmc = TrustManagerConfig()
          val wsConfig = defaultWsConfig.copy(ssl = SSLConfig(default = true, trustManagerConfig = tmc))
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new NingAsyncHttpClientConfigBuilder(config)

          val asyncClientConfig = builder.build()
          val sslContext = asyncClientConfig.getSSLContext
          sslContext must beEqualTo(SSLContext.getDefault)
        }

        "log a warning if sslConfig.default is passed in with an weak certificate" in {
          import ch.qos.logback.classic.spi._
          import ch.qos.logback.classic._

          // Pass in a configuration which is guaranteed to fail, by banning RSA, DSA and EC certificates
          val wsConfig = defaultWsConfig.copy(ssl = SSLConfig(default = true, disabledKeyAlgorithms = Seq("RSA", "DSA", "EC")))
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
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

      "with hostname verifier" should {
        "use the default hostname verifier" in {
          val sslConfig = SSLConfig()
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new NingAsyncHttpClientConfigBuilder(config)

          val asyncConfig = builder.build()
          asyncConfig.getHostnameVerifier must beAnInstanceOf[DefaultHostnameVerifier]
        }

        "use an explicit hostname verifier" in {
          val sslConfig = SSLConfig(hostnameVerifierClass = classOf[AllowAllHostnameVerifier])
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new NingAsyncHttpClientConfigBuilder(config)

          val asyncConfig = builder.build()
          asyncConfig.getHostnameVerifier must beAnInstanceOf[AllowAllHostnameVerifier]
        }

        "should disable the hostname verifier if loose.disableHostnameVerification is defined" in {
          val sslConfig = SSLConfig(loose = SSLLooseConfig(disableHostnameVerification = true))
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new NingAsyncHttpClientConfigBuilder(config)

          val asyncConfig = builder.build()
          asyncConfig.getHostnameVerifier must beAnInstanceOf[AllowAllHostnameVerifier]
        }
      }

      "with protocols" should {

        "provide recommended protocols if not specified" in {
          val sslConfig = SSLConfig()
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new NingAsyncHttpClientConfigBuilder(config)
          val existingProtocols = Array("TLSv1.2", "TLSv1.1", "TLSv1")

          val actual = builder.configureProtocols(existingProtocols, sslConfig)

          actual.toSeq must containTheSameElementsAs(Protocols.recommendedProtocols)
        }

        "provide explicit protocols if specified" in {
          val sslConfig = SSLConfig(enabledProtocols = Some(Seq("derp", "baz", "quux")))
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new NingAsyncHttpClientConfigBuilder(config)
          val existingProtocols = Array("quux", "derp", "baz")

          val actual = builder.configureProtocols(existingProtocols, sslConfig)

          actual.toSeq must containTheSameElementsAs(Seq("derp", "baz", "quux"))
        }

        "throw exception on deprecated protocols from explicit list" in {
          val deprecatedProtocol = Protocols.deprecatedProtocols.head

          // the enabled protocol list has a deprecated protocol in it.
          val enabledProtocols = Array(deprecatedProtocol, "goodOne", "goodTwo")
          val sslConfig = SSLConfig(enabledProtocols = Some(enabledProtocols))
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)

          val builder = new NingAsyncHttpClientConfigBuilder(config)

          // The existing protocols is larger than the enabled list, and out of order.
          val existingProtocols = Array("goodTwo", "badOne", "badTwo", deprecatedProtocol, "goodOne")

          builder.configureProtocols(existingProtocols, sslConfig).must(throwAn[IllegalStateException])
        }

        "not throw exception on deprecated protocols from list if allowWeakProtocols is enabled" in {
          val deprecatedProtocol = Protocols.deprecatedProtocols.head

          // the enabled protocol list has a deprecated protocol in it.
          val enabledProtocols = Array(deprecatedProtocol, "goodOne", "goodTwo")
          val sslConfig = SSLConfig(enabledProtocols = Some(enabledProtocols), loose = SSLLooseConfig(allowWeakProtocols = true))
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)

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
          val sslConfig = SSLConfig(enabledCipherSuites = Some(enabledCiphers))
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new NingAsyncHttpClientConfigBuilder(config)
          val existingCiphers = Array("goodone", "goodtwo", "goodthree")

          val actual = builder.configureCipherSuites(existingCiphers, sslConfig)

          actual.toSeq must containTheSameElementsAs(Seq("goodone", "goodtwo"))
        }

        "throw exception on deprecated ciphers from the explicit cipher list" in {
          val enabledCiphers = Seq(Ciphers.deprecatedCiphers.head, "goodone", "goodtwo")

          val sslConfig = SSLConfig(enabledCipherSuites = Some(enabledCiphers))
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new NingAsyncHttpClientConfigBuilder(config)
          val existingCiphers = enabledCiphers.toArray

          builder.configureCipherSuites(existingCiphers, sslConfig).must(throwAn[IllegalStateException])
        }

        "not throw exception on deprecated ciphers if allowWeakCiphers is enabled" in {
          // User specifies list with deprecated ciphers...
          val enabledCiphers = Seq("badone", "goodone", "goodtwo")

          val sslConfig = SSLConfig(enabledCipherSuites = Some(enabledCiphers), loose = SSLLooseConfig(allowWeakCiphers = true))
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new NingAsyncHttpClientConfigBuilder(config)
          val existingCiphers = Array("badone", "goodone", "goodtwo")

          val actual = builder.configureCipherSuites(existingCiphers, sslConfig)

          actual.toSeq must containTheSameElementsAs(Seq("badone", "goodone", "goodtwo"))
        }

      }
    }
  }
}
