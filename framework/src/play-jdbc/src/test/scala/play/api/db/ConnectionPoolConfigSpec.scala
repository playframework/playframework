/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import javax.inject.Inject
import play.api.test._

class ConnectionPoolConfigSpec extends PlaySpecification {

  "DBModule bindings" should {

    "use HikariCP when default pool is default" in new WithApplication(FakeApplication(
      additionalConfiguration = Map(
        "db.default.url" -> "jdbc:h2:mem:default",
        "db.other.driver" -> "org.h2.Driver",
        "db.other.url" -> "jdbc:h2:mem:other"
      )
    )) {
      val db = app.injector.instanceOf[DBApi]
      db.database("default").withConnection { c =>
        c.getClass.getName must contain("hikari")
      }
    }

    "use HikariCP when default pool is 'hikaricp'" in new WithApplication(FakeApplication(
      additionalConfiguration = Map(
        "play.db.pool" -> "hikaricp",
        "db.default.url" -> "jdbc:h2:mem:default",
        "db.other.driver" -> "org.h2.Driver",
        "db.other.url" -> "jdbc:h2:mem:other"
      )
    )) {
      val db = app.injector.instanceOf[DBApi]
      db.database("default").withConnection { c =>
        c.getClass.getName must contain("hikari")
      }
    }

    "use BoneCP when default pool is 'bonecp'" in new WithApplication(FakeApplication(
      additionalConfiguration = Map(
        "play.db.pool" -> "bonecp",
        "db.default.url" -> "jdbc:h2:mem:default",
        "db.other.driver" -> "org.h2.Driver",
        "db.other.url" -> "jdbc:h2:mem:other"
      )
    )) {
      val db = app.injector.instanceOf[DBApi]
      db.database("default").withConnection { c =>
        c.getClass.getName must contain("bonecp")
      }
    }

    "use BoneCP when database-specific pool is 'bonecp'" in new WithApplication(FakeApplication(
      additionalConfiguration = Map(
        "db.default.pool" -> "bonecp",
        "db.default.url" -> "jdbc:h2:mem:default",
        "db.other.driver" -> "org.h2.Driver",
        "db.other.url" -> "jdbc:h2:mem:other"
      )
    )) {
      val db = app.injector.instanceOf[DBApi]
      db.database("default").withConnection { c =>
        c.getClass.getName must contain("bonecp")
      }
    }

  }

}