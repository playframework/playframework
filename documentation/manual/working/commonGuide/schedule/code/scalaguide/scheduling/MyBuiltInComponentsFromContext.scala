/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

//###replace: package tasks
package scalaguide.scheduling

import play.api.ApplicationLoader.Context
import play.api.routing.Router
import play.api.{BuiltInComponentsFromContext, NoHttpFiltersComponents}

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
