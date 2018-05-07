/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters

import play.api.mvc.EssentialFilter
import play.filters.csp.CSPComponents
import play.filters.csrf.CSRFComponents
import play.filters.headers.SecurityHeadersComponents
import play.filters.hosts.AllowedHostsComponents

/**
 * A compile time default filters components.
 *
 * {{{
 * class MyComponents(context: ApplicationLoader.Context)
 *   extends BuiltInComponentsFromContext(context)
 *   with play.filters.HttpFiltersComponents {
 *
 * }
 * }}}
 */
trait HttpFiltersComponents
  extends CSRFComponents
  with SecurityHeadersComponents
  with AllowedHostsComponents {

  def httpFilters: Seq[EssentialFilter] = Seq(csrfFilter, securityHeadersFilter, allowedHostsFilter)
}
