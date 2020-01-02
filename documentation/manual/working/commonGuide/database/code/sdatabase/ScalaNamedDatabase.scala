/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package sdatabase
//#scala-jdbc-named-database
import javax.inject.Inject
import play.api.db.Database
import play.db.NamedDatabase

import scala.concurrent.Future

class ScalaNamedDatabase @Inject() (
    @NamedDatabase("orders") ordersDatabase: Database,
    databaseExecutionContext: DatabaseExecutionContext
) {
  def updateSomething(): Unit = {
    Future {
      ordersDatabase.withConnection { conn =>
        // do whatever you need with the db connection
      }
    }(databaseExecutionContext)
  }
}
//#scala-jdbc-named-database
