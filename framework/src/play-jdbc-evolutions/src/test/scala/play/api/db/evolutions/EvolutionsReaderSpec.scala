/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.db.evolutions

import java.io.File

import org.specs2.mutable.Specification
import play.api.{ Environment, Mode }

class EvolutionsReaderSpec extends Specification {

  "EnvironmentEvolutionsReader" should {

    "read evolution files from classpath" in {
      val environment = Environment(new File("."), getClass.getClassLoader, Mode.Test)
      val reader = new EnvironmentEvolutionsReader(environment)

      reader.evolutions("test") must_== Seq(
        Evolution(1, "create table test (id bigint not null, name varchar(255));", "drop table if exists test;"),
        Evolution(2, "insert into test (id, name) values (1, 'alice');\ninsert into test (id, name) values (2, 'bob');", "delete from test;"),
        Evolution(3, "insert into test (id, name) values (3, 'charlie');\ninsert into test (id, name) values (4, 'dave');", ""),
        Evolution(4, "", "")
      )
    }

    "read evolution files with different comment syntax" in {
      val environment = Environment(new File("."), getClass.getClassLoader, Mode.Test)
      val reader = new EnvironmentEvolutionsReader(environment)

      reader.evolutions("commentsyntax") must_== Seq(
        Evolution(1, "select 1;", "select 2;"), // 1.sql should have MySQL-style comments
        Evolution(2, "select 3;", "select 4;"), // 2.sql should have SQL92-style comments
        Evolution(3, "select 5;", "select 6;") // 3.sql mixes styles with arbitrary text
      )
    }

  }
}
