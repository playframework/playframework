package scalaguide.testing.database

import java.sql.SQLException

import org.specs2.mutable.Specification

object ScalaTestingWithDatabases extends Specification {

  // We don't test this code because otherwise it would try to connect to MySQL
  class NotTested {
    {
      //#database
      import play.api.db.Database

      val database = Database(
        driver = "com.mysql.jdbc.Driver",
        url = "jdbc:mysql://localhost/test"
      )
      //#database
    }

    {
      //#full-config
      import play.api.db.Database

      val database = Database(
        driver = "com.mysql.jdbc.Driver",
        url = "jdbc:mysql://localhost/test",
        name = "mydatabase",
        config = Map(
          "user" -> "test",
          "password" -> "secret"
        )
      )
      //#full-config

      //#shutdown
      database.shutdown()
      //#shutdown
    }

    {
      //#with-database
      import play.api.db.Database

      Database.withDatabase(
        driver = "com.mysql.jdbc.Driver",
        url = "jdbc:mysql://localhost/test"
      ) { database =>
        val connection = database.getConnection()
        // ...
      }
      //#with-database
    }

    {
      //#custom-with-database
      import play.api.db.Database

      def withMyDatabase[T](block: Database => T) = {
        Database.withDatabase(
          driver = "com.mysql.jdbc.Driver",
          url = "jdbc:mysql://localhost/test",
          name = "mydatabase",
          config = Map(
            "user" -> "test",
            "password" -> "secret"
          )
        )(block)
      }
      //#custom-with-database

      //#custom-with-database-use
      withMyDatabase { database =>
        val connection = database.getConnection()
        // ...
      }
      //#custom-with-database-use
    }

  }

  "database helpers" should {
    "allow connecting to an in memory database" in {
      //#in-memory
      import play.api.db.Database

      val database = Database.inMemory()
      //#in-memory

      try {
        database.getConnection().getMetaData.getDatabaseProductName must_== "H2"
      } finally {
        database.shutdown()
      }
    }

    "allow connecting to an in memory database with more options" in {
      //#in-memory-full-config
      import play.api.db.Database

      val database = Database.inMemory(
        name = "mydatabase",
        urlOptions = Map(
          "MODE" -> "MYSQL"
        ),
        config = Map(
          "logStatements" -> true
        )
      )
      //#in-memory-full-config

      try {
        database.getConnection().getMetaData.getDatabaseProductName must_== "H2"
      } finally {
        //#in-memory-shutdown
        database.shutdown()
        //#in-memory-shutdown
      }
    }
    
    "manage an in memory database for the user" in {
      //#with-in-memory
      import play.api.db.Database

      Database.withInMemory() { database =>
        val connection = database.getConnection()
        
        // ...
      }
      //#with-in-memory
      ok
    }

    "manage an in memory database for the user with custom config" in {
      //#with-in-memory-custom
      import play.api.db.Database

      def withMyDatabase[T](block: Database => T) = {
        Database.withInMemory(
          name = "mydatabase",
          urlOptions = Map(
            "MODE" -> "MYSQL"
          ),
          config = Map(
            "logStatements" -> true
          )
        )(block)
      }
      //#with-in-memory-custom

      withMyDatabase(_.getConnection().getMetaData.getDatabaseProductName must_== "H2")
    }

    "allow running evolutions" in play.api.db.Database.withInMemory() { database =>
      //#apply-evolutions
      import play.api.db.evolutions._

      Evolutions.applyEvolutions(database)
      //#apply-evolutions

      //#cleanup-evolutions
      Evolutions.cleanupEvolutions(database)
      //#cleanup-evolutions
      ok
    }

    "allow running static evolutions" in play.api.db.Database.withInMemory() { database =>
      //#apply-evolutions-simple
      import play.api.db.evolutions._

      Evolutions.applyEvolutions(database, SimpleEvolutionsReader.forDefault(
        Evolution(
          1,
          "create table test (id bigint not null, name varchar(255));",
          "drop table test;"
        )
      ))
      //#apply-evolutions-simple

      val connection = database.getConnection()
      connection.prepareStatement("insert into test values (10, 'testing')").execute()
      
      //#cleanup-evolutions-simple
      Evolutions.cleanupEvolutions(database)
      //#cleanup-evolutions-simple
      
      connection.prepareStatement("select * from test").executeQuery() must throwAn[SQLException]
    }
    
    "allow running evolutions from a custom path" in play.api.db.Database.withInMemory() { database =>
      //#apply-evolutions-custom-path
      import play.api.db.evolutions._

      Evolutions.applyEvolutions(database, ClassLoaderEvolutionsReader.forPrefix("testdatabase/"))
      //#apply-evolutions-custom-path
      ok
    }

    "allow play to manage evolutions for you" in play.api.db.Database.withInMemory() { database =>
      //#with-evolutions
      import play.api.db.evolutions._

      Evolutions.withEvolutions(database) {
        val connection = database.getConnection()

        // ...
      }
      //#with-evolutions
      ok
    }

    "allow simple composition of with database and with evolutions" in {
      //#with-evolutions-custom
      import play.api.db.Database
      import play.api.db.evolutions._

      def withMyDatabase[T](block: Database => T) = {

        Database.withInMemory(
          urlOptions = Map(
            "MODE" -> "MYSQL"
          ),
          config = Map(
            "logStatements" -> true
          )
        ) { database =>

          Evolutions.withEvolutions(database, SimpleEvolutionsReader.forDefault(
            Evolution(
              1,
              "create table test (id bigint not null, name varchar(255));",
              "drop table test;"
            )
          )) {

            block(database)

          }
        }
      }
      //#with-evolutions-custom

      //#with-evolutions-custom-use
      withMyDatabase { database =>
        val connection = database.getConnection()
        connection.prepareStatement("insert into test values (10, 'testing')").execute()

        connection.prepareStatement("select * from test where id = 10")
          .executeQuery().next() must_== true
      }
      //#with-evolutions-custom-use
    }

  }


}
