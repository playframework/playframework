/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
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

    "use BoneCP when default pool is 'bonecp'" in new WithApplication(_.configure(
      "play.db.pool" -> "bonecp",
      "db.default.url" -> "jdbc:h2:mem:default",
      "db.other.driver" -> "org.h2.Driver",
      "db.other.url" -> "jdbc:h2:mem:other"
    )) {
      val db = app.injector.instanceOf[DBApi]
      db.database("default").withConnection { c =>
        c.getClass.getName must contain("bonecp")
      }
    }

    "use BoneCP with 'bonecp' specific settings" in new WithApplication(_.configure(
      "play.db.pool" -> "bonecp",
      "db.default.url" -> "jdbc:h2:mem:default",
      "db.other.driver" -> "org.h2.Driver",
      "db.other.url" -> "jdbc:h2:mem:other",
      "play.db.prototype.bonecp.maxConnectionsPerPartition" -> "50"
    )) {
      import com.jolbox.bonecp.BoneCPDataSource
      val db = app.injector.instanceOf[DBApi]
      val bonecpDataSource: BoneCPDataSource = db.database("default").dataSource.asInstanceOf[BoneCPDataSource]
      val bonecpConfig = bonecpDataSource.getConfig
      bonecpConfig.getMaxConnectionsPerPartition must be_==(50)
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
