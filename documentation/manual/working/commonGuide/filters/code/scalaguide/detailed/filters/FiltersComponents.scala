/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.detailed.filters

// #appending-filters-compile-time-di
import akka.util.ByteString
import play.api.ApplicationLoader
import play.api.BuiltInComponentsFromContext
import play.api.NoHttpFiltersComponents
import play.api.libs.streams.Accumulator
import play.api.mvc.EssentialAction
import play.api.mvc.EssentialFilter
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.routing.Router
import play.filters.csrf.CSRFFilter

// ###replace: class MyAppComponents(context: ApplicationLoader.Context)
class AddHttpFiltersComponents(context: ApplicationLoader.Context)
    extends BuiltInComponentsFromContext(context)
    with play.filters.HttpFiltersComponents {

  lazy val loggingFilter = new LoggingFilter()

  override def httpFilters: Seq[EssentialFilter] = {
    super.httpFilters :+ loggingFilter
  }

  override def router: Router = Router.empty // implement the router as needed
}
// #appending-filters-compile-time-di

// #removing-filters-compile-time-di
// ###replace: class MyAppComponents(context: ApplicationLoader.Context)
class RemoveHttpFilterComponents(context: ApplicationLoader.Context)
    extends BuiltInComponentsFromContext(context)
    with play.filters.HttpFiltersComponents {

  override def httpFilters: Seq[EssentialFilter] = {
    super.httpFilters.filterNot(_.getClass == classOf[CSRFFilter])
  }

  override def router: Router = Router.empty // implement the router as needed
}
// #removing-filters-compile-time-di

// #remove-all-filters-compile-time-di
// ###replace: class MyAppComponents(context: ApplicationLoader.Context)
class RemoveAllHttpFiltersComponents(context: ApplicationLoader.Context)
    extends BuiltInComponentsFromContext(context)
    with NoHttpFiltersComponents {

  override def router: Router = Router.empty // implement the router as needed

}
// #remove-all-filters-compile-time-di

class LoggingFilter extends EssentialFilter {
  override def apply(next: EssentialAction): EssentialAction = new EssentialAction {
    override def apply(request: RequestHeader): Accumulator[ByteString, Result] = next(request)
  }
}
