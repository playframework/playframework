package anorm

import org.specs2.mutable.Specification

import acolyte.Acolyte.{ connection, handleQuery, handleStatement }
import acolyte.{
  UpdateExecution,
  QueryResult,
  DefinedParameter => DParam,
  ParameterMetaData
}
import acolyte.RowLists
import RowLists.{ stringList, rowList1, rowList2, rowList3 }
import acolyte.Rows.{ row1, row2, row3 }
import acolyte.Implicits._

object AnormSpec extends Specification with H2Database with AnormTest {
  "Anorm" title

  lazy val fooBarTable = rowList3(
    classOf[Long] -> "id", classOf[String] -> "foo", classOf[Int] -> "bar")

  "Row parser" should {
    "return newly inserted data" in withConnection { implicit c =>
      createTest1Table()
      SQL("insert into test1(id, foo, bar) values ({id}, {foo}, {bar})")
        .on('id -> 10L, 'foo -> "Hello", 'bar -> 20).execute()

      SQL("select * from test1 where id = {id}").on('id -> 10L)
        .map(row => row[String]("foo") -> row[Int]("bar"))
        .single must_== ("Hello" -> 20)
    }

    "return case class instance from result" in withConnection { implicit c =>
      createTest1Table()
      SQL("insert into test1(id, foo, bar) values ({id}, {foo}, {bar})")
        .on('id -> 11L, 'foo -> "World", 'bar -> 21)
        .execute()

      SQL("select * from test1 where id = {id}")
        .on('id -> 11L).as(fooBarParser.singleOpt)
        .aka("result data") must beSome(TestTable(11L, "World", 21))

    }

    "return defined option of case class" in withQueryResult(
      fooBarTable :+ row3(11L, "World", 21)) { implicit c =>

        SQL("SELECT * FROM test WHERE id = {id}")
          .on("id" -> 11L).as(fooBarParser.singleOpt)
          .aka("result data") must beSome(TestTable(11L, "World", 21))

      }

    "handle scalar result" >> {
      "return single value" in withQueryResult(20) { implicit c =>
        (SQL("SELECT * FROM test").as(SqlParser.scalar[Int].single).
          aka("single value #1") must_== 20).
          and(SQL("SELECT * FROM test").using(SqlParser.scalar[Int]).
            single aka "single value #2" must_== 20)

      }

      "return None for missing optional value" in withQueryResult(
        null.asInstanceOf[String]) { implicit c =>
          SQL("SELECT * FROM test").as(SqlParser.scalar[String].singleOpt).
            aka("single value") must beNone
        }

      "return 0 for missing optional numeric" in withQueryResult(
        null.asInstanceOf[Double]) { implicit c =>
          SQL("SELECT * FROM test").as(SqlParser.scalar[Double].singleOpt).
            aka("single value") must beSome(0d)

        }

      "throw exception when single result is missing" in withQueryResult(
        fooBarTable) { implicit c =>

          SQL("SELECT * FROM test").as(fooBarParser.single).
            aka("mapping") must throwA[Exception].like {
              case e: Exception => e.getMessage aka "error" mustEqual (
                "SqlMappingError(No rows when expecting a single one)")
            }
        }

      "throw exception when there is more than 1 required or option row" in {
        withQueryResult(stringList :+ "A" :+ "B") { implicit c =>
          lazy val sql = SQL("SELECT 1")

          (sql.as(SqlParser.scalar[Int].single)
            .aka("single parser") must throwA[Exception].like {
              case e: Exception => e.getMessage aka "error" mustEqual (
                "SqlMappingError(too many rows when expecting a single one)")
            }).and(sql.as(SqlParser.scalar[Int].singleOpt)
              .aka("singleOpt parser") must throwA[Exception].like {
                case e: Exception => e.getMessage aka "error" mustEqual (
                  "SqlMappingError(too many rows when expecting a single one)")
              })
        }
      }

      "return single string from executed query" in withQueryResult(
        "Result for test-proc-1") { implicit c =>

          SQL("EXEC stored_proc({param})")
            .on("param" -> "test-proc-1").executeQuery()
            .as(SqlParser.scalar[String].single)
            .aka("single string") must_== "Result for test-proc-1"
        }
    }

    "handle optional property in case class" >> {
      "return instance with defined option" in withQueryResult(rowList2(
        classOf[Int] -> "id", classOf[String] -> "val") :+ row2(2, "str")) {
        implicit c =>

          SQL("SELECT * FROM test").as(
            SqlParser.int("id") ~ SqlParser.str("val").? map {
              case id ~ v => (id -> v)
            } single) aka "mapped data" must_== (2 -> Some("str"))

      }

      "return instance with None for column not found" in withQueryResult(
        rowList1(classOf[Long] -> "id") :+ 123l) { implicit c =>

          SQL("SELECT * FROM test").as(
            SqlParser.long("id") ~ SqlParser.str("val").? map {
              case id ~ v => (id -> v)
            } single) aka "mapped data" must_== (123l -> None)

        }

      "throw exception when type doesn't match" in withQueryResult(
        fooBarTable :+ row3(1l, "str", 3)) { implicit c =>

          SQL("SELECT * FROM test").as(
            SqlParser.long("id") ~ SqlParser.int("foo").? map {
              case id ~ v => (id -> v)
            } single) aka "parser" must throwA[Exception].like {
              case e: Exception => e.getMessage aka "error" must startWith(
                "TypeDoesNotMatch(Cannot convert str:")
            }
        }
    }

    "throw exception when type doesn't match" in withQueryResult("str") {
      implicit c =>

        SQL("SELECT * FROM test").as(SqlParser.scalar[Int].single).
          aka("mismatching type") must throwA[Exception]("TypeDoesNotMatch")

    }
  }

  "List" should {
    "be Nil when there is no result" in withQueryResult(QueryResult.Nil) {
      implicit c =>
        SQL("EXEC test").as(SqlParser.scalar[Int].*) aka "list" must_== Nil
    }

    "be parsed from mapped result" in withQueryResult(
      rowList2(classOf[String] -> "foo", classOf[Int] -> "bar").
        append("row1", 100) :+ row2("row2", 200)) { implicit c =>

        SQL("SELECT * FROM test").map(row =>
          row[String]("foo") -> row[Int]("bar")
        ).list aka "tuple list" must_== List("row1" -> 100, "row2" -> 200)
      }

    "be parsed from class mapping" in withQueryResult(
      fooBarTable :+ row3(12L, "World", 101) :+ row3(14L, "Mondo", 3210)) {
        implicit c =>
          SQL("SELECT * FROM test").as(fooBarParser.*).
            aka("parsed list") must_== List(
              TestTable(12L, "World", 101), TestTable(14L, "Mondo", 3210))

      }

    "be parsed from mapping with optional column" in withQueryResult(rowList2(
      classOf[Int] -> "id", classOf[String] -> "val").
      append(9, null.asInstanceOf[String]) :+ row2(2, "str")) { implicit c =>

      SQL("SELECT * FROM test").as(
        SqlParser.int("id") ~ SqlParser.str("val").? map {
          case id ~ v => (id -> v)
        } *) aka "parsed list" must_== List(9 -> None, 2 -> Some("str"))
    }

    "include scalar values" in withQueryResult(
      stringList :+ "A" :+ "B" :+ "C" :+ "D") { implicit c =>

        SQL("SELECT c FROM letters").as(SqlParser.scalar[String].*).
          aka("string list") must_== List("A", "B", "C", "D")
      }
  }

  "Stream" should {
    "be empty when there is no result" in withQueryResult(QueryResult.Nil) {
      implicit c => SQL("EXEC test").apply().headOption must beNone
    }

    "be parsed from mapped result" in withQueryResult(
      rowList2(classOf[String] -> "foo", classOf[Int] -> "bar").
        append("row1", 100) :+ row2("row2", 200)) { implicit c =>

        SQL("SELECT * FROM test").apply()
          .map(row => row[String]("foo") -> row[Int]("bar"))
          .aka("tuple stream") mustEqual {
            List("row1" -> 100, "row2" -> 200).toStream
          }
      }

    "be parsed from class mapping" in withQueryResult(
      fooBarTable :+ row3(12L, "World", 101) :+ row3(14L, "Mondo", 3210)) {
        implicit c =>
          SQL("SELECT * FROM test").apply().map(fooBarParser).
            aka("parsed stream") must_== List(
              Success(TestTable(12L, "World", 101)),
              Success(TestTable(14L, "Mondo", 3210))).toStream

      }

    "be parsed from mapping with optional column" in withQueryResult(rowList2(
      classOf[Int] -> "id", classOf[String] -> "val").
      append(9, null.asInstanceOf[String]) :+ row2(2, "str")) { implicit c =>

      lazy val parser = SqlParser.int("id") ~ SqlParser.str("val").? map {
        case id ~ v => (id -> v)
      }

      SQL("SELECT * FROM test").apply().map(parser).
        aka("parsed stream") must_== List(
          Success(9 -> None), Success(2 -> Some("str"))).toStream
    }

    "include scalar values" in withQueryResult(
      stringList :+ "A" :+ "B" :+ "C" :+ "D") { implicit c =>

        SQL("SELECT c FROM letters").apply().map(SqlParser.scalar[String]).
          aka("string stream") must_== List(
            Success("A"), Success("B"), Success("C"), Success("D"))
      }
  }

  "SQL warning" should {
    "be handle from executed query" in withQueryResult(
      QueryResult.Nil.withWarning("Warning for test-proc-2")) { implicit c =>

        SQL("EXEC stored_proc({param})")
          .on("param" -> "test-proc-2").executeQuery()
          .statementWarning aka "statement warning" must beSome.which { warn =>
            warn.getMessage aka "message" must_== "Warning for test-proc-2"
          }
      }
  }
}

sealed trait AnormTest { db: H2Database =>
  import SqlParser.{ int, long, str }

  val fooBarParser = long("id") ~ str("foo") ~ int("bar") map {
    case id ~ foo ~ bar => TestTable(id, foo, bar)
  }

  def withQueryResult[A](r: QueryResult)(f: java.sql.Connection => A): A =
    f(connection(handleQuery { _ => r }))

}
