/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.testing.database

import java.sql.SQLException

import org.specs2.mutable.Specification

class ScalaTestingWithDatabases extends Specification {
  // We don't test this code because otherwise it would try to connect to MySQL
  class NotTested {
    {
      // #database
      import play.api.db.Databases

      val database = Databases(
        driver = "com.mysql.jdbc.Driver",
        url = "jdbc:mysql://localhost/test"
      )
      // #database
    }

    {
      // #full-config
      import play.api.db.Databases

      val database = Databases(
        driver = "com.mysql.jdbc.Driver",
        url = "jdbc:mysql://localhost/test",
        name = "mydatabase",
        config = Map(
          "username" -> "test",
          "password" -> "secret"
        )
      )
      // #full-config

      // #shutdown
      database.shutdown()
      // #shutdown
    }

    {
      // #with-database
      import play.api.db.Databases

      Databases.withDatabase(
        driver = "com.mysql.jdbc.Driver",
        url = "jdbc:mysql://localhost/test"
      ) { database =>
        val connection = database.getConnection()
      // ...
      }
      // #with-database
    }

    {
      // #custom-with-database
      import play.api.db.Database
      import play.api.db.Databases

      def withMyDatabase[T](block: Database => T) = {
        Databases.withDatabase(
          driver = "com.mysql.jdbc.Driver",
          url = "jdbc:mysql://localhost/test",
          name = "mydatabase",
          config = Map(
            "username" -> "test",
            "password" -> "secret"
          )
        )(block)
      }
      // #custom-with-database

      // #custom-with-database-use
      withMyDatabase { database =>
        val connection = database.getConnection()
      // ...
      }
      // #custom-with-database-use
    }
  }

  "database helpers" should {
    "allow connecting to an in memory database" in {
      // #in-memory
      import play.api.db.Databases

      val database = Databases.inMemory()
      // #in-memory

      try {
        database.getConnection().getMetaData.getDatabaseProductName must_== "H2"
      } finally {
        database.shutdown()
      }
    }

    "allow connecting to an in memory database with more options" in {
      // #in-memory-full-config
      import play.api.db.Databases

      val database = Databases.inMemory(
        name = "mydatabase",
        urlOptions = Map(
          "MODE" -> "MYSQL"
        ),
        config = Map(
          "logStatements" -> true
        )
      )
      // #in-memory-full-config

      try {
        database.getConnection().getMetaData.getDatabaseProductName must_== "H2"
      } finally {
        // #in-memory-shutdown
        database.shutdown()
        // #in-memory-shutdown
      }
    }

    "manage an in memory database for the user" in {
      // #with-in-memory
      import play.api.db.Databases

      Databases.withInMemory() { database =>
        val connection = database.getConnection()

      // ...
      }
      // #with-in-memory
      ok
    }

    "manage an in memory database for the user with custom config" in {
      // #with-in-memory-custom
      import play.api.db.Database
      import play.api.db.Databases

      def withMyDatabase[T](block: Database => T) = {
        Databases.withInMemory(
          name = "mydatabase",
          urlOptions = Map(
            "MODE" -> "MYSQL"
          ),
          config = Map(
            "logStatements" -> true
          )
        )(block)
      }
      // #with-in-memory-custom

      withMyDatabase(_.getConnection().getMetaData.getDatabaseProductName must_== "H2")
    }

    "allow running evolutions" in play.api.db.Databases.withInMemory() { database =>
      // #apply-evolutions
      import play.api.db.evolutions._

      Evolutions.applyEvolutions(
        database,
        SimpleEvolutionsReader.forDefault(
          Evolution(
            1,
            "create table test (id bigint not null, name varchar(255));",
            "drop table test;"
          )
        )
      )
      // #apply-evolutions

      // #cleanup-evolutions
      Evolutions.cleanupEvolutions(database)
      // #cleanup-evolutions
      ok
    }

    "allow running static evolutions" in play.api.db.Databases.withInMemory() { database =>
      // #apply-evolutions-simple
      import play.api.db.evolutions._

      Evolutions.applyEvolutions(
        database,
        SimpleEvolutionsReader.forDefault(
          Evolution(
            1,
            "create table test (id bigint not null, name varchar(255));",
            "drop table test;"
          )
        )
      )
      // #apply-evolutions-simple

      val connection = database.getConnection()
      connection.prepareStatement("insert into test values (10, 'testing')").execute()

      // #cleanup-evolutions-simple
      Evolutions.cleanupEvolutions(database)
      // #cleanup-evolutions-simple

      connection.prepareStatement("select * from test").executeQuery() must throwAn[SQLException]
    }

    "allow running evolutions from a custom path" in play.api.db.Databases.withInMemory() { database =>
      // #apply-evolutions-custom-path
      import play.api.db.evolutions._
      import play.api._
      import java.io.File

      val environment   = Environment(new File("."), getClass.getClassLoader, Mode.Test)
      val configuration = Configuration.load(environment)
      val evoConfig     = new DefaultEvolutionsConfigParser(configuration).get()

      Evolutions.applyEvolutions(database, ClassLoaderEvolutionsReader.forPrefix(evoConfig, "testdatabase/"))
      // #apply-evolutions-custom-path
      ok
    }

    "allow play to manage evolutions for you" in play.api.db.Databases.withInMemory() { database =>
      // #with-evolutions
      import play.api.db.evolutions._
      import play.api._
      import java.io.File

      val environment   = Environment(new File("."), getClass.getClassLoader, Mode.Test)
      val configuration = Configuration.load(environment)
      val evoConfig     = new DefaultEvolutionsConfigParser(configuration).get()
      val evoReader     = new EnvironmentEvolutionsReader(evoConfig, environment)

      Evolutions.withEvolutions(database, evoReader) {
        val connection = database.getConnection()

        // ...
      }
      // #with-evolutions
      ok
    }

    "allow simple composition of with database and with evolutions" in {
      // #with-evolutions-custom
      import play.api.db.Database
      import play.api.db.Databases
      import play.api.db.evolutions._

      def withMyDatabase[T](block: Database => T) = {
        Databases.withInMemory(
          urlOptions = Map(
            "MODE" -> "MYSQL"
          ),
          config = Map(
            "logStatements" -> true
          )
        ) { database =>
          Evolutions.withEvolutions(
            database,
            SimpleEvolutionsReader.forDefault(
              Evolution(
                1,
                "create table test (id bigint not null, name varchar(255));",
                "drop table test;"
              )
            )
          ) {
            block(database)
          }
        }
      }
      // #with-evolutions-custom

      // #with-evolutions-custom-use
      withMyDatabase { database =>
        val connection = database.getConnection()
        connection.prepareStatement("insert into test values (10, 'testing')").execute()

        connection
          .prepareStatement("select * from test where id = 10")
          .executeQuery()
          .next() must_== true
      }
      // #with-evolutions-custom-use
    }
  }
}
