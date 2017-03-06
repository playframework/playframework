/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.db

import java.sql.{ Connection, ResultSet, SQLException }
import javax.sql.DataSource

import com.zaxxer.hikari.HikariDataSource
import org.jdbcdslog.LogSqlDataSource
import org.specs2.mutable.{ After, Specification }
import org.specs2.concurrent.ExecutionEnv
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.Try

class DatabasesSpec(implicit ee: ExecutionEnv) extends Specification with DefaultAwaitTimeout with FutureAwaits {

  def isLogDataSource(ds: DataSource): Boolean = ds match {
    case _: LogSqlDataSource => true
    case w: AsyncDataSource.Wrapper => isLogDataSource(w.dataSource)
    case _ => false
  }

  "Databases" should {

    "create database" in new WithDatabase {
      val db = Databases(name = "test", driver = "org.h2.Driver", url = "jdbc:h2:mem:test")
      db.name must_== "test"
      db.url must_== "jdbc:h2:mem:test"
    }

    "create database with named arguments" in new WithDatabase {
      val db = Databases(name = "test", driver = "org.h2.Driver", url = "jdbc:h2:mem:test")
      db.name must_== "test"
      db.url must_== "jdbc:h2:mem:test"
    }

    "create default database" in new WithDatabase {
      val db = Databases(driver = "org.h2.Driver", url = "jdbc:h2:mem:default")
      db.name must_== "default"
      db.url must_== "jdbc:h2:mem:default"
    }

    "create database with log sql" in new WithDatabase {
      val config = Map("logSql" -> "true")
      val db = Databases(driver = "org.h2.Driver", url = "jdbc:h2:mem:default", config = config)
      isLogDataSource(db.dataSource) must beTrue
    }

    "create default in-memory database" in new WithDatabase {
      val db = Databases.inMemory()
      db.name must_== "default"
      db.url must_== "jdbc:h2:mem:default"
    }

    "create named in-memory database" in new WithDatabase {
      val db = Databases.inMemory(name = "test")
      db.name must_== "test"
      db.url must_== "jdbc:h2:mem:test"
    }

    "create in-memory database with url options" in new WithDatabase {
      val db = Databases.inMemory(urlOptions = Map("MODE" -> "MySQL"))
      db.name must_== "default"
      db.url must_== "jdbc:h2:mem:default"
      db.dataSource match {
        case ds: HikariDataSource => ds.getJdbcUrl must_== "jdbc:h2:mem:default;MODE=MySQL"
        case _ =>
      }
    }

    "supply connections" in new WithDatabase {
      val db = Databases.inMemory(name = "test-connection")

      val connection = db.getConnection
      connection.createStatement.execute("create table test (id bigint not null, name varchar(255))")
      connection.close()

      // Async

      await {
        db.getConnectionAsync().map { c =>
          c.createStatement.execute("create table test_async (id bigint not null, name varchar(255))")
          c.close()
        }
      }

    }

    "supply async connections up to the pool limit" in new WithDatabase {
      val db = Databases.inMemory(name = "test-connection")
      val poolSize = 10

      // Get poolSize connections
      val connections: Seq[Connection] = await {
        Future.sequence((0 until poolSize).map(_ => db.getConnectionAsync()))
      }

      // Try to get connections one by one, closing the existing connections to free them up.
      val extraConnections: Seq[Connection] = for (heldConnection <- connections) yield {

        // Try to get one more - this shouldn't succeeed yet
        val extraConnection: Future[Connection] = db.getConnectionAsync()
        // Give the connection pool a moment to get the connection. This will only succeed if the pool limit is not
        // enforced properly. Due to timing issues it might not fail, even if there's a bug in the pool implementation.
        Thread.sleep(10)
        extraConnection.isCompleted must beFalse

        // Close the held connection, this should allow the next connection to be available
        heldConnection.close()
        await { extraConnection } // This will await the connection or timeout if it isn't available
      }

      // Clean up
      for (c <- extraConnections) { c.close() }
    }

    /** Convert a ResultSet into a List. Assumes rows are (Int, String). */
    def resultSetToList(results: ResultSet) = {
      val buffer = new ArrayBuffer[(Int, String)]()
      while (results.next) {
        buffer += ((results.getInt(1), results.getString(2)))
      }
      buffer.toList
    }

    "enable autocommit on connections by default" in new WithDatabase {
      val db = Databases.inMemory(name = "test-autocommit")

      val c1 = db.getConnection
      val c2 = await { db.getConnectionAsync() }

      try {
        c1.createStatement.execute("create table test (id bigint not null, name varchar(255))")
        c1.createStatement.execute("insert into test (id, name) values (1, 'alice')")
        resultSetToList(c2.createStatement.executeQuery("select * from test")) must_== List((1, "alice"))
      } finally {
        c1.close()
        c2.close()
      }
    }

    "provide connection helper" in new WithDatabase {
      val db = Databases.inMemory(name = "test-withConnection")

      db.withConnection { c =>
        c.createStatement.execute("create table test (id bigint not null, name varchar(255))")
        c.createStatement.execute("insert into test (id, name) values (1, 'alice')")
        resultSetToList(c.createStatement.executeQuery("select * from test")) must_== List((1, "alice"))
      }

      // Async

      await {
        db.withConnectionAsync { c =>
          c.createStatement.execute("insert into test (id, name) values (2, 'bob')")
          resultSetToList(c.createStatement.executeQuery("select * from test order by id")) must_== List((1, "alice"), (2, "bob"))
          Future.successful(())
        }
      }
    }

    "provide transaction helper" in new WithDatabase {
      val db = Databases.inMemory(name = "test-withTransaction")

      // Compares the values in the 'test' table to a List of (Int, String) pairs.
      def checkTestTable(expected: List[(Int, String)]) = {
        db.withConnection { c =>
          val results = c.createStatement.executeQuery("select * from test order by id")
          resultSetToList(results) must_== expected
        }
      }

      db.withTransaction { c =>
        c.createStatement.execute("create table test (id bigint not null, name varchar(255))")
        c.createStatement.execute("insert into test (id, name) values (1, 'alice')")
      }

      checkTestTable(List((1, "alice")))

      db.withTransaction { c =>
        c.createStatement.execute("insert into test (id, name) values (2, 'bob')")
        throw new RuntimeException("boom")
        success
      } must throwA[RuntimeException](message = "boom")

      checkTestTable(List((1, "alice")))

      // Async

      await {
        db.withTransactionAsync { c =>
          c.createStatement.execute("insert into test (id, name) values (3, 'charlie')")
          Future.successful(1)
        }
      }

      checkTestTable(List((1, "alice"), (3, "charlie")))

      await {
        db.withTransactionAsync[Unit] { c =>
          c.createStatement.execute("insert into test (id, name) values (4, 'dan')")
          throw new RuntimeException("bam")
        }
      } must throwA[RuntimeException](message = "bam")

      checkTestTable(List((1, "alice"), (3, "charlie")))
    }

    "not supply connections after shutdown" in {
      val db = Databases.inMemory(name = "test-shutdown")
      db.getConnection.close()
      db.shutdown()
      db.getConnection must throwA[SQLException].like {
        case e => e.getMessage must endWith("has been closed.")
      }

      // Async

      await(db.getConnectionAsync()) must throwAn[IllegalStateException]
    }

    "not supply connections after shutdown a database with log sql" in {
      val config = Map("logSql" -> "true")
      val db = Databases(driver = "org.h2.Driver", url = "jdbc:h2:mem:default", config = config)

      db.getConnection.close()
      db.shutdown()
      db.getConnection must throwA[SQLException]

      // Async

      await(db.getConnectionAsync()) must throwAn[IllegalStateException]
    }

  }

  trait WithDatabase extends After {
    def db: Database
    def after = db.shutdown()
  }

}
