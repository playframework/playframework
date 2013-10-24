package scalaguide.advanced.filters


// #filter-trait-example
import play.api.mvc._
import play.filters.gzip.GzipFilter

object Global extends WithFilters(LoggingFilter, new GzipFilter()) {
  // ...
}
// #filter-trait-example

object DifferentNamespace{

// #filter-method-example
import play.api.mvc._
import play.filters.gzip.GzipFilter
import play.api.GlobalSettings

object Global extends GlobalSettings {
  override def doFilter(next: EssentialAction): EssentialAction = {
    Filters(super.doFilter(next), LoggingFilter, new GzipFilter())
  }
}
// #filter-method-example
}