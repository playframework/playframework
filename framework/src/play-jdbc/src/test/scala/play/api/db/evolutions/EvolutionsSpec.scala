/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db.evolutions

import org.specs2.mutable.Specification

import play.api.Configuration
import play.api.db.BoneConnectionPool

// TODO: test down scripts
// TODO: test inconsistent state
// TODO: test reading evolution files
// TODO: fuctional test with InvalidDatabaseRevision exception

object EvolutionsSpec extends Specification {

  "Evolutions" should {

    "apply UP scripts" in {
      val db = new BoneConnectionPool(
        Configuration.from(Map(
          "default.driver" -> "org.h2.Driver",
          "default.url" -> "jdbc:h2:mem:evolutions-test"
        ))
      )

      val evolutions = new DefaultEvolutionsApi(db)

      val e1 = Evolution(
        1,
        "create table test (id bigint not null, name varchar(255));",
        "drop table test;"
      )

      val e2 = Evolution(
        2,
        "alter table test add (age int);",
        "alter table test drop age;"
      )

      val e3 = Evolution(
        3,
        "insert into test (id, name, age) values (1, 'alice', 42);",
        "delete from test;"
      )

      // TODO: evolutions passed in order? (change reversing in EvolutionsApi)
      val scripts = evolutions.scripts("default", Seq(e3, e2, e1))

      scripts must have length(3)
      scripts must contain(UpScript(e1), UpScript(e2), UpScript(e3))

      evolutions.evolve("default", scripts, autocommit = true)

      val connection = db.getConnection("default")

      try {
        val resultSet = connection.createStatement.executeQuery("select * from test")
        resultSet.next must beTrue
        resultSet.getLong(1) must_== 1L
        resultSet.getString(2) must_== "alice"
        resultSet.getInt(3) must_== 42
        resultSet.next must beFalse
      } finally {
        connection.close()
        db.stop()
      }
    }

  }
}
