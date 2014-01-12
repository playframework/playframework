package anorm

import acolyte.QueryResult
import acolyte.Acolyte.{ connection, handleQuery }
import acolyte.RowLists.{ rowList1, rowList2 }
import acolyte.Implicits._

object SqlResultSpec extends org.specs2.mutable.Specification {
  "SQL result" title

  "SQL result" should {
    "work with for-comprehension" >> {
      "failing when there is no data" in {
        withQueryResult("scalar") { implicit c =>
          lazy val parser = for {
            a <- SqlParser.str("col1")
            b <- SqlParser.int("col2")
          } yield (a -> b)

          SQL("SELECT * FROM test") as parser.single must throwA[Exception](
            message = "col1 not found")
        }
      }

      "returning expected mandatory single result" in withQueryResult(
        rowList2(classOf[String] -> "a", classOf[Int] -> "b") :+ ("str", 2)) {
          implicit c =>
            lazy val parser = for {
              a <- SqlParser.str("a")
              b <- SqlParser.int("b")
            } yield (a -> b)

            SQL("SELECT * FROM test") as parser.single must_== ("str" -> 2)
        }

      "failing with sub-parser when there is no data" in {
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

      "failing when column is missing for sub-parser" in withQueryResult(
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

      "returning None from optional sub-parser" in withQueryResult(
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
  }

  def withQueryResult[A](r: QueryResult)(f: java.sql.Connection => A): A =
    f(connection(handleQuery { _ => r }))

}
