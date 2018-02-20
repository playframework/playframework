/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.csp

import java.util.Base64
import java.util.regex.{ Matcher, Pattern }

import javax.inject.{ Inject, Singleton }
import play.api.mvc.RequestHeader
import play.api.mvc.request.RequestAttrKey

/**
 * This trait processes a request header for CSP related logic.
 */
trait CSPProcessor {
  /**
   * Inspects the request header, and returns a CSPResult iff the
   * request should be subject to CSP processing.
   *
   * If the request header has a CSP Nonce already defined,
   * then the processor will carry the existing nonce through in the
   * result, otherwise a new nonce will be generated.
   *
   * @param requestHeader a request header
   * @return Some(CSPResult) if config.shouldProtect returns true, otherwise None
   */
  def process(requestHeader: RequestHeader): Option[CSPResult]
}

object CSPProcessor {
  def apply(config: CSPConfig = CSPConfig()): CSPProcessor = new DefaultCSPProcessor(config)
}

/**
 * The default CSP processor.  This handles processing of a CSP Nonce and hashes into
 * a Content-Security-Policy series of directives, based off the CSPConfig.
 *
 * If a request has the attribute RequestAttrKey.CSPNonce, then that nonce is used.
 * Otherwise, a nonce is generated from 16 bytes of SecureRandom.
 *
 * @param config the CSPConfig to use for processing rules.
 */
class DefaultCSPProcessor @Inject() (config: CSPConfig) extends CSPProcessor {

  protected val noncePattern: Pattern = Pattern.compile(config.nonce.pattern, Pattern.LITERAL)

  protected val hashPatterns: Seq[(Pattern, String)] = config.hashes.map { hashConfig =>
    Pattern.compile(hashConfig.pattern, Pattern.LITERAL) -> (hashConfig.algorithm + "-" + hashConfig.hash)
  }

  protected val cspLine: String = generateDirectives(config.directives)

  override def process(requestHeader: RequestHeader): Option[CSPResult] = {
    val nonce = if (config.nonce.enabled) {
      Some(generateContentSecurityPolicyNonce(Some(requestHeader)))
    } else {
      None
    }

    if (config.shouldProtect(requestHeader)) {
      Some(CSPResult(nonce, generateLine(nonce), config.reportOnly, config.nonce.header))
    } else {
      None
    }
  }

  protected def generateLine(nonce: Option[String]): String = {
    val cspLineWithNonce = nonce.map { n =>
      noncePattern.matcher(cspLine).replaceAll(Matcher.quoteReplacement(s"'nonce-$n'"))
    }.getOrElse(cspLine)

    hashPatterns.foldLeft(cspLineWithNonce)((line, pair) =>
      pair._1.matcher(line).replaceAll(Matcher.quoteReplacement(s"'${pair._2}'"))
    )
  }

  protected def generateDirectives(directives: CSPDirectivesConfig): String = {
    import directives._

    @inline def boolMap(flag: Boolean, name: String): Option[(String, String)] = {
      if (flag) Some(name -> "") else None
    }

    Seq(
      baseUri.map("base-uri" -> _),
      boolMap(blockAllMixedContent, "block-all-mixed-content"),
      childSrc.map("child-src" -> _),
      connectSrc.map("connect-src" -> _),
      defaultSrc.map("default-src" -> _),
      boolMap(disownOpener, "disown-opener"),
      formAction.map("form-action" -> _),
      fontSrc.map("font-src" -> _),
      frameAncestors.map("frame-ancestors" -> _),
      frameSrc.map("frame-src" -> _),
      imgSrc.map("img-src" -> _),
      manifestSrc.map("manifest-src" -> _),
      mediaSrc.map("media-src" -> _),
      objectSrc.map("object-src" -> _),
      pluginTypes.map("plugin-types" -> _),
      reportUri.map("report-uri" -> _),
      reportTo.map("report-to" -> _),
      requireSriFor.map("require-sri-for" -> _),
      sandbox.map("sandbox" -> _),
      scriptSrc.map("script-src" -> _),
      styleSrc.map("style-src" -> _),
      workerSrc.map("worker-src" -> _),
      boolMap(upgradeInsecureRequests, "upgrade-insecure-requests")
    ).flatten.map {
        case (k, v) if v.isEmpty =>
          k
        case (k, v) =>
          s"$k $v".trim
      }.mkString(" ; ").trim
  }

  protected def generateContentSecurityPolicyNonce(maybeRequest: Option[RequestHeader]): String = {
    maybeRequest.flatMap(_.attrs.get(RequestAttrKey.CSPNonce)).getOrElse {
      val random = new java.security.SecureRandom()
      val values = new Array[Byte](16)
      random.nextBytes(values)
      Base64.getMimeEncoder.encodeToString(values)
    }
  }
}

case class CSPResult(nonce: Option[String], directives: String, reportOnly: Boolean, nonceHeader: Boolean)
