package anorm

import acolyte.jdbc.QueryResult
import acolyte.jdbc.AcolyteDSL.{ connection, handleQuery }
import acolyte.jdbc.RowLists.{ rowList1, rowList2, stringList }
import acolyte.jdbc.Implicits._

object SqlResultSpec extends org.specs2.mutable.Specification with H2Database {
  "SQL result" title

  "For-comprehension over result" should {
    "fail when there is no data" in {
      withQueryResult("scalar") { implicit c =>
        lazy val parser = for {
          a <- SqlParser.str("col1")
          b <- SqlParser.int("col2")
        } yield (a -> b)

        SQL("SELECT * FROM test") as parser.single must throwA[Exception](
          message = "col1 not found")
      }
    }

    "return expected mandatory single result" in withQueryResult(
      rowList2(classOf[String] -> "a", classOf[Int] -> "b") :+ ("str", 2)) {
        implicit c =>
          lazy val parser = for {
            a <- SqlParser.str("a")
            b <- SqlParser.int("b")
          } yield (a -> b)

          SQL("SELECT * FROM test") as parser.single must_== ("str" -> 2)
      }

    "fail with sub-parser when there is no data" in {
      withQueryResult("scalar") { implicit c =>
        lazy val sub = for {
          b <- SqlParser.str("b")
          c <- SqlParser.int("c")
        } yield (b -> c)

        lazy val parser = for {
          a <- SqlParser.str("col1")
          bc <- sub
        } yield Tuple3(a, bc._1, bc._2)

        SQL("SELECT * FROM test") as parser.single must throwA[Exception](
          message = "col1 not found")
      }
    }

    "fail when column is missing for sub-parser" in withQueryResult(
      rowList1(classOf[String] -> "a") :+ "str") { implicit c =>
        lazy val sub = for {
          b <- SqlParser.str("col2")
          c <- SqlParser.int("col3")
        } yield (b -> c)

        lazy val parser = for {
          a <- SqlParser.str("a")
          bc <- sub
        } yield Tuple3(a, bc._1, bc._2)

        SQL("SELECT * FROM test") as parser.single must throwA[Exception](
          message = "col2 not found")
      }

    "return None from optional sub-parser" in withQueryResult(
      rowList1(classOf[String] -> "a") :+ "str") { implicit c =>
        lazy val sub = for {
          b <- SqlParser.str("b")
          c <- SqlParser.int("c")
        } yield (b -> c)

        lazy val parser = for {
          a <- SqlParser.str("a")
          bc <- sub.?
        } yield (a -> bc)

        SQL("SELECT * FROM test") as parser.single must_== ("str" -> None)
      }
  }

  "Column" should {
    "be found in result" in withQueryResult(
      rowList1(classOf[Float] -> "f") :+ 1.2f) { implicit c =>

        SQL("SELECT f") as SqlParser.contains("f", 1.2f).single must_== (())
      }

    "not be found in result when value not matching" in withQueryResult(
      rowList1(classOf[Float] -> "f") :+ 1.2f) { implicit c =>

        SQL("SELECT f") as SqlParser.contains("f", 2.34f).single must throwA[Exception]("SqlMappingError\\(Row doesn't contain a column: f with value 2\\.34\\)")
      }

    "not be found in result when column missing" in withQueryResult(
      rowList1(classOf[Float] -> "f") :+ 1.2f) { implicit c =>

        SQL("SELECT f") as SqlParser.contains("x", 1.2f).single must throwA[Exception]("x not found, available columns")
      }

  }

  "Column" should {
    "be matching in result" in withQueryResult(
      rowList1(classOf[Float] -> "f") :+ 1.2f) { implicit c =>

        SQL("SELECT f") as SqlParser.matches("f", 1.2f).single must beTrue
      }

    "not be found in result when value not matching" in withQueryResult(
      rowList1(classOf[Float] -> "f") :+ 1.2f) { implicit c =>

        SQL("SELECT f") as SqlParser.matches("f", 2.34f).single must beFalse
      }

    "not be found in result when column missing" in withQueryResult(
      rowList1(classOf[Float] -> "f") :+ 1.2f) { implicit c =>

        SQL("SELECT f") as SqlParser.matches("x", 1.2f).single must beFalse
      }
  }

