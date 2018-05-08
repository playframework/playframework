/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csp

import akka.stream.Materializer
import akka.util.ByteString
import javax.inject._
import play.api.libs.streams.Accumulator
import play.api.mvc._

/**
 * This filter enables the Content-Security-Policy header in Play for all requests.
 *
 * Please see [[https://www.playframework.com/documentation/latest/CspFilter the documentation]] for more information.
 *
 * @param cspResultProcessor the underlying CSP processing logic.
 */
@Singleton
class CSPFilter @Inject() (cspResultProcessor: CSPResultProcessor) extends EssentialFilter {
  override def apply(next: EssentialAction): EssentialAction = new EssentialAction {
    override def apply(request: RequestHeader): Accumulator[ByteString, Result] = {
      cspResultProcessor(next, request)
    }
  }
}

object CSPFilter {
  def apply(cspResultProcessor: CSPResultProcessor)(implicit mat: Materializer): CSPFilter = new CSPFilter(cspResultProcessor)
}
