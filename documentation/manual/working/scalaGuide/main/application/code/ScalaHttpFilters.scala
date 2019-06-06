/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.advanced.filters

package simple {

// #simple-filter
  import javax.inject.Inject
  import akka.stream.Materializer
  import play.api.Logger
  import play.api.mvc._
  import scala.concurrent.ExecutionContext
  import scala.concurrent.Future

  class LoggingFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

    def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {

      val startTime = System.currentTimeMillis

      nextFilter(requestHeader).map { result =>
        val endTime     = System.currentTimeMillis
        val requestTime = endTime - startTime

        Logger.info(
          s"${requestHeader.method} ${requestHeader.uri} took ${requestTime}ms and returned ${result.header.status}"
        )

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
  import play.api.http.DefaultHttpFilters
  import play.api.http.EnabledFilters
  import play.filters.gzip.GzipFilter

  class Filters @Inject()(
      defaultFilters: EnabledFilters,
      gzip: GzipFilter,
      log: LoggingFilter
  ) extends DefaultHttpFilters(defaultFilters.filters :+ gzip :+ log: _*)
//#filters

  object router {
    class Routes extends play.api.routing.Router {
      def routes                     = ???
      def documentation              = ???
      def withPrefix(prefix: String) = ???
    }
  }

//#components-filters

  import play.api._
  import play.filters.gzip._
  import play.filters.HttpFiltersComponents
  import router.Routes

  class MyComponents(context: ApplicationLoader.Context)
      extends BuiltInComponentsFromContext(context)
      with HttpFiltersComponents
      with GzipFilterComponents {

    // implicit executionContext and materializer are defined in BuiltInComponents
    lazy val loggingFilter: LoggingFilter = new LoggingFilter()

    // gzipFilter is defined in GzipFilterComponents
    override lazy val httpFilters = Seq(gzipFilter, loggingFilter)

    lazy val router = new Routes( /* ... */ )
  }

//#components-filters

}
