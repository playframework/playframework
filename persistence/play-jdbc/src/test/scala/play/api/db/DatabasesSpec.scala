/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db

import java.sql.SQLException
import java.sql.SQLNonTransientConnectionException
import java.sql.SQLSyntaxErrorException

import acolyte.jdbc.ConnectionHandler
import acolyte.jdbc.QueryResult
import acolyte.jdbc.ResourceHandler
import acolyte.jdbc.StatementHandler
import acolyte.jdbc.UpdateResult
import org.jdbcdslog.ConnectionPoolDataSourceProxy
import org.specs2.mutable.After
import org.specs2.mutable.Specification

class DatabasesSpec extends Specification {
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
      val db     = Databases(driver = "org.h2.Driver", url = "jdbc:h2:mem:default", config = config)
      db.dataSource must beAnInstanceOf[ConnectionPoolDataSourceProxy]
    }

    "create default in-memory database" in new WithDatabase {
      val db = Databases.inMemory()
      db.name must_== "default"
      db.url must beEqualTo("jdbc:h2:mem:default")
    }

    "create named in-memory database" in new WithDatabase {
      val db = Databases.inMemory(name = "test")
      db.name must_== "test"
      db.url must beEqualTo("jdbc:h2:mem:test")
    }

    "create in-memory database with url options" in new WithDatabase {
      val db = Databases.inMemory(urlOptions = Map("MODE" -> "MySQL"))
      db.name must_== "default"
      db.url must_== "jdbc:h2:mem:default;MODE=MySQL"
    }

    "create in-memory database with url as is when there are no additional options" in new WithDatabase {
      val db = Databases.inMemory()
      db.name must_== "default"
      db.url must_== "jdbc:h2:mem:default"
    }

    "supply connections" in new WithDatabase {
      val db         = Databases.inMemory(name = "test-connection")
      val connection = db.getConnection()
      connection.createStatement.execute("create table test (id bigint not null, name varchar(255))")
      connection.close()
    }

    "enable autocommit on connections by default" in new WithDatabase {
      val db = Databases.inMemory(name = "test-autocommit")

      val c1 = db.getConnection()
      val c2 = db.getConnection()

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
      val db = Databases.inMemory(name = "test-withConnection")

      db.withConnection { c =>
        c.createStatement.execute("create table test (id bigint not null, name varchar(255))")
        c.createStatement.execute("insert into test (id, name) values (1, 'alice')")
        val results = c.createStatement.executeQuery("select * from test")
        results.next must beTrue
        results.next must beFalse
      }
    }

    "provide transaction helper" in new WithDatabase {
      val db = Databases.inMemory(name = "test-withTransaction")

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

    "resurface a non-fatal error when the rollback fails" in {
      withNonFatalErrorDatabase("test-withTransaction-nonFatalError") { db =>
        db.withTransaction { c =>
          c.createStatement.execute("insert into test (id, name) values (1, 'alice')")
        } must throwA[SQLSyntaxErrorException](message = "Invalid SQL")
      }
    }

    "resurface a fatal error when the rollback fails" in {
      withFatalErrorDatabase("test-withTransaction-fatalError") { db =>
        db.withTransaction { c =>
          c.createStatement.execute("insert into test (id, name) values (1, 'alice')")
        } must throwA[SQLNonTransientConnectionException](message = "Socket error")
      }
    }

    "manual setup transaction isolation level" in new WithDatabase {
      val db = Databases.inMemory(name = "test-manualSetupTrasactionIsolationLevel")

      db.withTransaction(TransactionIsolationLevel.Serializable) { c =>
        c.createStatement.execute("create table test (id bigint not null, name varchar(255))")
        c.createStatement.execute("insert into test (id, name) values (1, 'alice')")
      }
    }

    "resurface a non-fatal error when the rollback fails, with isolation level" in {
      withNonFatalErrorDatabase("test-withTransactionIsolationLevel-nonFatalError") { db =>
        db.withTransaction(TransactionIsolationLevel.Serializable) { c =>
          c.createStatement.execute("insert into test (id, name) values (1, 'alice')")
        } must throwA[SQLSyntaxErrorException](message = "Invalid SQL")
      }
    }

    "resurface a fatal error when the rollback fails, with isolation level" in {
      withFatalErrorDatabase("test-withTransactionIsolationLevel-fatalError") { db =>
        db.withTransaction(TransactionIsolationLevel.Serializable) { c =>
          c.createStatement.execute("insert into test (id, name) values (1, 'alice')")
        } must throwA[SQLNonTransientConnectionException](message = "Socket error")
      }
    }

    "not supply connections after shutdown" in {
      val db = Databases.inMemory(name = "test-shutdown")
      db.getConnection().close()
      db.shutdown()
      db.getConnection().close() must throwA[SQLException].like {
        case e => e.getMessage must endWith("has been closed.")
      }
    }

    "not supply connections after shutdown a database with log sql" in {
      val config = Map("logSql" -> "true")
      val db     = Databases(driver = "org.h2.Driver", url = "jdbc:h2:mem:default", config = config)

      db.getConnection().close()
      db.shutdown()
      db.getConnection().close() must throwA[SQLException]
    }
  }

  // statement-level error, as reported on invalid SQL
  def invalidSql: SQLException = new SQLSyntaxErrorException("Invalid SQL", "42000")

  // connection-level error, as reported on lost socket
  def connectionLost: SQLException = new SQLNonTransientConnectionException("Socket error", "08S01")

  /**
   * A database that rejects every statement as invalid SQL, which leaves the connection alive, and
   * whose connections turn out to be gone by the time the transaction is rolled back, so that the
   * rollback fails in turn. The two errors are deliberately distinct, so that a test can tell which
   * one the caller ends up with.
   */
  private def nonFatalErrorDatabase(name: String): Database =
    acolyteDatabase(
      name,
      "DatabasesSpec-nonFatalError",
      new ConnectionHandler.Default(
        new StatementHandler {
          def isQuery(sql: String): Boolean = false

          def whenSQLQuery(sql: String, parameters: java.util.List[StatementHandler.Parameter]): QueryResult =
            throw invalidSql

          def whenSQLUpdate(sql: String, parameters: java.util.List[StatementHandler.Parameter]): UpdateResult =
            throw invalidSql
        },
        new ResourceHandler {
          // only the rollback matters here, the transaction is never committed
          def whenCommitTransaction(connection: acolyte.jdbc.Connection): Unit   = ()
          def whenRollbackTransaction(connection: acolyte.jdbc.Connection): Unit = throw connectionLost
        }
      )
    )

  /**
   * A database that loses its connection on every statement. Nothing needs to break the rollback
   * here: the pool sees a fatal error, evicts the connection, and refuses the rollback on its own.
   */
  private def fatalErrorDatabase(name: String): Database =
    acolyteDatabase(
      name,
      "DatabasesSpec-fatalError",
      new ConnectionHandler.Default(
        new StatementHandler {
          def isQuery(sql: String): Boolean = false

          def whenSQLQuery(sql: String, parameters: java.util.List[StatementHandler.Parameter]): QueryResult =
            throw connectionLost

          def whenSQLUpdate(sql: String, parameters: java.util.List[StatementHandler.Parameter]): UpdateResult =
            throw connectionLost
        },
        new ResourceHandler.Default
      )
    )

  private def acolyteDatabase(name: String, handlerId: String, handler: ConnectionHandler): Database = {
    acolyte.jdbc.Driver.register(handlerId, handler)

    Databases(
      driver = "acolyte.jdbc.Driver",
      url = s"jdbc:acolyte:DatabasesSpec?handler=$handlerId",
      name = name
    )
  }

  private def withNonFatalErrorDatabase[T](name: String)(block: Database => T): T =
    withShutdown(nonFatalErrorDatabase(name))(block)

  private def withFatalErrorDatabase[T](name: String)(block: Database => T): T =
    withShutdown(fatalErrorDatabase(name))(block)

  // Runs the given block against a database, then shuts it down.
  // Provides isolation for Acolyte testing.
  private def withShutdown[T](db: Database)(block: Database => T): T = {
    try block(db)
    finally db.shutdown()
  }

  trait WithDatabase extends After {
    def db: Database
    def after: Unit = () // db.shutdown()
  }
}
