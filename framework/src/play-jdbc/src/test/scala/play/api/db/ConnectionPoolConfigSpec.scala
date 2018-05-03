/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db

import play.api.test._

class ConnectionPoolConfigSpec extends PlaySpecification {

  "DBModule bindings" should {

    "use HikariCP when default pool is default" in new WithApplication(_.configure(
      "db.default.url" -> "jdbc:h2:mem:default",
      "db.other.driver" -> "org.h2.Driver",
      "db.other.url" -> "jdbc:h2:mem:other"
    )) {
      val db = app.injector.instanceOf[DBApi]
      db.database("default").withConnection { c =>
        c.getClass.getName must contain("hikari")
      }
    }

    "use HikariCP when default pool is 'hikaricp'" in new WithApplication(_.configure(
      "play.db.pool" -> "hikaricp",
      "db.default.url" -> "jdbc:h2:mem:default",
      "db.other.driver" -> "org.h2.Driver",
      "db.other.url" -> "jdbc:h2:mem:other"
    )) {
      val db = app.injector.instanceOf[DBApi]
      db.database("default").withConnection { c =>
        c.getClass.getName must contain("hikari")
      }
    }

    "do not use LogSqlDataSource by default" in new WithApplication(_.configure(
      "db.default.driver" -> "org.h2.Driver",
      "db.default.url" -> "jdbc:h2:mem:default"
    )) {
      val db = app.injector.instanceOf[DBApi]
      db.database("default").dataSource.getClass.getName must not contain ("LogSqlDataSource")
    }

    "use LogSqlDataSource when logSql is true" in new WithApplication(_.configure(
      "db.default.driver" -> "org.h2.Driver",
      "db.default.url" -> "jdbc:h2:mem:default",
      "db.default.logSql" -> "true"
    )) {
      val db = app.injector.instanceOf[DBApi]
      db.database("default").dataSource.getClass.getName must contain("LogSqlDataSource")
    }
  }

}
