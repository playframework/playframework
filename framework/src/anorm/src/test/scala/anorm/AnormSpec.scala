package anorm

import org.specs2.mutable.Specification

import anorm.{ ParameterValue => Val }

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
    val jbg1 = new java.math.BigDecimal(1.234d)
    val sbg1 = BigDecimal(jbg1)
    val date = new java.util.Date()
    val SqlStr = ParameterMetaData.Str
    val SqlBool = ParameterMetaData.Bool
    val SqlInt = ParameterMetaData.Int
    val SqlByte = ParameterMetaData.Byte
    val SqlShort = ParameterMetaData.Short
    val SqlLong = ParameterMetaData.Long
    val SqlFloat = ParameterMetaData.Float(1.23f)
    val SqlDouble = ParameterMetaData.Double(23.456d)
    val SqlTimestamp = ParameterMetaData.Timestamp
    val SqlNum1 = ParameterMetaData.Numeric(jbg1)

    def withConnection[A](ps: (String, String)*)(f: java.sql.Connection => A): A = f(connection(handleStatement withUpdateHandler {
      case UpdateExecution("set-str ?",
        DParam("string", SqlStr) :: Nil) => 1 /* case ok */
      case UpdateExecution("set-false ?",
        DParam(false, SqlBool) :: Nil) => 1 /* case ok */
      case UpdateExecution("set-true ?",
        DParam(true, SqlBool) :: Nil) => 1 /* case ok */
      case UpdateExecution("set-int ?",
        DParam(2, SqlInt) :: Nil) => 1 /* case ok */
      case UpdateExecution("set-short ?",
        DParam(3, SqlShort) :: Nil) => 1 /* case ok */
      case UpdateExecution("set-byte ?",
        DParam(4, SqlByte) :: Nil) => 1 /* case ok */
      case UpdateExecution("set-long ?",
        DParam(5l, SqlLong) :: Nil) => 1 /* case ok */
      case UpdateExecution("set-float ?",
        DParam(1.23f, SqlFloat) :: Nil) => 1 /* case ok */
      case UpdateExecution("set-double ?",
        DParam(23.456d, SqlDouble) :: Nil) => 1 /* case ok */
      case UpdateExecution("set-jbg ?",
        DParam(jbg1, SqlNum1) :: Nil) => 1 /* case ok */
      case UpdateExecution("set-sbg ?",
        DParam(sbg1, SqlNum1) :: Nil) => 1 /* case ok */
      case UpdateExecution("set-date ?",
        DParam(date, SqlTimestamp) :: Nil) => 1 /* case ok */
      case UpdateExecution("set-s-jbg ?, ?",
        DParam("string", SqlStr) :: DParam(jbg1, SqlNum1) :: Nil) => 1 /* ok */
      case UpdateExecution("set-s-sbg ?, ?",
        DParam("string", SqlStr) :: DParam(sbg1, SqlNum1) :: Nil) => 1 /* ok */
      case UpdateExecution("reorder-s-jbg ?, ?",
        DParam(jbg1, SqlNum1) :: DParam("string", SqlStr) :: Nil) => 1 /* ok */
      case UpdateExecution("set-str-opt ?",
        DParam("string", SqlStr) :: Nil) => 1 /* case ok */
      case UpdateExecution("set-jbg-opt ?",
        DParam(jbg1, SqlNum1) :: Nil) => 1 /* case ok */
      case UpdateExecution("set-sbg-opt ?",
        DParam(sbg1, SqlNum1) :: Nil) => 1 /* case ok */
      case UpdateExecution("set-none ?", DParam(null, _) :: Nil) => 1 /* ok */
      case UpdateExecution("no-param-placeholder", Nil)          => 1 /* ok */
      case UpdateExecution("no-snd-placeholder ?",
        DParam("first", SqlStr) :: Nil) => 1 /* case ok */

    }, ps: _*))

    "Named parameters" should {
      "be one string" in withConnection() { implicit c =>
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

      "be boolean true" in withConnection() { implicit c =>
        SQL("set-true {p}").on('p -> true).execute() must beFalse
      }

      "be boolean false" in withConnection() { implicit c =>
        SQL("set-false {p}").on('p -> false).execute() must beFalse
      }

      "be int" in withConnection() { implicit c =>
        SQL("set-int {p}").on('p -> 2).execute() must beFalse
      }

      "be short" in withConnection() { implicit c =>
        SQL("set-short {p}").on('p -> 3.toShort).execute() must beFalse
      }

      "be byte" in withConnection() { implicit c =>
        SQL("set-byte {p}").on('p -> 4.toByte).execute() must beFalse
      }

      "be long" in withConnection() { implicit c =>
        SQL("set-long {p}").on('p -> 5l).execute() must beFalse
      }

      "be float" in withConnection() { implicit c =>
        SQL("set-float {p}").on('p -> 1.23f).execute() must beFalse
      }

      "be double" in withConnection() { implicit c =>
        SQL("set-double {p}").on('p -> 23.456d).execute() must beFalse
      }

      "be one Java big decimal" in withConnection() { implicit c =>
        SQL("set-jbg {p}").on('p -> jbg1).execute() must beFalse
      }

      "be one Scala big decimal" in withConnection() { implicit c =>
        SQL("set-sbg {p}").on('p -> sbg1).execute() must beFalse
      }

      "be one date" in withConnection() { implicit c =>
        SQL("set-date {p}").on('p -> date).execute() must beFalse
      }

      "be multiple (string, Java big decimal)" in withConnection() {
        implicit c =>
          SQL("set-s-jbg {a}, {b}").on("a" -> "string", "b" -> jbg1).
            aka("query") must beLike {
              case q @ SimpleSql(
                SqlQuery("set-s-jbg ?, ?", List("a", "b"), _),
                Seq(("a", Val("string", _)), ("b", Val(jbg1, _))), _) =>
                q.execute() aka "execution" must beFalse

            }
      }

      "be multiple (string, Scala big decimal)" in withConnection() {
        implicit c =>
          SQL("set-s-sbg {a}, {b}").on("a" -> "string", "b" -> sbg1).
            execute() aka "execution" must beFalse
      }

      "be reordered" in withConnection() { implicit c =>
        SQL("reorder-s-jbg ?, ?").copy(argsInitialOrder = List("b", "a")).
          on('a -> "string", 'b -> jbg1) aka "query" must beLike {
            case q @ SimpleSql(
              SqlQuery("reorder-s-jbg ?, ?", List("b", "a"), _),
              Seq(("a", Val("string", _)), ("b", Val(jbg1, _))), _) =>
              q.execute() aka "execution" must beFalse

          }
      }

      "be defined string option" in withConnection() { implicit c =>
        SQL("set-str-opt {p}").on('p -> Some("string")).execute().
          aka("execution") must beFalse
      }

      "be defined Java big decimal option" in withConnection() { implicit c =>
        SQL("set-jbg-opt {p}").on('p -> Some(jbg1)).
          execute() aka "execution" must beFalse

      }

      "be defined Scala big decimal option" in withConnection() { implicit c =>
        SQL("set-sbg-opt {p}").on('p -> Some(sbg1)).
          execute() aka "execution" must beFalse

      }

      "not be set if placeholder not found in SQL" in withConnection() {
        implicit c =>
          SQL("no-param-placeholder").on('p -> "not set").execute().
            aka("execution") must beFalse

      }

      "be partially set if matching placeholder is missing for second one" in {
        withConnection() { implicit c =>
          SQL("no-snd-placeholder {a}")
            .on("a" -> "first", "b" -> "second").execute() must beFalse

        }
      }

      "set null parameter from None" in withConnection(
        "acolyte.parameter.untypedNull" -> "true") { implicit c =>
          /*
         http://docs.oracle.com/javase/6/docs/api/java/sql/PreparedStatement.html#setObject%28int,%20java.lang.Object%29
         -> Note: Not all databases allow for a non-typed Null to be sent to the backend. For maximum portability, the setNull or the setObject(int parameterIndex, Object x, int sqlType) method should be used instead of setObject(int parameterIndex, Object x).
         -> Note: This method throws an exception if there is an ambiguity, for example, if the object is of a class implementing more than one of the interfaces named above. 
         */
          SQL("set-none {p}").on('p -> None).
            execute() aka "execution" must beFalse
        }
    }

    "Indexed parameters" should {
      "be one string" in withConnection() { implicit c =>
        SQL("set-str ?").copy(argsInitialOrder = "p" :: Nil).
          onParams(pv("string")) aka "query" must beLike {
            case q @ SimpleSql( // check accross construction
              SqlQuery("set-str ?", List("p"), _),
              Seq(("p", Val("string", _))), _) =>

              // execute = false: update ok but returns no resultset
              // see java.sql.PreparedStatement#execute
              q.execute() aka "execution" must beFalse
          }
      }

      "be boolean true" in withConnection() { implicit c =>
        SQL("set-true ?").copy(argsInitialOrder = "p" :: Nil).
          onParams(pv(true)).execute() must beFalse
      }

      "be boolean false" in withConnection() { implicit c =>
        SQL("set-false ?").copy(argsInitialOrder = "p" :: Nil).
          onParams(pv(false)).execute() must beFalse
      }

      "be short" in withConnection() { implicit c =>
        SQL("set-short ?").copy(argsInitialOrder = "p" :: Nil).
          onParams(pv(3.toShort)).execute() must beFalse
      }

      "be byte" in withConnection() { implicit c =>
        SQL("set-byte ?").copy(argsInitialOrder = "p" :: Nil).
          onParams(pv(4.toByte)).execute() must beFalse
      }

      "be long" in withConnection() { implicit c =>
        SQL("set-long ?").copy(argsInitialOrder = "p" :: Nil).
          onParams(pv(5l)).execute() must beFalse
      }

      "be float" in withConnection() { implicit c =>
        SQL("set-float ?").copy(argsInitialOrder = "p" :: Nil).
          onParams(pv(1.23f)).execute() must beFalse
      }

      "be double" in withConnection() { implicit c =>
        SQL("set-double ?").copy(argsInitialOrder = "p" :: Nil).
          onParams(pv(23.456d)).execute() must beFalse
      }

      "be one Java big decimal" in withConnection() { implicit c =>
        SQL("set-jbg ?").copy(argsInitialOrder = "p" :: Nil).
          onParams(pv(jbg1)).execute() must beFalse
      }

      "be one Scala big decimal" in withConnection() { implicit c =>
        SQL("set-sbg ?").copy(argsInitialOrder = "p" :: Nil).
          onParams(pv(sbg1)).execute() must beFalse
      }

      "be one date" in withConnection() { implicit c =>
        SQL("set-date ?").copy(argsInitialOrder = "p" :: Nil).
          onParams(pv(date)).execute() must beFalse
      }

      "be multiple (string, Java big decimal)" in withConnection() {
        implicit c =>
          SQL("set-s-jbg ?, ?").copy(argsInitialOrder = List("a", "b")).
            onParams(pv("string"), pv(jbg1)) aka "query" must beLike {
              case q @ SimpleSql(
                SqlQuery("set-s-jbg ?, ?", List("a", "b"), _),
                Seq(("a", Val("string", _)), ("b", Val(jbg1, _))), _) =>
                q.execute() aka "execution" must beFalse

            }
      }

      "be multiple (string, Scala big decimal)" in withConnection() {
        implicit c =>
          SQL("set-s-sbg ?, ?").copy(argsInitialOrder = List("a", "b")).
            onParams(pv("string"), pv(sbg1)).execute().
            aka("execution") must beFalse

      }

      "be defined string option" in withConnection() { implicit c =>
        SQL("set-str-opt ?").copy(argsInitialOrder = List("p")).
          onParams(pv(Some("string"))).execute() aka "execution" must beFalse
      }

      "be defined Java big decimal option" in withConnection() { implicit c =>
        SQL("set-jbg-opt ?").copy(argsInitialOrder = "p" :: Nil).
          onParams(pv(Some(jbg1))).execute() aka "execute" must beFalse

      }

      "be defined Scala big decimal option" in withConnection() { implicit c =>
        SQL("set-sbg-opt ?").copy(argsInitialOrder = "p" :: Nil).
          onParams(pv(Some(sbg1))).execute() aka "execute" must beFalse

      }

      "set null parameter from None" in withConnection(
        "acolyte.parameter.untypedNull" -> "true") { implicit c =>
          /*
         http://docs.oracle.com/javase/6/docs/api/java/sql/PreparedStatement.html#setObject%28int,%20java.lang.Object%29
         */
          SQL("set-none ?").copy(argsInitialOrder = "p" :: Nil).
            onParams(pv(None)).execute() aka "execution" must beFalse
        }

      "not be set if placeholder not found in SQL" in withConnection() {
        implicit c =>
          SQL("no-param-placeholder").onParams(pv("not set")).execute().
            aka("execution") must beFalse

      }

      "be partially set if matching placeholder is missing for second one" in {
        withConnection() { implicit c =>
          SQL("no-snd-placeholder ?").copy(argsInitialOrder = List("p")).
            onParams(pv("first"), pv("second")).execute() must beFalse

        }
      }
    }
  }

  "Column" >> {
    val bd = new java.math.BigDecimal("34.5679")

    "mapped as Java big decimal" should {
      "be parsed from big decimal" in withQueryResult(
        RowLists.bigDecimalList :+ bd) { implicit con =>

          SQL("SELECT bd").as(SqlParser.scalar[java.math.BigDecimal].single).
            aka("parsed big decimal") must_== bd
        }

      "be parsed from double" in withQueryResult(RowLists.doubleList :+ 1.35d) {
        implicit con =>
          SQL("SELECT bd").as(SqlParser.scalar[java.math.BigDecimal].single).
            aka("parsed double") must_== java.math.BigDecimal.valueOf(1.35d)

      }

      "be parsed from long" in withQueryResult(RowLists.longList :+ 5l) {
        implicit con =>
          SQL("SELECT bd").as(SqlParser.scalar[java.math.BigDecimal].single).
            aka("parsed long") must_== java.math.BigDecimal.valueOf(5l)

      }
    }

    "mapped as Scala big decimal" should {
      "be parsed from big decimal" in withQueryResult(
        RowLists.bigDecimalList :+ bd) { implicit con =>

          SQL("SELECT bd").as(SqlParser.scalar[BigDecimal].single).
            aka("parsed big decimal") must_== BigDecimal(bd)
        }

      "be parsed from double" in withQueryResult(RowLists.doubleList :+ 1.35d) {
        implicit con =>
          SQL("SELECT bd").as(SqlParser.scalar[BigDecimal].single).
            aka("parsed double") must_== BigDecimal(1.35d)

      }

      "be parsed from long" in withQueryResult(RowLists.longList :+ 5l) {
        implicit con =>
          SQL("SELECT bd").as(SqlParser.scalar[BigDecimal].single).
            aka("parsed long") must_== BigDecimal(5l)

      }
    }

    val bi = new java.math.BigInteger("1234")

    "mapped as Java big integer" should {
      "be parsed from big integer" in withQueryResult(
        rowList1(classOf[java.math.BigInteger]) :+ bi) { implicit con =>
          // Useless as proper resultset won't return BigInteger

          SQL("SELECT bi").as(SqlParser.scalar[java.math.BigInteger].single).
            aka("parsed big integer") must_== bi
        }

      "be parsed from long" in withQueryResult(RowLists.longList :+ 5l) {
        implicit con =>
          SQL("SELECT bi").as(SqlParser.scalar[java.math.BigInteger].single).
            aka("parsed long") must_== java.math.BigInteger.valueOf(5l)

      }

      "be parsed from int" in withQueryResult(RowLists.intList :+ 2) {
        implicit con =>
          SQL("SELECT bi").as(SqlParser.scalar[java.math.BigInteger].single).
            aka("parsed int") must_== java.math.BigInteger.valueOf(2)

      }
    }

    "mapped as Scala big integer" should {
      "be parsed from big integer" in withQueryResult(
        rowList1(classOf[java.math.BigInteger]) :+ bi) { implicit con =>
          // Useless as proper resultset won't return BigInteger

          SQL("SELECT bi").as(SqlParser.scalar[BigInt].single).
            aka("parsed big integer") must_== BigInt(bi)
        }

      "be parsed from long" in withQueryResult(RowLists.longList :+ 5l) {
        implicit con =>
          SQL("SELECT bi").as(SqlParser.scalar[BigInt].single).
            aka("parsed long") must_== BigInt(5l)

      }

      "be parsed from int" in withQueryResult(RowLists.intList :+ 2) {
        implicit con =>
          SQL("SELECT bi").as(SqlParser.scalar[BigInt].single).
            aka("parsed int") must_== BigInt(2)

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
    f(connection(handleQuery { _ => r }))

  def pv[A](v: A)(implicit t: ToStatement[A]) = Val(v, t)
}
