/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package sdatabase

//#scala-jdbc-connection

import javax.inject.Inject

import scala.concurrent.Future

import play.api.db.Database

class ScalaJdbcConnection @Inject() (db: Database, databaseExecutionContext: DatabaseExecutionContext) {
  def updateSomething(): Unit = {
    Future {
      // get jdbc connection
      val connection = db.getConnection()

      // do whatever you need with the db connection

      // remember to close the connection
      connection.close()
    }(databaseExecutionContext)
  }
}
//#scala-jdbc-connection
