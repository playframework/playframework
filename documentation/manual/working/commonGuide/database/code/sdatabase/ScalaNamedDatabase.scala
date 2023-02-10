/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package sdatabase
//#scala-jdbc-named-database
import javax.inject.Inject

import scala.concurrent.Future

import play.api.db.Database
import play.db.NamedDatabase

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
