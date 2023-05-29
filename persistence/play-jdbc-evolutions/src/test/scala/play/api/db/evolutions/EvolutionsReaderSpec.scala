/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db.evolutions

import java.io.File

import org.specs2.mutable.Specification
import play.api.Configuration
import play.api.Environment
import play.api.Logger
import play.api.Mode

object EvolutionsReaderSpec {
  initLogback()
  val defaultEvolutionsApiLogger: Logger = Logger(classOf[DefaultEvolutionsApi])

  @scala.annotation.tailrec
  private def initLogback(attempts: Int = 0): Unit = {
    val factory = org.slf4j.LoggerFactory.getILoggerFactory
    if (factory.isInstanceOf[org.slf4j.helpers.SubstituteLoggerFactory]) {
      if (attempts < 30) {
        java.util.concurrent.TimeUnit.MILLISECONDS.sleep(100L)
        initLogback(attempts + 1)
      } else {
        val msg = s"Failed to initialise Logback after $attempts attempts"
        throw new ExceptionInInitializerError(msg)
      }
    }
  }
}

class EvolutionsReaderSpec extends Specification {
  import EvolutionsReaderSpec.defaultEvolutionsApiLogger

  "EnvironmentEvolutionsReader" should {
    "read evolution files from classpath" in withLogbackCapturingAppender {
      val environment              = Environment(new File("."), getClass.getClassLoader, Mode.Test)
      val configuration            = Configuration.load(environment)
      val evolutionsConfigurations = new DefaultEvolutionsConfigParser(configuration).get()
      val appender                 = LogbackCapturingAppender.attachForLogger(defaultEvolutionsApiLogger)
      val reader                   = new EnvironmentEvolutionsReader(evolutionsConfigurations, environment)

      reader.evolutions("test") must_== Seq(
        Evolution(1, "create table test (id bigint not null, name varchar(255));", "drop table if exists test;"),
        Evolution(
          2,
          "insert into test (id, name) values (1, 'alice');\ninsert into test (id, name) values (2, 'bob');",
          "delete from test;"
        ),
        Evolution(
          3,
          "insert into test (id, name) values (3, 'charlie');\ninsert into test (id, name) values (4, 'dave');",
          ""
        ),
        Evolution(4, "insert into test (id, name) values (5, 'Emma');", "delete from test where name = 'Emma';"),
        Evolution(5, "insert into test (id, name) values (6, 'Noah');", "delete from test where name = 'Noah';"),
        Evolution(6, "insert into test (id, name) values (7, 'Olivia');", "delete from test where name = 'Olivia';"),
        Evolution(7, "insert into test (id, name) values (8, 'Liam');", "delete from test where name = 'Liam';"),
        Evolution(8, "insert into test (id, name) values (9, 'William');", "delete from test where name = 'William';"),
        Evolution(9, "insert into test (id, name) values (10, 'Sophia');", "delete from test where name = 'Sophia';"),
        Evolution(10, "insert into test (id, name) values (11, 'Mason');", "delete from test where name = 'Mason';")
        // revision file 100 will not even run because revision 11 - 99 do not exist
      )
      appender.events.map(_.getMessage) must_== Seq(
        "Ignoring evolution script 01.sql, using 1.sql instead already",
        "Ignoring evolution script 001.sql, using 1.sql instead already",
        "Ignoring evolution script 02.sql, using 2.sql instead already",
        "Ignoring evolution script 002.sql, using 2.sql instead already",
        "Ignoring evolution script 005.sql, using 05.sql instead already",
        "Ignoring evolution script 0010.sql, using 010.sql instead already"
      )
    }

    "read evolution files with different comment syntax" in {
      val environment              = Environment(new File("."), getClass.getClassLoader, Mode.Test)
      val configuration            = Configuration.load(environment)
      val evolutionsConfigurations = new DefaultEvolutionsConfigParser(configuration).get()
      val reader                   = new EnvironmentEvolutionsReader(evolutionsConfigurations, environment)

      reader.evolutions("commentsyntax") must_== Seq(
        Evolution(1, "select 1;", "select 2;"), // 1.sql should have MySQL-style comments
        Evolution(2, "select 3;", "select 4;"), // 2.sql should have SQL92-style comments
        Evolution(3, "select 5;", "select 6;")  // 3.sql mixes styles with arbitrary text
      )
    }
  }

  private def withLogbackCapturingAppender[T](block: => T): T = {
    val result = block
    LogbackCapturingAppender.detachAll()
    result
  }
}
