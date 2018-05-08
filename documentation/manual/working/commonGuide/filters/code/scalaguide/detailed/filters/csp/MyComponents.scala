/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.detailed.filters.csp

import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import play.filters.csp.CSPComponents

// #scala-csp-components
class MyComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents with CSPComponents {

  override def httpFilters: Seq[EssentialFilter] = super.httpFilters :+ cspFilter

  lazy val router = Router.empty
}
// #scala-csp-components