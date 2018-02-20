/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.csp

import play.api.mvc.RequestHeader
import play.api.test.{ FakeRequest, PlaySpecification }
import com.shapesecurity.salvation._
import com.shapesecurity.salvation.data._
import java.util

import com.shapesecurity.salvation.directiveValues.HashSource.HashAlgorithm

import scala.collection.JavaConverters._

class CSPProcessorSpec extends PlaySpecification {

  "shouldProtect" should {

    "produce a result when shouldProtect is true" in {
      val shouldProtect: RequestHeader => Boolean = _ => true
      val config = CSPConfig(shouldProtect = shouldProtect)
      val processor = new DefaultCSPProcessor(config)
      val maybeResult = processor.process(FakeRequest())
      maybeResult must beSome
    }

    "not produce a result when shouldProtect is false" in {
      val shouldProtect: RequestHeader => Boolean = _ => false
      val config = CSPConfig(shouldProtect = shouldProtect)
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
      val directives: CSPDirectivesConfig = CSPDirectivesConfig(scriptSrc = Some(CPSNonceConfig.DEFAULT_CSP_NONCE_PATTERN))
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
      val directives = CSPDirectivesConfig(scriptSrc = Some("%CSP_MYSCRIPT_HASH%"))
      val processor = new DefaultCSPProcessor(CSPConfig(hashes = Seq(hashConfig), directives = directives))
      val Some(cspResult) = processor.process(FakeRequest())
      val (policy, notices) = parse(cspResult.directives)
      val base64Value = new Base64Value(hashConfig.hash)

      notices must beEmpty
      policy.hasSomeEffect must beTrue
      policy.allowsScriptWithHash(HashAlgorithm.SHA256, base64Value) must beTrue
    }

    "have effect with christmas tree directives" in {
      val directives = CSPDirectivesConfig(
        baseUri = Some("'none'"),
        blockAllMixedContent = false, // experimental
        childSrc = None, // deprecated in CSP3
        connectSrc = Some("'none'"),
        defaultSrc = Some("'none'"),
        disownOpener = false, // salvation does not know this
        fontSrc = Some("'none'"),
        formAction = Some("'none'"),
        frameAncestors = Some("'none'"),
        frameSrc = Some("'none'"),
        imgSrc = Some("'none'"),
        manifestSrc = None, // experimental
        mediaSrc = Some("'self' data:"),
        objectSrc = Some("'none'"),
        pluginTypes = Some("application/x-shockwave-flash"),
        reportUri = None, // gives info notice
        reportTo = None, // gives info notice
        requireSriFor = Some("script"),
        sandbox = Some("allow-forms"),
        scriptSrc = Some("'none'"),
        styleSrc = Some("'none'"),
        workerSrc = Some("'none'"),
        upgradeInsecureRequests = false // experimental
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
