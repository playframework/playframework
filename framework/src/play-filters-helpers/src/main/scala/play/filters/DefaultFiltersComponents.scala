package play.filters

import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.csrf.CSRFComponents
import play.filters.headers.SecurityHeadersComponents
import play.filters.hosts.AllowedHostsComponents

/**
 * A compile time default filters components.
 *
 * {{{
 * class MyComponents(context: ApplicationLoader.Context)
 *   extends BuiltInComponentsFromContext(context)
 *   with play.filters.DefaultFiltersComponents {
 *
 * }
 * }}}
 */
trait DefaultFiltersComponents
    extends CSRFComponents
    with SecurityHeadersComponents
    with AllowedHostsComponents {

  lazy val defaultFilters: HttpFilters = {
    new HttpFilters {
      val filters: Seq[EssentialFilter] = Seq(csrfFilter, securityHeadersFilter, allowedHostsFilter)
    }
  }

}
