/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csp

import play.api.mvc.RequestHeader
import play.api.test.{ FakeRequest, PlaySpecification }
import com.shapesecurity.salvation._
import com.shapesecurity.salvation.data._
import java.util

import com.shapesecurity.salvation.directiveValues.HashSource.HashAlgorithm
import com.shapesecurity.salvation.directives.{ DirectiveValue, UpgradeInsecureRequestsDirective }

import scala.collection.JavaConverters._

class CSPProcessorSpec extends PlaySpecification {

  "shouldFilterRequest" should {

    "produce a result when shouldFilterRequest is true" in {
      val shouldFilterRequest: RequestHeader => Boolean = _ => true
      val config = CSPConfig(shouldFilterRequest = shouldFilterRequest)
      val processor = new DefaultCSPProcessor(config)
      val maybeResult = processor.process(FakeRequest())
      maybeResult must beSome
    }

    "not produce a result when shouldFilterRequest is false" in {
      val shouldFilterRequest: RequestHeader => Boolean = _ => false
      val config = CSPConfig(shouldFilterRequest = shouldFilterRequest)
      val processor = new DefaultCSPProcessor(config)
      val maybeResult = processor.process(FakeRequest())
      maybeResult must beNone
    }

  }

  "CSP directives" should {

    "have no effect with a default CSPConfig" in {
      val processor = new DefaultCSPProcessor(CSPConfig())
      val cspResult = processor.process(FakeRequest()).get
      val nonce = cspResult.nonce.get
      val (policy, notices) = parse(cspResult.directives)

      notices must beEmpty
      policy.hasSomeEffect must beFalse
    }

    "have no effect with reportOnly" in {
      val processor = new DefaultCSPProcessor(CSPConfig(reportOnly = true))
      val cspResult = processor.process(FakeRequest()).get
      val nonce = cspResult.nonce.get
      val (policy, notices) = parse(cspResult.directives)

      notices must beEmpty
      policy.hasSomeEffect must beFalse
    }

    "have effect with a nonce" in {
      val directives: Seq[CSPDirective] = Seq(CSPDirective("script-src", CPSNonceConfig.DEFAULT_CSP_NONCE_PATTERN))
      val processor: CSPProcessor = new DefaultCSPProcessor(CSPConfig(directives = directives))
      val cspResult = processor.process(FakeRequest()).get
      val nonce = cspResult.nonce.get
      val (policy, notices) = parse(cspResult.directives)

      notices must beEmpty
      policy.hasSomeEffect must beTrue
      policy.allowsScriptWithNonce(nonce) must beTrue
    }

    "have effect with a hash" in {
      val hashConfig = CSPHashConfig("sha256", "RpniQm4B6bHP0cNtv7w1p6pVcgpm5B/eu1DNEYyMFXc=", "%CSP_MYSCRIPT_HASH%")
      val directives = Seq(CSPDirective("script-src", "%CSP_MYSCRIPT_HASH%"))
      val processor = new DefaultCSPProcessor(CSPConfig(hashes = Seq(hashConfig), directives = directives))
      val Some(cspResult) = processor.process(FakeRequest())
      val (policy, notices) = parse(cspResult.directives)
      val base64Value = new Base64Value(hashConfig.hash)

      notices must beEmpty
      policy.hasSomeEffect must beTrue
      policy.allowsScriptWithHash(HashAlgorithm.SHA256, base64Value) must beTrue
    }

    "have effect using directives with no value" in {
      val directives = Seq(
        CSPDirective("upgrade-insecure-requests", "")
      )
      val processor = new DefaultCSPProcessor(CSPConfig(directives = directives))
      val Some(cspResult) = processor.process(FakeRequest())
      val (policy, notices) = parse(cspResult.directives)

      val directive = policy.getDirectiveByType[DirectiveValue, UpgradeInsecureRequestsDirective](classOf[UpgradeInsecureRequestsDirective])
      directive must not beNull
    }

    "have effect with christmas tree directives" in {
      val directives = Seq(
        CSPDirective("base-uri", "'none'"),
        CSPDirective("connect-src", "'none'"),
        CSPDirective("default-src", "'none'"),
        CSPDirective("font-src", "'none'"),
        CSPDirective("form-action", "'none'"),
        CSPDirective("frame-ancestors", "'none'"),
        CSPDirective("frame-src", "'none'"),
        CSPDirective("img-src", "'none'"),
        CSPDirective("media-src", "'self' data:"),
        CSPDirective("object-src", "'none'"),
        CSPDirective("plugin-types", "application/x-shockwave-flash"),
        CSPDirective("require-sri-for", "script"),
        CSPDirective("sandbox", "allow-forms"),
        CSPDirective("script-src", "'none'"),
        CSPDirective("style-src", "'none'"),
        CSPDirective("worker-src", "'none'")
      )
      val processor = new DefaultCSPProcessor(CSPConfig(directives = directives))
      val Some(cspResult) = processor.process(FakeRequest())
      val (policy, notices) = parse(cspResult.directives)

      // We're more interested in parsing successfully than in the actual effect here
      notices must beEmpty
      policy.hasSomeEffect must beTrue
    }
  }

  def parse(policyText: String): (Policy, Seq[Notice]) = {
    val notices = new util.ArrayList[Notice]
    val origin = URI.parse("http://example.com")
    val policy = Parser.parse(policyText, origin, notices)
    (policy, notices.asScala)
  }

}
