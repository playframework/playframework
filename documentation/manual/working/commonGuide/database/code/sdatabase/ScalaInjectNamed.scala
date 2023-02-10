/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package sdatabase

// #named-database
// ###insert: package controllers
import javax.inject.Inject

import play.api.db.Database
import play.api.db.NamedDatabase
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents

// inject "orders" database instead of "default"
class ScalaInjectNamed @Inject() (
    @NamedDatabase("orders") db: Database,
    val controllerComponents: ControllerComponents
) extends BaseController {
  // do whatever you need with the db
}
// #named-database
