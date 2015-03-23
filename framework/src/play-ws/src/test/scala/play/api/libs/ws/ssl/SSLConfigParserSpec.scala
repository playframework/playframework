/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._

import com.typesafe.config.ConfigFactory
import play.api.PlayConfig
import play.api.test.WithApplication

object SSLConfigParserSpec extends Specification {

  // We can get horrible concurrent modification exceptions in the logger if we run
  // several WithApplication at the same time.  Usually happens in the build.
  sequential

  "SSLConfigParser" should {

    def parseThis(input: String)(implicit app: play.api.Application) = {
      val config = ConfigFactory.parseString(input).withFallback(ConfigFactory.defaultReference().getConfig("play.ws.ssl"))
      val parser = new SSLConfigParser(PlayConfig(config), app.classloader)
      parser.parse()
    }

    "parse ws.ssl base section" in new WithApplication() {
      val actual = parseThis("""
                                |default = true
                                |protocol = TLSv1.1
                                |checkRevocation = true
                                |revocationLists = [ "http://example.com" ]
                                |hostnameVerifierClass = "com.ning.http.util.DefaultHostnameVerifier"
                                |enabledCipherSuites = [ TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA ]
                                |enabledProtocols = [ TLSv1.2, TLSv1.1, SSLv3 ]
                                |disabledSignatureAlgorithms = [md2, md3]
                                |disabledKeyAlgorithms = ["RSA keySize < 1024"]
                              """.stripMargin)

      actual.default must beTrue
      actual.protocol must_== "TLSv1.1"
      actual.checkRevocation must beSome(true)
      actual.revocationLists must beSome.which {
        _ must beEqualTo(Seq(new java.net.URL("http://example.com")))
      }
      actual.hostnameVerifierClass must_== classOf[com.ning.http.util.DefaultHostnameVerifier]
      actual.enabledCipherSuites must beSome.which(_ must containTheSameElementsAs(Seq("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA")))
      actual.enabledProtocols must beSome.which(_ must containTheSameElementsAs(Seq("TLSv1.2", "TLSv1.1", "SSLv3")))
      actual.disabledSignatureAlgorithms must containTheSameElementsAs(Seq("md2", "md3"))
      actual.disabledKeyAlgorithms must containTheSameElementsAs(Seq("RSA keySize < 1024"))
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
                                | acceptAnyCertificate = true
                                |}
                              """.stripMargin)
      actual.loose.allowLegacyHelloMessages must beSome(true)
      actual.loose.allowUnsafeRenegotiation must beSome(true)
      actual.loose.allowWeakCiphers must beTrue
      actual.loose.allowWeakProtocols must beTrue
      actual.loose.disableHostnameVerification must beTrue
      actual.loose.acceptAnyCertificate must beTrue
    }

    "say debug is disabled if all debug is disabled" in new WithApplication() {
      parseThis("").debug.enabled must beFalse
    }

    "parse ws.ssl.debug section" in new WithApplication() {
      val actual = parseThis("""
                                |debug = {
                                |certpath = true
                                |ssl = true
                                |defaultctx = true
                                |handshake = true
                                |  verbose = true
                                |  data = true
                                |keygen = true
                                |keymanager = true
                                |pluggability = true
                                |record = true
                                |  packet = true
                                |  plaintext = true
                                |session = true
                                |sessioncache = true
                                |sslctx = true
                                |trustmanager = true
                                |}
                              """.stripMargin)

      actual.debug.enabled must beTrue

      actual.debug.certpath must beTrue

      actual.debug.all must beFalse
      actual.debug.ssl must beTrue

      actual.debug.defaultctx must beTrue
      actual.debug.handshake must beSome.which { handshake =>
        handshake.data must beTrue
        handshake.verbose must beTrue
      }
      actual.debug.keygen must beTrue
      actual.debug.keymanager must beTrue
      actual.debug.pluggability must beTrue
      actual.debug.record must beSome.which { record =>
        record.packet must beTrue
        record.plaintext must beTrue
      }
      actual.debug.session must beTrue
      actual.debug.sessioncache must beTrue
      actual.debug.sslctx must beTrue
      actual.debug.trustmanager must beTrue
    }

    "parse ws.ssl.debug section with all" in new WithApplication() {
      val actual = parseThis("""
                                |debug = {
                                |certpath = true
                                |all = true
                                |}
                              """.stripMargin)

      actual.debug.enabled must beTrue

      actual.debug.certpath must beTrue

      // everything else is false, all wins everything.
      actual.debug.all must beTrue
    }

    "parse ws.ssl.debug section with ssl" in new WithApplication() {
      val actual = parseThis("""
                                |debug = {
                                |ssl = true
                                |}
                              """.stripMargin)
      actual.debug.enabled must beTrue
      actual.debug.ssl must beTrue
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

      val tmc = info.trustManagerConfig
      tmc.algorithm must_== "trustme"
      val tsi = tmc.trustStoreConfigs(0)
      tsi.filePath must beSome.which(_ must beEqualTo("trusted"))
      tsi.storeType must_== "storeType"
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

      val kmc = info.keyManagerConfig
      kmc.algorithm must_== "keyStore"
      kmc.keyStoreConfigs.size must_== 2
      val fileStoreConfig = kmc.keyStoreConfigs(0)
      fileStoreConfig.filePath must beSome.which(_ must beEqualTo("cacerts"))
      fileStoreConfig.storeType must_== "storeType"
      fileStoreConfig.password must beSome.which {
        _ must beEqualTo("password1")
      }
      val stringStoreConfig = kmc.keyStoreConfigs(1)
      stringStoreConfig.data must beSome.which(_ must beEqualTo("data"))
    }

    "fail on ws.ssl.keyManager with no path defined" in new WithApplication() {
      parseThis("""
                   |keyManager = {
                   |  algorithm = "keyStore"
                   |  stores = [
                   |    { type: "storeType", password: "password1" }
                   |  ]
                   |}
                 """.stripMargin).must(throwAn[AssertionError])
    }

  }

}
