package scalaguide.advanced.filters

// #simple-filter
import play.api.Logger
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object LoggingFilter extends Filter {
  def apply(nextFilter: (RequestHeader) => Future[Result])
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

object SomeScope{
// #concise-filter-syntax
val loggingFilter = Filter { (nextFilter, requestHeader) =>
  val startTime = System.currentTimeMillis
  nextFilter(requestHeader).map { result =>
    val endTime = System.currentTimeMillis
    val requestTime = endTime - startTime
    Logger.info(s"${requestHeader.method} ${requestHeader.uri} took ${requestTime}ms " +
      s"and returned ${result.header.status}")
    result.withHeaders("Request-Time" -> requestTime.toString)
  }
}
// #concise-filter-syntax
}

