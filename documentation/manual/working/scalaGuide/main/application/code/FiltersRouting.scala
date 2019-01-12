/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.advanced.filters.routing

// #routing-info-access
import javax.inject.Inject
import akka.stream.Materializer
import play.api.mvc.{Result, RequestHeader, Filter}
import play.api.Logging
import play.api.routing.{HandlerDef, Router}
import scala.concurrent.{Future, ExecutionContext}

class LoggingFilter @Inject() (implicit val mat: Materializer, ec: ExecutionContext) extends Filter with Logging {
  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

    val startTime = System.currentTimeMillis

    nextFilter(requestHeader).map { result =>
      val handlerDef: HandlerDef = requestHeader.attrs(Router.Attrs.HandlerDef)
      val action = handlerDef.controller + "." + handlerDef.method
      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime

      logger.info(s"${action} took ${requestTime}ms and returned ${result.header.status}")

      result.withHeaders("Request-Time" -> requestTime.toString)
    }
  }
}
// #routing-info-access
