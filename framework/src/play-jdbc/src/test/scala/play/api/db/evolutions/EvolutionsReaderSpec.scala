/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db.evolutions

import java.io.File
import org.specs2.mutable.Specification
import play.api.{ Environment, Mode }

object EvolutionsReaderSpec extends Specification {

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

  }
}
