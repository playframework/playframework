/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csp

import akka.util.ByteString
import javax.inject.Inject
import play.api.libs.streams.Accumulator
import play.api.mvc.request.RequestAttrKey
import play.api.mvc.{ EssentialAction, RequestHeader, Result }

/**
 * A result processor that applies a CSPResult to a play request pipeline -- either an ActionBuilder or a Filter.
 */
trait CSPResultProcessor {
  def apply(next: EssentialAction, request: RequestHeader): Accumulator[ByteString, Result]
}

object CSPResultProcessor {
  def apply(processor: CSPProcessor): CSPResultProcessor = new DefaultCSPResultProcessor(processor)
}

/**
 * This trait is used by CSPActionBuilder and CSPFilter to apply the CSPResult to a
 * Play HTTP result as headers.
 *
 * Appends as `play.api.http.HeaderNames.CONTENT_SECURITY_POLICY` or
 * `play.api.http.HeaderNames.CONTENT_SECURITY_POLICY_REPORT_ONLY`,
 * depending on config.reportOnly.
 *
 * If `cspResult.nonceHeader` is defined then
 * `play.api.http.HeaderNames.X_CONTENT_SECURITY_POLICY_NONCE_HEADER``
 * is set as an additional header.
 */
class DefaultCSPResultProcessor @Inject() (cspProcessor: CSPProcessor)
  extends CSPResultProcessor {

  def apply(next: EssentialAction, request: RequestHeader): Accumulator[ByteString, Result] = {
    cspProcessor
      .process(request)
      .map { cspResult =>
        val maybeNonceRequest = cspResult.nonce
          .map { nonce =>
            request.addAttr(RequestAttrKey.CSPNonce, nonce)
          }
          .getOrElse(request)

        next(maybeNonceRequest).map { result =>
          result.withHeaders(generateHeaders(cspResult): _*)
        }(play.core.Execution.trampoline)
      }
      .getOrElse {
        next(request)
      }
  }

  protected def generateHeaders(cspResult: CSPResult): Seq[(String, String)] = {
    import play.api.http.HeaderNames._
    val headerName = if (cspResult.reportOnly) {
      CONTENT_SECURITY_POLICY_REPORT_ONLY
    } else {
      CONTENT_SECURITY_POLICY
    }
    var cspHeader = collection.immutable.Seq(headerName -> cspResult.directives)

    cspResult.nonce match {
      case Some(nonce) if cspResult.nonceHeader =>
        cspHeader :+ (X_CONTENT_SECURITY_POLICY_NONCE_HEADER -> nonce)
      case _ =>
        cspHeader
    }
  }

}
