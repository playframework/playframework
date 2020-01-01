/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.advanced.filters.routing

// #routing-info-access
import javax.inject.Inject
import akka.stream.Materializer
import play.api.mvc.Result
import play.api.mvc.RequestHeader
import play.api.mvc.Filter
import play.api.Logging
import play.api.routing.HandlerDef
import play.api.routing.Router
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class LoggingFilter @Inject() (implicit val mat: Materializer, ec: ExecutionContext) extends Filter with Logging {
  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    val startTime = System.currentTimeMillis

    nextFilter(requestHeader).map { result =>
      val handlerDef: HandlerDef = requestHeader.attrs(Router.Attrs.HandlerDef)
      val action                 = handlerDef.controller + "." + handlerDef.method
      val endTime                = System.currentTimeMillis
      val requestTime            = endTime - startTime

      logger.info(s"${action} took ${requestTime}ms and returned ${result.header.status}")

      result.withHeaders("Request-Time" -> requestTime.toString)
    }
  }
}
// #routing-info-access
