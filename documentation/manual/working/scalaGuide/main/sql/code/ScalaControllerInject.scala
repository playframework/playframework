/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.sql

// #inject-controller
// ###insert: package controllers
import javax.inject.Inject

import play.api.db._
import play.api.mvc._

class ScalaControllerInject @Inject()(db: Database, val controllerComponents: ControllerComponents)
    extends BaseController {

  def index = Action {
    var outString = "Number is "
    val conn      = db.getConnection()

    try {
      val stmt = conn.createStatement
      val rs   = stmt.executeQuery("SELECT 9 as testkey ")

      while (rs.next()) {
        outString += rs.getString("testkey")
      }
    } finally {
      conn.close()
    }
    Ok(outString)
  }

}
// #inject-controller

class DatabaseOperations @Inject()(db: Database) {

  // #access-default-database
  // access "default" database
  db.withConnection { conn =>
    // do whatever you need with the connection
  }
  // #access-default-database

  // #access-db-connection
  db.withTransaction { conn =>
    // do whatever you need with the connection
  }
  // #access-db-connection
}
