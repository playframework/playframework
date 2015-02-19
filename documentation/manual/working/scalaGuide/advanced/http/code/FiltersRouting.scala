package scalaguide.advanced.filters.routing

// #routing-info-access
import play.api.mvc.{Result, RequestHeader, Filter}
import play.api.{Logger, Routes}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object LoggingFilter extends Filter {
  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

    val startTime = System.currentTimeMillis

    nextFilter(requestHeader).map { result =>

      val action = requestHeader.tags(Routes.ROUTE_CONTROLLER) +
        "." + requestHeader.tags(Routes.ROUTE_ACTION_METHOD)
      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime

      Logger.info(s"${action} took ${requestTime}ms" +
        s" and returned ${result.header.status}")

      result.withHeaders("Request-Time" -> requestTime.toString)
    }
  }
}
// #routing-info-access