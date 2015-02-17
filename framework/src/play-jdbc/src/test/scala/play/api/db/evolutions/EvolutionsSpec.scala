/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db.evolutions

import java.sql.{ SQLException, ResultSet }
import org.specs2.mutable.{ After, Specification }
import play.api.db.Database

// TODO: fuctional test with InvalidDatabaseRevision exception

object EvolutionsSpec extends Specification {

  sequential

  import TestEvolutions._

  "Evolutions" should {

    "apply up scripts" in new WithEvolutions {
      val scripts = evolutions.scripts(Seq(a1, a2, a3))

      scripts must have length (3)
      scripts must_== Seq(UpScript(a1), UpScript(a2), UpScript(a3))

      evolutions.evolve(scripts, autocommit = true)

      val resultSet = executeQuery("select * from test")
      resultSet.next must beTrue
      resultSet.getLong(1) must_== 1L
      resultSet.getString(2) must_== "alice"
      resultSet.getInt(3) must_== 42
      resultSet.next must beFalse
    }

    "apply down scripts" in new WithEvolutions {
      val original = evolutions.scripts(Seq(a1, a2, a3))
      evolutions.evolve(original, autocommit = true)

      val scripts = evolutions.scripts(Seq(b1, a2, b3))

      scripts must have length (6)
      scripts must_== Seq(DownScript(a3), DownScript(a2), DownScript(a1), UpScript(b1), UpScript(a2), UpScript(b3))

      evolutions.evolve(scripts, autocommit = true)

      val resultSet = executeQuery("select * from test")
      resultSet.next must beTrue
      resultSet.getLong(1) must_== 1L
      resultSet.getString(2) must_== "bob"
      resultSet.getInt(3) must_== 42
      resultSet.next must beFalse
    }

    "report inconsistent state and resolve" in new WithEvolutions {
      val broken = evolutions.scripts(Seq(c1, a2, a3))
      val fixed = evolutions.scripts(Seq(a1, a2, a3))

      evolutions.evolve(broken, autocommit = true) must throwAn[InconsistentDatabase]

      // inconsistent until resolved
      evolutions.evolve(fixed, autocommit = true) must throwAn[InconsistentDatabase]

      evolutions.resolve(1)

      evolutions.evolve(fixed, autocommit = true)
    }

    "reset the database" in new WithEvolutions {
      val scripts = evolutions.scripts(Seq(a1, a2, a3))
      evolutions.evolve(scripts, autocommit = true)
      // Check that there's data in the database
      val resultSet = executeQuery("select * from test")
      resultSet.next must beTrue
      resultSet.close()

      val resetScripts = evolutions.resetScripts()
      evolutions.evolve(resetScripts, autocommit = true)

      // Should be no table because all downs should have been executed
      executeQuery("select * from test") must throwA[SQLException]
    }

    "provide a helper for testing" in new WithEvolutions {
      Evolutions.withEvolutions(database, SimpleEvolutionsReader.forDefault(a1, a2, a3)) {
        // Check that there's data in the database
        val resultSet = executeQuery("select * from test")
        resultSet.next must beTrue
        resultSet.close()
      }

      // Check that cleanup was done afterwards
      executeQuery("select * from test") must throwA[SQLException]
    }

  }

  trait WithEvolutions extends After {
    lazy val database = Database.inMemory("default")

    lazy val evolutions = new DatabaseEvolutions(database)

    lazy val connection = database.getConnection()

    def executeQuery(sql: String): ResultSet = connection.createStatement.executeQuery(sql)

    def after = {
      connection.close()
      database.shutdown()
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
