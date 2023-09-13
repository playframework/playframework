/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import javax.inject._

import play.api._
import play.api.db.Database
import play.api.mvc._

@Singleton
class HomeController @Inject() (val controllerComponents: ControllerComponents, val database: Database)
    extends BaseController {

  def index() = Action { implicit request: Request[AnyContent] =>
    val value = database.withConnection(connection => {
      val rs = connection
        .prepareStatement("select * from table1")
        .executeQuery()
      rs.next()
      rs.getString(1)
    })
    Ok(value)
  }
}
