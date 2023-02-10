/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

// ###replace: package tasks
package scalaguide.scheduling

import play.api.routing.Router
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.NoHttpFiltersComponents

class MyBuiltInComponentsFromContext(context: Context)
    extends BuiltInComponentsFromContext(context)
    with NoHttpFiltersComponents {
  override def router: Router = Router.empty

  // Task is initialize here
  initialize()

  private def initialize(): Unit = {
    new CodeBlockTask(actorSystem)
  }
}
