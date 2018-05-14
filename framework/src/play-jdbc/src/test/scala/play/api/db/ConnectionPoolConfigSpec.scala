/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db

import javax.inject._
import play.api.Environment
import play.api.test._

class ConnectionPoolConfigSpec extends PlaySpecification {

  "DBModule bindings" should {

    "use HikariCP as default pool" in new WithApplication(_.configure(
      "db.default.url" -> "jdbc:h2:mem:default",
      "db.other.driver" -> "org.h2.Driver",
      "db.other.url" -> "jdbc:h2:mem:other"
    )) {
      val db = app.injector.instanceOf[DBApi].database("default")
      db must beLike {
        case pdb: PooledDatabase => pdb.pool must haveClass[HikariCPConnectionPool]
      }
    }

    "use HikariCP when default pool set to 'hikaricp'" in new WithApplication(_.configure(
      "play.db.pool" -> "hikaricp",
      "db.default.url" -> "jdbc:h2:mem:default",
      "db.other.driver" -> "org.h2.Driver",
      "db.other.url" -> "jdbc:h2:mem:other"
    )) {
      val db = app.injector.instanceOf[DBApi].database("default")
      db must beLike {
        case pdb: PooledDatabase => pdb.pool must haveClass[HikariCPConnectionPool]
      }
    }

    "use custom class when default pool set to class name" in new WithApplication(_.configure(
      "play.db.pool" -> classOf[CustomConnectionPool].getName,
      "db.default.url" -> "jdbc:h2:mem:default",
      "db.other.driver" -> "org.h2.Driver",
      "db.other.url" -> "jdbc:h2:mem:other"
    )) {
      val db = app.injector.instanceOf[DBApi].database("default")
      db must beLike {
        case pdb: PooledDatabase => pdb.pool must haveClass[CustomConnectionPool]
      }
    }

    "use custom class when database pool set to class name" in new WithApplication(_.configure(
      "db.default.pool" -> classOf[CustomConnectionPool].getName,
      "db.default.url" -> "jdbc:h2:mem:default",
      "db.other.driver" -> "org.h2.Driver",
      "db.other.url" -> "jdbc:h2:mem:other"
    )) {
      val db = app.injector.instanceOf[DBApi].database("default")
      db must beLike {
        case pdb: PooledDatabase => pdb.pool must haveClass[CustomConnectionPool]
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

@Singleton
class CustomConnectionPool @Inject() (environment: Environment) extends HikariCPConnectionPool(environment)