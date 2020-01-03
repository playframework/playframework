/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package sdatabase

//#scala-jdbc-connection

import javax.inject.Inject
import play.api.db.Database

import scala.concurrent.Future

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