  "Collecting" should {
    sealed trait EnumX
    object Xa extends EnumX
    object Xb extends EnumX
    val pf: PartialFunction[String, EnumX] = {
      case "XA" => Xa
      case "XB" => Xb
    }

    "return Xa object" in withQueryResult(stringList :+ "XA") { implicit c =>
      SQL"SELECT str".as(SqlParser.str(1).collect("ERR")(pf).single).
        aka("collected") must_== Xa
    }

    "return Xb object" in withQueryResult(stringList :+ "XB") { implicit c =>
      SQL"SELECT str".as(SqlParser.str(1).collect("ERR")(pf).single).
        aka("collected") must_== Xb
    }

    "fail" in withQueryResult(stringList :+ "XC") { implicit c =>
      SQL"SELECT str".as(SqlParser.str(1).collect("ERR")(pf).single).
        aka("collecting") must throwA[Exception]("SqlMappingError\\(ERR\\)")
    }
  }

  "Streaming" should {
    "release resources" in withQueryResult(
      stringList :+ "A" :+ "B" :+ "C") { implicit c =>

      val res: SqlQueryResult = SQL"SELECT str".executeQuery()
      var closed = false
      val probe = resource.managed(
        new java.io.Closeable { def close() = closed = true })

      var i = 0      
      val stream: Stream[Int] = res.copy(resultSet = 
        res.resultSet.and(probe).map(_._1)).apply() map { r => i = i+1; i }

      stream aka "streaming" must_== List(1, 2, 3) and (
        closed aka "resource release" must beTrue) and (
        i aka "row count" must_== 3)

    }

    "release resources on exception" in withQueryResult(
      stringList :+ "A" :+ "B" :+ "C") { implicit c =>

      val res: SqlQueryResult = SQL"SELECT str".executeQuery()
      var closed = false
      val probe = resource.managed(
        new java.io.Closeable { def close() = closed = true })

      var i = 0      
      val stream = res.copy(resultSet = res.resultSet.and(probe).map(_._1)).
        apply() map { _ => if (i == 1) sys.error("Unexpected") else i = i+1 }
      
      stream.toList aka "streaming" must throwA[Exception]("Unexpected") and (
        closed aka("resource release") must beTrue) and (
        i aka "row count" must_== 1)

    }

    "release resources on partial read" in withQueryResult(
      stringList :+ "A" :+ "B" :+ "C") { implicit c =>

      val res: SqlQueryResult = SQL"SELECT str".executeQuery()
      var closed = false
      val probe = resource.managed(
        new java.io.Closeable { def close() = closed = true })

      var i = 0      
      val stream: Stream[Int] = res.copy(resultSet = 
        res.resultSet.and(probe).map(_._1)).apply() map { r => i = i+1; i }

      stream.head aka "streaming" must_== 1 and (
        closed aka "resource release" must beTrue) and (
        i aka "row count" must_== 1)

    }
  }

  "Aggregation over all rows" should {
    "release resources" in withQueryResult(
      stringList :+ "A" :+ "B" :+ "C") { implicit c =>

      val res: SqlQueryResult = SQL"SELECT str".executeQuery()
      var closed = false
      val probe = resource.managed(
        new java.io.Closeable { def close() = closed = true })

      var i = 0      
      lazy val agg = res.copy(resultSet = 
        res.resultSet.and(probe).map(_._1)).fold(List[Int]()) { 
        (l, _) => i = i+1; l :+ i
      }

      agg aka "aggregation" must_== Right(List(1, 2, 3)) and (
        closed aka "resource release" must beTrue) and (
        i aka "row count" must_== 3)

    }

    "release resources on exception" in withQueryResult(
      stringList :+ "A" :+ "B" :+ "C") { implicit c =>

      val res: SqlQueryResult = SQL"SELECT str".executeQuery()
      var closed = false
      val probe = resource.managed(
        new java.io.Closeable { def close() = closed = true })

      var i = 0      
      lazy val agg = res.copy(resultSet = res.resultSet.and(probe).map(_._1)).
        fold(List[Int]()) { (l, _) => 
          if (i == 1) sys.error("Unexpected") else { i = i +1; l :+ i }
        } 

      agg aka "aggregation" must beLike {
        case Left(err :: Nil) =>
          err.getMessage aka "failure" must_== "Unexpected"
      } and (closed aka("resource release") must beTrue) and (
        i aka "row count" must_== 1)

    }
  }

