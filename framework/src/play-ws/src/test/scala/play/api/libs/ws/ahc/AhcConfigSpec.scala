/*
 *
 *  * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 *
 */
package play.api.libs.ws.ahc

import com.typesafe.config.ConfigFactory
import org.specs2.mutable._
import org.specs2.mock._
import play.api.Environment

import play.api.libs.ws.WSClientConfig
import play.api.libs.ws.ssl._

import javax.net.ssl.{ SSLSession, SSLContext }
import org.asynchttpclient.proxy.ProxyServerSelector
import org.asynchttpclient.util.ProxyUtils
import org.slf4j.LoggerFactory

import play.api.test.WithApplication

import scala.concurrent.duration._

object AhcConfigSpec extends Specification with Mockito {

  val defaultWsConfig = new WSClientConfig()
  val defaultConfig = new AhcWSClientConfig(defaultWsConfig)

  "AhcConfigSpec" should {

    def parseThis(input: String)(implicit app: play.api.Application) = {
      val config = play.api.Configuration(ConfigFactory.parseString(input)
        .withFallback(ConfigFactory.defaultReference()))
      val parser = new AhcWSClientConfigParser(defaultWsConfig, config, app.injector.instanceOf[Environment])
      parser.parse()
    }

    "case class defaults must match reference.conf defaults" in new WithApplication {
      AhcWSClientConfig() must_== parseThis("")
    }

    "parse ws ahc section" in new WithApplication {
      val actual = parseThis("""
                               |play.ws.ahc.maxConnectionsPerHost = 3
                               |play.ws.ahc.maxConnectionsTotal = 6
                               |play.ws.ahc.maxConnectionLifetime = 1 minute
                               |play.ws.ahc.idleConnectionInPoolTimeout = 30 seconds
                               |play.ws.ahc.maxNumberOfRedirects = 0
                               |play.ws.ahc.maxRequestRetry = 99
                               |play.ws.ahc.disableUrlEncoding = true
                               |play.ws.ahc.keepAlive = false
                             """.stripMargin)

      actual.maxConnectionsPerHost must_== 3
      actual.maxConnectionsTotal must_== 6
      actual.maxConnectionLifetime must_== 1.minute
      actual.idleConnectionInPoolTimeout must_== 30.seconds
      actual.maxNumberOfRedirects must_== 0
      actual.maxRequestRetry must_== 99
      actual.disableUrlEncoding must beTrue
      actual.keepAlive must beFalse
    }

    "with keepAlive" should {
      "parse keepAlive default as true" in new WithApplication {
        val actual = parseThis("""""".stripMargin)

        actual.keepAlive must beTrue
      }

      "throw exception on play.ws.ning.allowPoolingConnection" in new WithApplication {
        {
          parseThis("""
                      |play.ws.ning.allowPoolingConnection = false
                    """.stripMargin)
        }.must(throwAn[play.api.PlayException])
      }

      "throw exception on play.ws.ning.allowSslConnectionPool" in new WithApplication {
        {
          parseThis("""
                      |play.ws.ning.allowSslConnectionPool = false
                    """.stripMargin)
        }.must(throwAn[play.api.PlayException])
      }
    }

    "with basic options" should {

      "provide a basic default client with default settings" in {
        val config = defaultConfig
        val builder = new AhcConfigBuilder(config)
        val actual = builder.build()

        actual.getReadTimeout must_== defaultWsConfig.idleTimeout.toMillis
        actual.getRequestTimeout must_== defaultWsConfig.requestTimeout.toMillis
        actual.getConnectTimeout must_== defaultWsConfig.connectionTimeout.toMillis
        actual.isFollowRedirect must_== defaultWsConfig.followRedirects

        actual.getEnabledCipherSuites.toSeq must not contain Ciphers.deprecatedCiphers
        actual.getEnabledProtocols.toSeq must not contain Protocols.deprecatedProtocols
      }

      "use an explicit idle timeout" in {
        val wsConfig = defaultWsConfig.copy(idleTimeout = 42.millis)
        val config = defaultConfig.copy(wsClientConfig = wsConfig)
        val builder = new AhcConfigBuilder(config)

        val actual = builder.build()
        actual.getReadTimeout must_== 42L
      }

      "use an explicit request timeout" in {
        val wsConfig = defaultWsConfig.copy(requestTimeout = 47.millis)
        val config = defaultConfig.copy(wsClientConfig = wsConfig)
        val builder = new AhcConfigBuilder(config)

        val actual = builder.build()
        actual.getRequestTimeout must_== 47L
      }

      "use an explicit connection timeout" in {
        val wsConfig = defaultWsConfig.copy(connectionTimeout = 99.millis)
        val config = defaultConfig.copy(wsClientConfig = wsConfig)
        val builder = new AhcConfigBuilder(config)

        val actual = builder.build()
        actual.getConnectTimeout must_== 99L
      }

      "use an explicit followRedirects option" in {
        val wsConfig = defaultWsConfig.copy(followRedirects = true)
        val config = defaultConfig.copy(wsClientConfig = wsConfig)
        val builder = new AhcConfigBuilder(config)

        val actual = builder.build()
        actual.isFollowRedirect must_== true
      }

      "use an explicit proxy if useProxyProperties is true and there are system defined proxy settings" in {
        val wsConfig = defaultWsConfig.copy(useProxyProperties = true)
        val config = defaultConfig.copy(wsClientConfig = wsConfig)

        // Used in ProxyUtils.createProxyServerSelector
        try {
          System.setProperty(ProxyUtils.PROXY_HOST, "localhost")
          val builder = new AhcConfigBuilder(config)
          val actual = builder.build()

          val proxyServerSelector = actual.getProxyServerSelector

          proxyServerSelector must not(beNull)

          proxyServerSelector must not be_== ProxyServerSelector.NO_PROXY_SELECTOR
        } finally {
          // Unset http.proxyHost
          System.clearProperty(ProxyUtils.PROXY_HOST)
        }
      }
    }

    "with ahc options" should {

      "allow setting ahc keepAlive" in {
        val config = defaultConfig.copy(keepAlive = false)
        val builder = new AhcConfigBuilder(config)
        val actual = builder.build()
        actual.isKeepAlive must_== false
      }

      "allow setting ahc maximumConnectionsPerHost" in {
        val config = defaultConfig.copy(maxConnectionsPerHost = 3)
        val builder = new AhcConfigBuilder(config)
        val actual = builder.build()
        actual.getMaxConnectionsPerHost must_== 3
      }

      "allow setting ahc maximumConnectionsTotal" in {
        val config = defaultConfig.copy(maxConnectionsTotal = 6)
        val builder = new AhcConfigBuilder(config)
        val actual = builder.build()
        actual.getMaxConnections must_== 6
      }

      "allow setting ahc maximumConnectionsTotal" in {
        val config = defaultConfig.copy(maxNumberOfRedirects = 0)
        val builder = new AhcConfigBuilder(config)
        val actual = builder.build()
        actual.getMaxRedirects must_== 0
      }

      "allow setting ahc maxRequestRetry" in {
        val config = defaultConfig.copy(maxRequestRetry = 99)
        val builder = new AhcConfigBuilder(config)
        val actual = builder.build()
        actual.getMaxRequestRetry must_== 99
      }

      "allow setting ahc disableUrlEncoding" in {
        val config = defaultConfig.copy(disableUrlEncoding = true)
        val builder = new AhcConfigBuilder(config)
        val actual = builder.build()
        actual.isDisableUrlEncodingForBoundRequests must_== true
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
          val builder = new AhcConfigBuilder(config)

          org.mockito.Mockito.doReturn("TLS").when(sslConfig).protocol

          val asyncClientConfig = builder.build()

          // Check that the spy got called...
          org.mockito.Mockito.verify(sslConfig)

          // ...and return a result so specs2 is happy.
          asyncClientConfig.getSslEngineFactory must not(beNull)
        }

        "use the default with a current certificate" in {
          // You can't get the value of SSLContext out from the JSSE SSL engine factory, so
          // checking SSLContext.getDefault from a default = true is hard.
          // Unless we can mock this, it doesn't seem like it's easy to unit test.
          pending("AHC 2.0 does not provide a reference to a configured SSLContext")

          //val tmc = TrustManagerConfig()
          //val wsConfig = defaultWsConfig.copy(ssl = SSLConfig(default = true, trustManagerConfig = tmc))
          //val config = defaultConfig.copy(wsClientConfig = wsConfig)
          //val builder = new AhcConfigBuilder(config)
          //
          //val asyncClientConfig = builder.build()
          //val sslEngineFactory = asyncClientConfig.getSslEngineFactory
        }

        "log a warning if sslConfig.default is passed in with an weak certificate" in {
          import ch.qos.logback.classic.spi._
          import ch.qos.logback.classic._

          // Pass in a configuration which is guaranteed to fail, by banning RSA, DSA and EC certificates
          val wsConfig = defaultWsConfig.copy(ssl = SSLConfig(default = true, disabledKeyAlgorithms = Seq("RSA", "DSA", "EC")))
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new AhcConfigBuilder(config)
          // this only works with test:test, has a different type in test:testQuick and test:testOnly!
          val logger = builder.logger.asInstanceOf[ch.qos.logback.classic.Logger]
          val appender = new ch.qos.logback.core.read.ListAppender[ILoggingEvent]()
          val lc = LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext]
          appender.setContext(lc)
          appender.start()
          logger.addAppender(appender)
          logger.setLevel(Level.WARN)

          builder.build()

          val warahcs = appender.list
          warahcs.size must beGreaterThan(0)
        }

        "should validate certificates" in {
          val sslConfig = SSLConfig()
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new AhcConfigBuilder(config)

          val asyncConfig = builder.build()
          asyncConfig.isAcceptAnyCertificate must beFalse
        }

        "should disable the hostname verifier if loose.acceptAnyCertificate is enabled" in {
          val sslConfig = SSLConfig(loose = SSLLooseConfig(acceptAnyCertificate = true))
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new AhcConfigBuilder(config)

          val asyncConfig = builder.build()
          asyncConfig.isAcceptAnyCertificate must beTrue
        }
      }

