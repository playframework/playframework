/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
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
   * @return Some(CSPResult) if the processor is enabled for this request, otherwise None
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

    if (config.shouldFilterRequest(requestHeader)) {
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

  protected def generateDirectives(directives: Seq[CSPDirective]): String = {
    directives.map(d => s"${d.name} ${d.value}").mkString("; ")
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