  "Aggregation over variable number of rows" should {
    "release resources" in withQueryResult(
      stringList :+ "A" :+ "B" :+ "C") { implicit c =>

      val res: SqlQueryResult = SQL"SELECT str".executeQuery()
      var closed = false
      val probe = resource.managed(
        new java.io.Closeable { def close() = closed = true })

      var i = 0      
      lazy val agg = res.copy(resultSet = 
        res.resultSet.and(probe).map(_._1)).foldWhile(List[Int]()) { 
        (l, _) => i = i+1; (l :+ i) -> true
      }

      agg aka "aggregation" must_== Right(List(1, 2, 3)) and (
        closed aka "resource release" must beTrue) and (
        i aka "row count" must_== 3)

    }

    "release resources on exception" in withQueryResult(
      stringList :+ "A" :+ "B" :+ "C") { implicit c =>

      val res: SqlQueryResult = SQL"SELECT str".executeQuery()
      var closed = false
      val probe = resource.managed(
        new java.io.Closeable { def close() = closed = true })

      var i = 0      
      lazy val agg = res.copy(resultSet = res.resultSet.and(probe).map(_._1)).
        foldWhile(List[Int]()) { (l, _) => 
          if (i == 1) sys.error("Unexpected") else { 
            i = i +1; (l :+ i) -> true 
          }
        } 

      agg aka "aggregation" must beLike {
        case Left(err :: Nil) =>
          err.getMessage aka "failure" must_== "Unexpected"
      } and (closed aka "resource release" must beTrue) and (
        i aka "row count" must_== 1)

    }

    "stop after second row & release resources" in withQueryResult(
      stringList :+ "A" :+ "B" :+ "C") { implicit c =>

      val res: SqlQueryResult = SQL"SELECT str".executeQuery()
      var closed = false
      val probe = resource.managed(
        new java.io.Closeable { def close() = closed = true })

      var i = 0      
      lazy val agg = res.copy(resultSet = res.resultSet.and(probe).map(_._1)).
        foldWhile(List[Int]()) { (l, _) => 
          if (i == 2) (l, false) else { i = i +1; (l :+ i) -> true }
        } 

      agg aka "aggregation" must_== Right(List(1, 2)) and (
        closed aka "resource release" must beTrue) and (
        i aka "row count" must_== 2)

    }
  }

  "SQL warning" should {
    "not be there on success" in withQueryResult(stringList :+ "A") { 
      implicit c =>

      SQL"SELECT str".executeQuery().statementWarning.
        aka("statement warning") must beNone

    }

    "be handled from executed query" in withQueryResult(
      QueryResult.Nil.withWarning("Warning for test-proc-2")) { implicit c =>

        SQL("EXEC stored_proc({param})")
          .on("param" -> "test-proc-2").executeQuery()
          .statementWarning aka "statement warning" must beSome.which { warn =>
            warn.getMessage aka "message" must_== "Warning for test-proc-2"
          }
      }
  }

  "Column value" should {
    val foo = s"alias-${System.identityHashCode(this)}"
    val (v1, v2) = (s"1-$foo", s"2-$foo")

    "be found either by name and alias" in withTestDB(v1) { implicit c =>
      SQL"SELECT foo AS AL, bar FROM test1".as(SqlParser.str("foo").single).
        aka("by name") must_== v1 and (SQL"SELECT foo AS AL, bar FROM test1".
          as(SqlParser.getAliased[String]("AL").single).
          aka("by alias") must_== v1)

    }

    "not be found without alias parser" in withTestDB(v2) { implicit c =>
      SQL"SELECT foo AS AL, bar FROM test1".as(SqlParser.str("foo").single).
        aka("by name") must_== v2 and (SQL"SELECT foo AS AL, bar FROM test1".
          as(SqlParser.str("AL").single).aka("by alias") must throwA[Exception](
            "AL not found, available columns : TEST1.FOO, AL, TEST1.BAR, BAR"))

    }

    def withTestDB[T](foo: String)(f: java.sql.Connection => T): T =
      withH2Database { implicit c =>
        createTest1Table()
        SQL("insert into test1(id, foo, bar) values ({id}, {foo}, {bar})").
          on('id -> 10L, 'foo -> foo, 'bar -> 20).execute()

        f(c)
      }
  }

  def withQueryResult[A](r: QueryResult)(f: java.sql.Connection => A): A =
    f(connection(handleQuery { _ => r }))

}