      "with protocols" should {

        "provide recommended protocols if not specified" in {
          val sslConfig = SSLConfig()
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new AhcConfigBuilder(config)
          val existingProtocols = Array("TLSv1.2", "TLSv1.1", "TLSv1")

          val actual = builder.configureProtocols(existingProtocols, sslConfig)

          actual.toSeq must containTheSameElementsAs(Protocols.recommendedProtocols)
        }

        "provide explicit protocols if specified" in {
          val sslConfig = SSLConfig(enabledProtocols = Some(Seq("derp", "baz", "quux")))
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new AhcConfigBuilder(config)
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

          val builder = new AhcConfigBuilder(config)

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

          val builder = new AhcConfigBuilder(config)

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
          val builder = new AhcConfigBuilder(config)
          val existingCiphers = Array("goodone", "goodtwo", "goodthree")

          val actual = builder.configureCipherSuites(existingCiphers, sslConfig)

          actual.toSeq must containTheSameElementsAs(Seq("goodone", "goodtwo"))
        }

        "throw exception on deprecated ciphers from the explicit cipher list" in {
          val enabledCiphers = Seq(Ciphers.deprecatedCiphers.head, "goodone", "goodtwo")

          val sslConfig = SSLConfig(enabledCipherSuites = Some(enabledCiphers))
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new AhcConfigBuilder(config)
          val existingCiphers = enabledCiphers.toArray

          builder.configureCipherSuites(existingCiphers, sslConfig).must(throwAn[IllegalStateException])
        }

        "not throw exception on deprecated ciphers if allowWeakCiphers is enabled" in {
          // User specifies list with deprecated ciphers...
          val enabledCiphers = Seq("badone", "goodone", "goodtwo")

          val sslConfig = SSLConfig(enabledCipherSuites = Some(enabledCiphers), loose = SSLLooseConfig(allowWeakCiphers = true))
          val wsConfig = defaultWsConfig.copy(ssl = sslConfig)
          val config = defaultConfig.copy(wsClientConfig = wsConfig)
          val builder = new AhcConfigBuilder(config)
          val existingCiphers = Array("badone", "goodone", "goodtwo")

          val actual = builder.configureCipherSuites(existingCiphers, sslConfig)

          actual.toSeq must containTheSameElementsAs(Seq("badone", "goodone", "goodtwo"))
        }

      }
    }
  }
}
