/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import java.sql.SQLException
import com.zaxxer.hikari.HikariDataSource
import org.specs2.mutable.{ After, Specification }

object DatabaseSpec extends Specification {

  "Database" should {

    "create database" in new WithDatabase {
      val db = Database(name = "test", driver = "org.h2.Driver", url = "jdbc:h2:mem:test")
      db.name must_== "test"
      db.url must_== "jdbc:h2:mem:test"
    }

    "create database with named arguments" in new WithDatabase {
      val db = Database(name = "test", driver = "org.h2.Driver", url = "jdbc:h2:mem:test")
      db.name must_== "test"
      db.url must_== "jdbc:h2:mem:test"
    }

    "create default database" in new WithDatabase {
      val db = Database(driver = "org.h2.Driver", url = "jdbc:h2:mem:default")
      db.name must_== "default"
      db.url must_== "jdbc:h2:mem:default"
    }

    "create default in-memory database" in new WithDatabase {
      val db = Database.inMemory()
      db.name must_== "default"
      db.url must_== "jdbc:h2:mem:default"
    }

    "create named in-memory database" in new WithDatabase {
      val db = Database.inMemory(name = "test")
      db.name must_== "test"
      db.url must_== "jdbc:h2:mem:test"
    }

    "create in-memory database with url options" in new WithDatabase {
      val db = Database.inMemory(urlOptions = Map("MODE" -> "MySQL"))
      db.name must_== "default"
      db.url must_== "jdbc:h2:mem:default"
      db.dataSource match {
        case ds: HikariDataSource => ds.getJdbcUrl must_== "jdbc:h2:mem:default;MODE=MySQL"
        case _ =>
      }
    }

    "supply connections" in new WithDatabase {
      val db = Database.inMemory(name = "test-connection")
      val connection = db.getConnection
      connection.createStatement.execute("create table test (id bigint not null, name varchar(255))")
      connection.close()
    }

    "enable autocommit on connections by default" in new WithDatabase {
      val db = Database.inMemory(name = "test-autocommit")

      val c1 = db.getConnection
      val c2 = db.getConnection

      try {
        c1.createStatement.execute("create table test (id bigint not null, name varchar(255))")
        c1.createStatement.execute("insert into test (id, name) values (1, 'alice')")
        val results = c2.createStatement.executeQuery("select * from test")
        results.next must beTrue
        results.next must beFalse
      } finally {
        c1.close()
        c2.close()
      }
    }

    "provide connection helper" in new WithDatabase {
      val db = Database.inMemory(name = "test-withConnection")

      db.withConnection { c =>
        c.createStatement.execute("create table test (id bigint not null, name varchar(255))")
        c.createStatement.execute("insert into test (id, name) values (1, 'alice')")
        val results = c.createStatement.executeQuery("select * from test")
        results.next must beTrue
        results.next must beFalse
      }
    }

    "provide transaction helper" in new WithDatabase {
      val db = Database.inMemory(name = "test-withTransaction")

      db.withTransaction { c =>
        c.createStatement.execute("create table test (id bigint not null, name varchar(255))")
        c.createStatement.execute("insert into test (id, name) values (1, 'alice')")
      }

      db.withConnection { c =>
        val results = c.createStatement.executeQuery("select * from test")
        results.next must beTrue
        results.next must beFalse
      }

      db.withTransaction { c =>
        c.createStatement.execute("insert into test (id, name) values (2, 'bob')")
        throw new RuntimeException("boom")
        success
      } must throwA[RuntimeException](message = "boom")

      db.withConnection { c =>
        val results = c.createStatement.executeQuery("select * from test")
        results.next must beTrue
        results.next must beFalse
      }
    }

    "not supply connections after shutdown" in {
      val db = Database.inMemory(name = "test-shutdown")
      db.getConnection.close()
      db.shutdown()
      db.getConnection.close() must throwA[SQLException].like {
        case e => e.getMessage must startWith("Pool has been shutdown")
      }
    }

  }

  trait WithDatabase extends After {
    def db: Database
    def after = () //db.shutdown()
  }

}
