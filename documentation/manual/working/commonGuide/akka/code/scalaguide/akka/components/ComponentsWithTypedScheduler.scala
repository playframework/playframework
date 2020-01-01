/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.components

//#scheduler-compile-time-injection
import play.api.Application
import play.api.ApplicationLoader
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext

import play.api.routing.Router
import play.filters.HttpFiltersComponents

import play.api.libs.concurrent.AkkaTypedComponents

class MyApplicationLoaderUsingTypedScheduler extends ApplicationLoader {
  override def load(context: Context): Application = {
    new ComponentsWithTypedScheduler(context).application
  }
}

class ComponentsWithTypedScheduler(context: Context)
    extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with AkkaTypedComponents {
  override lazy val router: Router = Router.empty
}
//#scheduler-compile-time-injection
