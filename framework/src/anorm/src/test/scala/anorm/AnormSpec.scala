package anorm

import org.specs2.mutable.Specification

import anorm.{ ParameterValue => Val }

import acolyte.Acolyte._
import acolyte.{
  UpdateExecution,
  QueryResult,
  DefinedParameter => DParam,
  ParameterMetaData
}
import acolyte.RowLists.{ stringList, rowList1, rowList2, rowList3 }
import acolyte.Rows.{ row1, row2, row3 }

object AnormSpec extends Specification with H2Database with AnormTest {
  "Anorm" title

  lazy val fooBarTable = rowList3(
    classOf[Long] -> "id", classOf[String] -> "foo", classOf[Int] -> "bar")

  "Row parser" should {
    "return newly inserted data" in withConnection { implicit c =>
      createTestTable()
      SQL("insert into test(id, foo, bar) values ({id}, {foo}, {bar})")
        .on('id -> 10L, 'foo -> "Hello", 'bar -> 20)
        .execute()

      SQL("select * from test where id = {id}").on('id -> 10L)
        .map(row => row[String]("foo") -> row[Int]("bar"))
        .single must_== ("Hello" -> 20)
    }

    "return case class instance from result" in withConnection { implicit c =>
      createTestTable()
      SQL("insert into test(id, foo, bar) values ({id}, {foo}, {bar})")
        .on('id -> 11L, 'foo -> "World", 'bar -> 21)
        .execute()

      SQL("select * from test where id = {id}")
        .on('id -> 11L).as(fooBarParser.singleOpt)
        .aka("result data") must beSome(TestTable(11L, "World", 21))

    }

    "return defined option of case class" in withQueryResult(
      fooBarTable :+ row3(11L, "World", 21)) { implicit c =>

        SQL("SELECT * FROM test WHERE id = {id}")
          .on('id -> 11L).as(fooBarParser.singleOpt)
          .aka("result data") must beSome(TestTable(11L, "World", 21))

      }

    "handle scalar result" >> {
      "return single value" in withQueryResult(20) { implicit c =>
        SQL("SELECT * FROM test").as(SqlParser.scalar[Int].single).
          aka("single value") must_== 20

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

      "return single string from executed query" in withQueryResult(
        "Result for test-proc-1") { implicit c =>

          SQL("EXEC stored_proc({param})")
            .on('param -> "test-proc-1").executeQuery()
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

      "return instance with None for option" in withQueryResult(
        rowList1(classOf[Long] -> "id") :+ 123l) { implicit c =>

          SQL("SELECT * FROM test").as(
            SqlParser.long("id") ~ SqlParser.str("val").? map {
              case id ~ v => (id -> v)
            } single) aka "mapped data" must_== (123l -> None)

        }
    }

    "throw exception when single result is missing" in withQueryResult(
      fooBarTable) { implicit c =>

        SQL("SELECT * FROM test").as(fooBarParser.single).
          aka("mapping") must throwA[Exception](
            "No rows when expecting a single one")
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
        .aka("tuple stream") must_== List("row1" -> 100, "row2" -> 200).toStream

      true must beTrue
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
          .on('param -> "test-proc-2").executeQuery()
          .statementWarning aka "statement warning" must beSome.which { warn =>
            warn.getMessage aka "message" must_== "Warning for test-proc-2"
          }
      }
  }

  { // Parameter specs
    val bg1 = new java.math.BigDecimal(1.234d)
    val Str = ParameterMetaData.Str
    val Num1 = ParameterMetaData.Numeric(bg1)

    def withConnection[A](f: java.sql.Connection => A): A =
      f(connection(handleStatement withUpdateHandler {
        case UpdateExecution("set-str ?",
          DParam("string", Str) :: Nil) => 1 /* case ok */
        case UpdateExecution("set-bg ?",
          DParam(bg1, Num1) :: Nil) => 1 /* case ok */
        case UpdateExecution("set-s-bg ?, ?",
          DParam("string", Str) :: DParam(bg1, Num1) :: Nil) => 1 /* case ok */
        case UpdateExecution("reorder-s-bg ?, ?",
          DParam(bg1, Num1) :: DParam("string", Str) :: Nil) => 1 /* case ok */
        case UpdateExecution("set-str-opt ?",
          DParam("string", Str) :: Nil) => 1 /* case ok */
        case UpdateExecution("set-bg-opt ?",
          DParam(bg1, Num1) :: Nil) => 1 /* case ok */
        case UpdateExecution("no-param-placeholder", Nil) => 1 /* case ok */
        case UpdateExecution("no-snd-placeholder ?",
          DParam("first", Str) :: Nil) => 1 /* case ok */

      }))

    "Named parameters" should {
      "be one string" in withConnection { implicit c =>
        SQL("set-str {p}").on('p -> "string").
          aka("query") must beLike {
            case q @ SimpleSql( // check accross construction
              SqlQuery("set-str ?", List("p"), _),
              Seq(("p", Val("string", _))), _) =>

              // execute = false: update ok but returns no resultset
              // see java.sql.PreparedStatement#execute
              q.execute() aka "execution" must beFalse
          }
      }

      "be one big decimal" in withConnection { implicit c =>
        SQL("set-bg {p}").on('p -> bg1).execute() must beFalse
      }

      // TODO: single set for other types

      "be multiple (string, big decimal)" in withConnection { implicit c =>
        SQL("set-s-bg {a}, {b}").on("a" -> "string", "b" -> bg1).
          aka("query") must beLike {
            case q @ SimpleSql(
              SqlQuery("set-s-bg ?, ?", List("a", "b"), _),
              Seq(("a", Val("string", _)), ("b", Val(bg1, _))), _) =>
              q.execute() aka "execution" must beFalse

          }
      }

      "be reordered" in withConnection { implicit c =>
        SQL("reorder-s-bg ?, ?").copy(argsInitialOrder = List("b", "a")).
          on('a -> "string", 'b -> bg1) aka "query" must beLike {
            case q @ SimpleSql(
              SqlQuery("reorder-s-bg ?, ?", List("b", "a"), _),
              Seq(("a", Val("string", _)), ("b", Val(bg1, _))), _) =>
              q.execute() aka "execution" must beFalse

          }

      }

      "be defined string option" in withConnection { implicit c =>
        SQL("set-str-opt {p}").on('p -> Some("string")).execute().
          aka("execution") must beFalse
      }

      "be defined big decimal option" in withConnection { implicit c =>
        SQL("set-bg-opt {p}").on('p -> Some(bg1)).
          execute() aka "execute" must beFalse

      }

      "not be set if placeholder not found in SQL" in withConnection {
        implicit c =>
          SQL("no-param-placeholder").on('p -> "not set").execute().
            aka("execution") must beFalse

      }

      "be partially set if matching placeholder is missing for second one" in {
        withConnection { implicit c =>
          SQL("no-snd-placeholder {a}")
            .on("a" -> "first", "b" -> "second").execute() must beFalse

        }
      }

      /*
     http://docs.oracle.com/javase/6/docs/api/java/sql/PreparedStatement.html#setObject%28int,%20java.lang.Object%29
     -> Note: Not all databases allow for a non-typed Null to be sent to the backend. For maximum portability, the setNull or the setObject(int parameterIndex, Object x, int sqlType) method should be used instead of setObject(int parameterIndex, Object x).
     -> Note: This method throws an exception if there is an ambiguity, for example, if the object is of a class implementing more than one of the interfaces named above. 

    "set null parameter from None" in withConnection { implicit c =>
      SQL("set-none {p}").on('p -> None).execute() aka "execution" must beFalse
    }
     */
    }

    "Indexed parameters" should {
      "be one string" in withConnection { implicit c =>
        SQL("set-str ?").copy(argsInitialOrder = List("p")).
          on('p -> "string") aka "query" must beLike {
            case q @ SimpleSql( // check accross construction
              SqlQuery("set-str ?", List("p"), _),
              Seq(("p", Val("string", _))), _) =>

              // execute = false: update ok but returns no resultset
              // see java.sql.PreparedStatement#execute
              q.execute() aka "execution" must beFalse
          }
      }

      "be one big decimal" in withConnection { implicit c =>
        SQL("set-bg {p}").copy(argsInitialOrder = List("p")).
          on('p -> bg1).execute() must beFalse
      }

      // TODO: single set for other types

      "be multiple (string, big decimal)" in withConnection { implicit c =>
        SQL("set-s-bg ?, ?").copy(argsInitialOrder = List("a", "b")).
          onParams(pv("string"), pv(bg1)) aka "query" must beLike {
            case q @ SimpleSql(
              SqlQuery("set-s-bg ?, ?", List("a", "b"), _),
              Seq(("a", Val("string", _)), ("b", Val(bg1, _))), _) =>
              q.execute() aka "execution" must beFalse

          }
      }

      "be defined string option" in withConnection { implicit c =>
        SQL("set-str-opt ?").copy(argsInitialOrder = List("p")).
          onParams(pv(Some("string"))).execute() aka "execution" must beFalse
      }

      "be defined big decimal option" in withConnection { implicit c =>
        SQL("set-bg-opt ?").copy(argsInitialOrder = List("p")).
          onParams(pv(Some(bg1))).execute() aka "execute" must beFalse

      }

      "not be set if placeholder not found in SQL" in withConnection {
        implicit c =>
          SQL("no-param-placeholder").onParams(pv("not set")).execute().
            aka("execution") must beFalse

      }

      "be partially set if matching placeholder is missing for second one" in {
        withConnection { implicit c =>
          SQL("no-snd-placeholder ?").copy(argsInitialOrder = List("p")).
            onParams(pv("first"), pv("second")).execute() must beFalse

        }
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

  def pv[A](v: A)(implicit t: ToStatement[A]) = Val(v, t)
}
