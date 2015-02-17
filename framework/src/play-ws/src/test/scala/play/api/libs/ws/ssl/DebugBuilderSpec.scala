/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._

object DebugBuilderSpec extends Specification {

  "JavaSecurityDebugBuilder" should {

    "match nothing by default" in {
      val config = SSLDebugConfig(certpath = false)
      val builder = new JavaSecurityDebugBuilder(config)
      val actual = builder.build()

      actual.trim must beEmpty
    }

    "match certpath" in {
      val config = SSLDebugConfig(certpath = true)
      val builder = new JavaSecurityDebugBuilder(config)
      val actual = builder.build()

      actual.trim.split("\\s+").toSeq must containTheSameElementsAs(Seq("certpath"))
    }

    "match certpath + ocsp" in {
      val config = SSLDebugConfig(certpath = true, ocsp = true)
      val builder = new JavaSecurityDebugBuilder(config)
      val actual = builder.build()

      actual.trim.split("\\s+").toSeq must containTheSameElementsAs(Seq("certpath", "ocsp"))
    }
  }

  "JavaxNetDebugBuilder" should {
    "match nothing by default" in {
      val config = SSLDebugConfig()
      val builder = new JavaxNetDebugBuilder(config)
      val actual = builder.build()

      actual.trim must beEmpty
    }

    "match all" in {
      val config = SSLDebugConfig(all = true)
      val builder = new JavaxNetDebugBuilder(config)
      val actual = builder.build()

      actual.trim.split("\\s+").toSeq must containTheSameElementsAs(Seq("all"))
    }

    "match some random combinations" in {
      val config = SSLDebugConfig(ssl = true, defaultctx = true, handshake = Some(SSLDebugHandshakeOptions(data = true)))
      val builder = new JavaxNetDebugBuilder(config)
      val actual: String = builder.build()

      actual.trim.split("\\s+").toSeq must containTheSameElementsAs(Seq("ssl", "defaultctx", "handshake", "data"))
    }

  }
}
