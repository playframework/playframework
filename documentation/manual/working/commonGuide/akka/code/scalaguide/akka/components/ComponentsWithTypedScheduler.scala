/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.components

//#scheduler-compile-time-injection
import play.api.libs.concurrent.AkkaTypedComponents
import play.api.routing.Router
import play.api.Application
import play.api.ApplicationLoader
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.filters.HttpFiltersComponents

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
