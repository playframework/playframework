/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.sql

// #named-database
// ###insert: package controllers
import javax.inject.Inject

import play.api.mvc.{BaseController, ControllerComponents}
import play.api.db.{Database, NamedDatabase}

// inject "orders" database instead of "default"
class ScalaInjectNamed @Inject()(
  @NamedDatabase("orders") db: Database,
  val controllerComponents: ControllerComponents
) extends BaseController {
  // do whatever you need with the db
}
// #named-database
