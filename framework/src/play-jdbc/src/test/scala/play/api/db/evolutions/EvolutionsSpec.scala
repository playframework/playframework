/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db.evolutions

import java.sql.ResultSet
import org.specs2.mutable.{ After, Specification }
import play.api.Configuration
import play.api.db.DefaultDBApi

// TODO: fuctional test with InvalidDatabaseRevision exception

object EvolutionsSpec extends Specification {

  sequential

  import TestEvolutions._

  "Evolutions" should {

    "apply up scripts" in new WithEvolutions {
      val scripts = evolutions.scripts("default", Seq(a1, a2, a3))

      scripts must have length (3)
      scripts must_== Seq(UpScript(a1), UpScript(a2), UpScript(a3))

      evolutions.evolve("default", scripts, autocommit = true)

      val resultSet = executeQuery("select * from test")
      resultSet.next must beTrue
      resultSet.getLong(1) must_== 1L
      resultSet.getString(2) must_== "alice"
      resultSet.getInt(3) must_== 42
      resultSet.next must beFalse
    }

    "apply down scripts" in new WithEvolutions {
      val original = evolutions.scripts("default", Seq(a1, a2, a3))
      evolutions.evolve("default", original, autocommit = true)

      val scripts = evolutions.scripts("default", Seq(b1, a2, b3))

      scripts must have length (6)
      scripts must_== Seq(DownScript(a3), DownScript(a2), DownScript(a1), UpScript(b1), UpScript(a2), UpScript(b3))

      evolutions.evolve("default", scripts, autocommit = true)

      val resultSet = executeQuery("select * from test")
      resultSet.next must beTrue
      resultSet.getLong(1) must_== 1L
      resultSet.getString(2) must_== "bob"
      resultSet.getInt(3) must_== 42
      resultSet.next must beFalse
    }

    "report inconsistent state and resolve" in new WithEvolutions {
      val broken = evolutions.scripts("default", Seq(c1, a2, a3))
      val fixed = evolutions.scripts("default", Seq(a1, a2, a3))

      evolutions.evolve("default", broken, autocommit = true) must throwAn[InconsistentDatabase]

      // inconsistent until resolved
      evolutions.evolve("default", fixed, autocommit = true) must throwAn[InconsistentDatabase]

      evolutions.resolve("default", 1)

      evolutions.evolve("default", fixed, autocommit = true)
    }

  }

  trait WithEvolutions extends After {
    lazy val db = new DefaultDBApi(
      Configuration.from(Map(
        "default.driver" -> "org.h2.Driver",
        "default.url" -> "jdbc:h2:mem:evolutions-test"
      ))
    )

    lazy val evolutions = new DefaultEvolutionsApi(db)

    lazy val connection = db.database("default").getConnection()

    def executeQuery(sql: String): ResultSet = connection.createStatement.executeQuery(sql)

    def after = {
      connection.close()
      db.shutdown()
    }
  }

  object TestEvolutions {
    val a1 = Evolution(
      1,
      "create table test (id bigint not null, name varchar(255));",
      "drop table if exists test;"
    )

    val a2 = Evolution(
      2,
      "alter table test add (age int);",
      "alter table test drop if exists age;"
    )

    val a3 = Evolution(
      3,
      "insert into test (id, name, age) values (1, 'alice', 42);",
      "delete from test;"
    )

    val b1 = Evolution(
      1,
      "create table test (id bigint not null, content varchar(255));",
      "drop table if exists test;"
    )

    val b3 = Evolution(
      3,
      "insert into test (id, content, age) values (1, 'bob', 42);",
      "delete from test;"
    )

    val c1 = Evolution(
      1,
      "creaTYPOe table test (id bigint not null, name varchar(255));",
      "drop table if exists test;"
    )
  }
}
