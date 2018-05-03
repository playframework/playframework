/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db.evolutions

import java.sql.{ ResultSet, SQLException }

import org.specs2.mutable.{ After, Specification }
import play.api.db.{ Database, Databases }

// TODO: functional test with InvalidDatabaseRevision exception

class EvolutionsSpec extends Specification {

  sequential

  import TestEvolutions._

  "Evolutions" should {

    trait CreateSchema { this: WithEvolutions =>
      execute("create schema testschema")
    }

    trait UpScripts { this: WithEvolutions =>
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

    trait DownScripts { this: WithEvolutions =>
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

    trait ReportInconsistentStateAndResolve { this: WithEvolutions =>
      val broken = evolutions.scripts(Seq(c1, a2, a3))
      val fixed = evolutions.scripts(Seq(a1, a2, a3))

      evolutions.evolve(broken, autocommit = true) must throwAn[InconsistentDatabase]

      // inconsistent until resolved
      evolutions.evolve(fixed, autocommit = true) must throwAn[InconsistentDatabase]

      evolutions.resolve(1)

      evolutions.evolve(fixed, autocommit = true)
    }

    trait ResetDatabase { this: WithEvolutions =>
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

    trait ProvideHelperForTesting { this: WithEvolutions =>
      Evolutions.withEvolutions(database, SimpleEvolutionsReader.forDefault(a1, a2, a3)) {
        // Check that there's data in the database
        val resultSet = executeQuery("select * from test")
        resultSet.next must beTrue
        resultSet.close()
      }

      // Check that cleanup was done afterwards
      executeQuery("select * from test") must throwA[SQLException]
    }

    trait ProvideHelperForTestingSchema { this: WithEvolutions =>
      // Check if the play_evolutions table was created within the testschema
      val resultSet = executeQuery("select count(0) from testschema.play_evolutions")
      resultSet.next must beTrue
      resultSet.close()
    }

    "apply up scripts" in new UpScripts with WithEvolutions
    "apply up scripts derby" in new UpScripts with WithDerbyEvolutions

    "apply down scripts" in new DownScripts with WithEvolutions
    "apply down scripts derby" in new DownScripts with WithDerbyEvolutions

    "report inconsistent state and resolve" in new ReportInconsistentStateAndResolve with WithEvolutions
    "report inconsistent state and resolve derby" in new ReportInconsistentStateAndResolve with WithDerbyEvolutions

    "reset the database" in new ResetDatabase with WithEvolutions
    "reset the database derby" in new ResetDatabase with WithDerbyEvolutions

    "provide a helper for testing" in new ProvideHelperForTesting with WithEvolutions
    "provide a helper for testing derby" in new ProvideHelperForTesting with WithDerbyEvolutions

    // Test if the play_evolutions table gets created within a schema
    "create test schema derby" in new CreateSchema with WithDerbyEvolutionsSchema
    "reset the database to trigger creation of the play_evolutions table in the testschema derby" in new ResetDatabase with WithDerbyEvolutionsSchema
    "provide a helper for testing derby schema" in new ProvideHelperForTestingSchema with WithDerbyEvolutionsSchema
  }

  trait WithEvolutions extends After {
    lazy val database = Databases.inMemory("default")

    lazy val evolutions = new DatabaseEvolutions(database)

    lazy val connection = database.getConnection()

    def executeQuery(sql: String): ResultSet = connection.createStatement.executeQuery(sql)

    def execute(sql: String): Boolean = connection.createStatement.execute(sql)

    def after = {
      connection.close()
      database.shutdown()
    }
  }

  trait WithDerbyEvolutions extends WithEvolutions {
    override lazy val database: Database = Databases(
      driver = "org.apache.derby.jdbc.EmbeddedDriver",
      url = "jdbc:derby:memory:default;create=true"
    )
  }

  trait WithDerbyEvolutionsSchema extends WithDerbyEvolutions {
    override lazy val evolutions: DatabaseEvolutions = new DatabaseEvolutions(
      database = database,
      schema = "testschema"
    )
  }

  object TestEvolutions {
    val a1 = Evolution(
      1,
      "create table test (id bigint not null, name varchar(255));",
      "drop table test;"
    )

    val a2 = Evolution(
      2,
      "alter table test add column age int;",
      "alter table test drop age;"
    )

    val a3 = Evolution(
      3,
      "insert into test (id, name, age) values (1, 'alice', 42);",
      "delete from test;"
    )

    val b1 = Evolution(
      1,
      "create table test (id bigint not null, content varchar(255));",
      "drop table test;"
    )

    val b3 = Evolution(
      3,
      "insert into test (id, content, age) values (1, 'bob', 42);",
      "delete from test;"
    )

    val c1 = Evolution(
      1,
      "creaTYPOe table test (id bigint not null, name varchar(255));"
    )
  }

}
