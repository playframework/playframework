/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.detailed.filters.csp

import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.filters.csp.CSPComponents
import play.filters.HttpFiltersComponents

// #scala-csp-components
class MyComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with CSPComponents {
  override def httpFilters: Seq[EssentialFilter] = super.httpFilters :+ cspFilter

  lazy val router = Router.empty
}
// #scala-csp-components
