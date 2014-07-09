package anorm

import acolyte.jdbc.QueryResult
import acolyte.jdbc.AcolyteDSL.{ connection, handleQuery }
import acolyte.jdbc.RowLists.{ rowList1, rowList2, stringList }
import acolyte.jdbc.Implicits._

object SqlResultSpec extends org.specs2.mutable.Specification {
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

  def withQueryResult[A](r: QueryResult)(f: java.sql.Connection => A): A =
    f(connection(handleQuery { _ => r }))

}
