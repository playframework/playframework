/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package sdatabase
//#scala-jdbc-database
import javax.inject.Inject
import play.api.db.Database

import scala.concurrent.Future

class ScalaApplicationDatabase @Inject() (db: Database, databaseExecutionContext: DatabaseExecutionContext) {
  def updateSomething(): Unit = {
    Future {
      db.withConnection { conn =>
        // do whatever you need with the db connection
      }
    }(databaseExecutionContext)
  }
}
//#scala-jdbc-database
