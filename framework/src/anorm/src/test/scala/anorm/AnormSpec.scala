package anorm

import org.specs2.mutable.Specification

import acolyte.Acolyte._
import acolyte.{ Execution, QueryResult }
import acolyte.RowLists.stringList

object AnormSpec extends Specification with H2Database with AnormTest {
  "anorm" should {
    "allow inserting and retrieving data" in withConnection { implicit c =>
      createTestTable()
      SQL("insert into test(id, foo, bar) values ({id}, {foo}, {bar})")
        .on('id -> 10L, 'foo -> "Hello", 'bar -> 20)
        .execute()

      SQL("select * from test where id = {id}")
        .on('id -> 10L)
        .map(row =>
          row[String]("foo") -> row[Int]("bar")
        ).list() must_== List(("Hello", 20))
    }

    "returns query result for SELECT" in withConnection { implicit c =>
      createTestTable()
      SQL("insert into test(id, foo, bar) values ({id}, {foo}, {bar})")
        .on('id -> 11L, 'foo -> "World", 'bar -> 21)
        .execute()

      SQL("select * from test where id = {id}")
        .on('id -> 11L).as(testTableParser.singleOpt)
        .aka("result data") must beSome(TestTable(11L, "World", 21))

    }

    "returns None for missing single value" in withQueryResult(
      null.asInstanceOf[String]) { implicit c =>
        SQL("SELECT * FROM test").as(SqlParser.scalar[String].singleOpt).
          aka("single value") must beNone
      }

    "executes query for stored procedure" >> {
      "returns result data" in withQueryResult("Result for test-proc-1") {
        implicit con =>

          SQL("EXEC stored_proc({param})")
            .on('param -> "test-proc-1").executeQuery()
            .as(SqlParser.scalar[String].single) must_== "Result for test-proc-1"
      }

      "handles SQL warning" in withQueryResult(QueryResult.Nil.withWarning("Warning for test-proc-2")) { implicit con =>

        SQL("EXEC stored_proc({param})")
          .on('param -> "test-proc-2").executeQuery()
          .statementWarning aka "statement warning" must beSome.which { warn =>
            warn.getMessage aka "message" must_== "Warning for test-proc-2"
          }
      }
    }
  }
}

sealed trait AnormTest { db: H2Database =>
  import SqlParser._

  val testTableParser = long("id") ~ str("foo") ~ int("bar") map {
    case id ~ foo ~ bar => TestTable(id, foo, bar)
  }

  def withQueryResult[A](r: QueryResult)(f: java.sql.Connection => A): A =
    f(connection(handleQuery withQueryHandler { _ => r }))
}
