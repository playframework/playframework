/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.advanced.filters.routing

// #routing-info-access
import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.apache.pekko.stream.Materializer
import play.api.mvc.Filter
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.routing.HandlerDef
import play.api.routing.Router
import play.api.Logging

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
