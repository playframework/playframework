/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._

import com.typesafe.config.ConfigFactory
import play.api.test.WithApplication
import javax.net.ssl.SSLSession

object DefaultSSLConfigParserSpec extends Specification {

  // We can get horrible concurrent modification exceptions in the logger if we run
  // several WithApplication at the same time.  Usually happens in the build.
  sequential

  "SSLConfigParser" should {

    def parseThis(input: String)(implicit app: play.api.Application) = {
      val config = play.api.Configuration(ConfigFactory.parseString(input))
      val parser = new DefaultSSLConfigParser(config, app.classloader)
      parser.parse()
    }

    "parse ws.ssl base section" in new WithApplication() {
      val actual = parseThis("""
                                |default = true
                                |protocol = TLSv1.2
                                |checkRevocation = true
                                |revocationLists = [ "http://example.com" ]
                                |hostnameVerifierClass = "com.ning.http.util.AllowAllHostnameVerifier"
                                |enabledCipherSuites = [ TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA ]
                                |enabledProtocols = [ TLSv1.2, TLSv1.1, TLS ]
                                |disabledSignatureAlgorithms = "md2, md4, md5"
                                |disabledKeyAlgorithms = "RSA keySize < 1024"
                              """.stripMargin)

      actual.default must beSome.which(_ must beTrue)
      actual.protocol must beSome.which(_ must beEqualTo("TLSv1.2"))
      actual.checkRevocation must beSome.which(_ must beTrue)
      actual.revocationLists must beSome.which {
        _ must beEqualTo(Seq(new java.net.URL("http://example.com")))
      }
      actual.hostnameVerifierClass must beSome.which(_ must_== classOf[com.ning.http.util.AllowAllHostnameVerifier])
      actual.enabledCipherSuites must beSome.which(_ must containTheSameElementsAs(Seq("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA")))
      actual.enabledProtocols must beSome.which(_ must containTheSameElementsAs(Seq("TLSv1.2", "TLSv1.1", "TLS")))
      actual.disabledSignatureAlgorithms must beSome.which(_ must beEqualTo("md2, md4, md5"))
      actual.disabledKeyAlgorithms must beSome.which(_ must beEqualTo("RSA keySize < 1024"))
      actual.secureRandom must beNone
    }

    "parse ws.ssl base section with defaults" in new WithApplication() {
      val actual = parseThis("")

      actual.default must beNone
      actual.protocol must beNone
      actual.checkRevocation must beNone
      actual.revocationLists must beNone
      actual.hostnameVerifierClass must beNone
      actual.enabledCipherSuites must beNone
      actual.enabledProtocols must beNone
      actual.disabledSignatureAlgorithms must beNone
      actual.disabledKeyAlgorithms must beNone
      actual.secureRandom must beNone
    }

    "parse ws.ssl.loose section" in new WithApplication() {
      val actual = parseThis("""
                                |loose = {
                                | allowLegacyHelloMessages = true
                                | allowUnsafeRenegotiation = true
                                | allowWeakCiphers = true
                                | allowWeakProtocols = true
                                | disableHostnameVerification = true
                                |}
                              """.stripMargin)
      actual.loose must beSome.which { loose =>
        loose.allowLegacyHelloMessages must beSome(true)
        loose.allowUnsafeRenegotiation must beSome(true)
        loose.allowWeakCiphers must beSome(true)
        loose.allowWeakProtocols must beSome(true)
        loose.disableHostnameVerification must beSome(true)
      }
    }

    "parse ws.ssl.debug section" in new WithApplication() {
      val actual = parseThis("""
                                |debug = [
                                |"certpath",
                                |"ssl",
                                |"defaultctx",
                                |"handshake",
                                |  "verbose",
                                |  "data",
                                |"keygen",
                                |"keymanager",
                                |"pluggability",
                                |"record",
                                |  "packet",
                                |  "plaintext",
                                |"session",
                                |"sessioncache",
                                |"sslctx",
                                |"trustmanager"
                                |]
                              """.stripMargin)

      actual.debug must beSome.which { debug =>
        debug.certpath must beTrue

        debug.all must beFalse
        debug.ssl must beTrue

        debug.defaultctx must beTrue
        debug.handshake must beSome.which { handshake =>
          handshake.data must beTrue
          handshake.verbose must beTrue
        }
        debug.keygen must beTrue
        debug.keymanager must beTrue
        debug.pluggability must beTrue
        debug.record must beSome.which { record =>
          record.packet must beTrue
          record.plaintext must beTrue
        }
        debug.session must beTrue
        debug.sessioncache must beTrue
        debug.sslctx must beTrue
        debug.trustmanager must beTrue
      }
    }

    "parse ws.ssl.debug section with all" in new WithApplication() {
      val actual = parseThis("""
                                |debug = [
                                |"certpath",
                                |"all"
                                |]
                              """.stripMargin)

      actual.debug must beSome.which { debug =>
        debug.certpath must beTrue

        // everything else is false, all wins everything.
        debug.all must beTrue
      }
    }

    "parse ws.ssl.debug section with ssl" in new WithApplication() {
      val actual = parseThis("""
                                |debug = [
                                |"ssl"
                                |]
                              """.stripMargin)

      actual.debug must beSome.which { debug =>
        debug.ssl must beTrue
      }
    }

    "parse ws.ssl.trustBuilder section" in new WithApplication() {
      val info = parseThis("""
                              |trustManager = {
                              |  algorithm = "trustme"
                              |  stores = [
                              |    { type: "storeType", path: "trusted" }
                              |  ]
                              |}
                            """.stripMargin)

      info.trustManagerConfig must beSome.which {
        tmc =>
          tmc.algorithm must beSome.which {
            _ must be_==("trustme")
          }
          val tsi = tmc.trustStoreConfigs(0)
          tsi.filePath must beSome.which(_ must beEqualTo("trusted"))
          tsi.storeType must beSome.which {
            _ must beEqualTo("storeType")
          }
      }
    }

    "parse ws.ssl.keyManager section" in new WithApplication() {
      val info = parseThis("""
                              |keyManager = {
                              |  password = "changeit"
                              |  algorithm = "keyStore"
                              |  stores = [
                              |    {
                              |      type: "storeType",
                              |      path: "cacerts",
                              |      password: "password1"
                              |    },
                              |    { type: "PEM", data = "data",  password: "changeit" }
                              |  ]
                              |}
                            """.stripMargin)

      info.keyManagerConfig must beSome.which {
        kmc =>
          kmc.algorithm must beSome.which {
            _ must beEqualTo("keyStore")
          }
          kmc.keyStoreConfigs.size must beEqualTo(2)
          val fileStoreConfig = kmc.keyStoreConfigs(0)
          fileStoreConfig.filePath must beSome.which(_ must beEqualTo("cacerts"))
          fileStoreConfig.storeType must beSome.which {
            _ must beEqualTo("storeType")
          }
          fileStoreConfig.password must beSome.which {
            _ must beEqualTo("password1")
          }
          val stringStoreConfig = kmc.keyStoreConfigs(1)
          stringStoreConfig.data must beSome.which(_ must beEqualTo("data"))
      }
    }

    "fail on ws.ssl.keyManager with no path defined" in new WithApplication() {
      parseThis("""
                   |keyManager = {
                   |  algorithm = "keyStore"
                   |  stores = [
                   |    { type: "storeType", password: "password1" }
                   |  ]
                   |}
                 """.stripMargin).must(throwAn[IllegalStateException])
    }

  }

}
