package anorm

import org.specs2.mutable.Specification

import acolyte.Acolyte._
import acolyte.{ Execution, QueryResult }
import acolyte.RowLists.{ stringList, rowList1, rowList2, rowList3 }
import acolyte.Rows.{ row1, row2, row3 }

object AnormSpec extends Specification with H2Database with AnormTest {
  "Anorm" should {
    lazy val fooBarTable = rowList3(
      classOf[Long] -> "id", classOf[String] -> "foo", classOf[Int] -> "bar")

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
        .on('id -> 11L).as(fooBarParser.singleOpt)
        .aka("result data") must beSome(TestTable(11L, "World", 21))

    }

    "returns parsed option" in withQueryResult(
      fooBarTable :+ row3(11L, "World", 21)) { implicit c =>

        SQL("SELECT * FROM test WHERE id = {id}")
          .on('id -> 11L).as(fooBarParser.singleOpt)
          .aka("result data") must beSome(TestTable(11L, "World", 21))

      }

    "returns scalar single value" in withQueryResult(20) { implicit c =>
      SQL("SELECT * FROM test").as(SqlParser.scalar[Int].single).
        aka("single value") must_== 20

    }

    "returns 0 for missing numeric value" in withQueryResult(
      null.asInstanceOf[Double]) { implicit c =>
        SQL("SELECT * FROM test").as(SqlParser.scalar[Double].singleOpt).
          aka("single value") must beSome(0d)

      }

    "returns None for missing single value" in withQueryResult(
      null.asInstanceOf[String]) { implicit c =>
        SQL("SELECT * FROM test").as(SqlParser.scalar[String].singleOpt).
          aka("single value") must beNone
      }

    "returns optional value" in withQueryResult(rowList2(
      classOf[Int] -> "id", classOf[String] -> "val") :+ row2(2, "str")) {
      implicit c =>

        SQL("SELECT * FROM test").as(
          SqlParser.int("id") ~ SqlParser.str("val").? map {
            case id ~ v => (id -> v)
          } single) aka "mapped data" must_== (2 -> Some("str"))

    }

    "returns None for missing value" in withQueryResult(
      rowList1(classOf[Long] -> "id") :+ 123l) { implicit c =>

        SQL("SELECT * FROM test").as(
          SqlParser.long("id") ~ SqlParser.str("val").? map {
            case id ~ v => (id -> v)
          } single) aka "mapped data" must_== (123l -> None)

      }

    "throws exception when single result is missing" in withQueryResult(fooBarTable) { implicit c =>

      SQL("SELECT * FROM test").as(fooBarParser.single).
        aka("mapping") must throwA[Exception](
          "No rows when expecting a single one")
    }

    "throws exception when type doesn't match" in withQueryResult("str") {
      implicit c =>

        SQL("SELECT * FROM test").as(SqlParser.scalar[Int].single).
          aka("mismatching type") must throwA[Exception]("TypeDoesNotMatch")

    }

    "returns executed query and extract scalar single value" in withQueryResult("Result for test-proc-1") { implicit c =>

      SQL("EXEC stored_proc({param})")
        .on('param -> "test-proc-1").executeQuery()
        .as(SqlParser.scalar[String].single) must_== "Result for test-proc-1"
    }

    "returns executed query and handles SQL warning" in withQueryResult(QueryResult.Nil.withWarning("Warning for test-proc-2")) { implicit c =>

      SQL("EXEC stored_proc({param})")
        .on('param -> "test-proc-2").executeQuery()
        .statementWarning aka "statement warning" must beSome.which { warn =>
          warn.getMessage aka "message" must_== "Warning for test-proc-2"
        }
    }
  }
}

sealed trait AnormTest { db: H2Database =>
  import SqlParser._

  val fooBarParser = long("id") ~ str("foo") ~ int("bar") map {
    case id ~ foo ~ bar => TestTable(id, foo, bar)
  }

  def withQueryResult[A](r: QueryResult)(f: java.sql.Connection => A): A =
    f(connection(handleQuery withQueryHandler { _ => r }))
}
