/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.db

import java.sql.{ Connection, SQLTransientConnectionException }
import java.time.{ Duration => JavaDuration }
import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{ Application, Play }

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future, TimeoutException }

object Database_01_AsyncVsBlocking {
  @State(Scope.Thread)
  @AuxCounters(AuxCounters.Type.OPERATIONS)
  class OpCounters {
    var connectTimeouts = 0
    var jdbcFutureTimeouts = 0
    var cpuFutureTimeouts = 0
  }
}

/**
 * This benchmark reads a cookie value from a RequestHeader.
 */
@State(Scope.Benchmark)
@Fork(1)
abstract class Database_01_AsyncVsBlocking {

  @Param(Array("async", "blocking"))
  var dataSource: String = "async"

  private var application: Application = null
  private var executionContext: ExecutionContext = null
  private var db: Database = null
  private var dataSourceAsync: Boolean = false
  private val awaitDuration = Duration(350, TimeUnit.MILLISECONDS)

  // Set up pools at the level of a trial so that they have time to
  // (possibly) grow in size based on high usage over multiple iterations.
  @Setup(Level.Trial)
  def setup(): Unit = {
    application = GuiceApplicationBuilder().configure(Map(
      "db.default.driver" -> "org.h2.Driver",
      "db.default.url" -> "jdbc:h2:mem:default",
      "db.default.hikaricp.connectionTimeout" -> JavaDuration.ofMillis(250),
      // Force the application to use 15 threads to make this test more consistent
      // on different machines.
      "akka.actor.default-dispatcher.fork-join-executor.parallelism-min" -> "15",
      "akka.actor.default-dispatcher.fork-join-executor.parallelism-factor" -> "15",
      "akka.actor.default-dispatcher.fork-join-executor.parallelism-max" -> "15"
    )).build()
    Play.start(application)
    db = application.injector.instanceOf[Database]
    db.withTransaction { c =>
      c.createStatement.execute("create table test (id bigint not null, count bigint not null)")
      for (id <- 0 until 1000) {
        c.createStatement.execute(s"insert into test (id, count) values ($id, 0)")
      }
    }

    executionContext = application.injector.instanceOf[ExecutionContext]
    dataSourceAsync = dataSource match {
      case "async" => true
      case "blocking" => false
      case other => throw new IllegalStateException(s"Unknown connPool parameter: $other")
    }
  }

  @TearDown(Level.Trial)
  def tearDown(): Unit = {
    Play.stop(application)
  }

  import Database_01_AsyncVsBlocking.OpCounters

  @Benchmark
  @Group("ops")
  def jdbcOp(opCounters: OpCounters): Unit = {
    // The synchronous database operation
    def useConnection(connection: Connection): Unit = {
      connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE)
      try {
        val id = 0
        val rs = connection.createStatement.executeQuery(s"select count from test where id = $id")
        assert(rs.next())
        val count = rs.getInt(1)
        assert(!rs.next())
        connection.createStatement.execute(s"update test set count = ${count + 1} where id = $id")
      } catch {
        case _: Exception => connection.rollback()
      }
      connection.close()
    }

    implicit val ec = executionContext
    val fut = if (dataSourceAsync) {
      Future {
        db.getConnectionAsync(autocommit = false).map { connection =>
          useConnection(connection)
        }
      }.flatten
    } else {
      Future {
        val connection = db.getConnection()
        useConnection(connection)
      }
    }
    try Await.result(fut, awaitDuration) catch {
      case _: SQLTransientConnectionException => opCounters.connectTimeouts += 1
      case _: TimeoutException => opCounters.jdbcFutureTimeouts += 1
    }
  }

  @Benchmark
  @Group("ops")
  def cpuOp(opCounters: OpCounters): Unit = {
    implicit val ec = executionContext
    val fut = Future {
      val s = "12345678901234567890"
      s.reverse
    }
    try Await.result(fut, awaitDuration) catch {
      case _: SQLTransientConnectionException => opCounters.connectTimeouts += 1
      case _: TimeoutException => opCounters.cpuFutureTimeouts += 1
    }

  }

}

@Threads(10)
class Database_01_AsyncVsBlocking_10Users extends Database_01_AsyncVsBlocking

@Threads(50)
class Database_01_AsyncVsBlocking_50Users extends Database_01_AsyncVsBlocking