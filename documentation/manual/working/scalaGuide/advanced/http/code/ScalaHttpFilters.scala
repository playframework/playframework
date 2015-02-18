package scalaguide.advanced.filters

package simple {

// #simple-filter
import play.api.Logger
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class LoggingFilter extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

    val startTime = System.currentTimeMillis

    nextFilter(requestHeader).map { result =>

      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime

      Logger.info(s"${requestHeader.method} ${requestHeader.uri} " +
        s"took ${requestTime}ms and returned ${result.header.status}")

      result.withHeaders("Request-Time" -> requestTime.toString)
    }
  }
}
// #simple-filter

}

package httpfilters {

import simple.LoggingFilter

// #filters
import javax.inject.Inject
import play.api.http.HttpFilters
import play.filters.gzip.GzipFilter

class Filters @Inject() (
  gzip: GzipFilter,
  log: LoggingFilter
) extends HttpFilters {

  val filters = Seq(gzip, log)
}
//#filters
}
