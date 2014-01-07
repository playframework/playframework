package anorm

import org.specs2.mutable.Specification

import acolyte.Acolyte.{ connection, handleQuery }
import acolyte.{ QueryResult, RowLists }
import RowLists.{ rowList2, rowList1, stringList }
import acolyte.Implicits._

object RowSpec extends Specification {
  "Row" title

  "List of column values" should {
    "be expected one" in withQueryResult(rowList2(
      classOf[String] -> "foo", classOf[Int] -> "bar").append("row1", 100)) {
      implicit c =>
        SQL("SELECT * FROM test").map(_.asList).single.
          aka("column list") must_== List("row1", 100)
    }

    "keep null if not nullable" in withQueryResult(stringList append null) {
      implicit c =>
        SQL("SELECT 1").map(_.asList).single.
          aka("column list") must_== List(null)

    }

    // TODO: nullable specs
  }

  "Column dictionary" should {
    "be expected one" in withQueryResult(rowList2(
      classOf[String] -> "foo", classOf[Int] -> "bar").append("row1", 100)) {
      implicit c =>
        SQL("SELECT * FROM test").map(_.asMap).single.
          aka("column map") must_== Map(".foo" -> "row1", ".bar" -> 100)

    }

    "keep null if not nullable" in withQueryResult(
      rowList1(classOf[String] -> "foo").append(null)) { implicit c =>
        SQL("SELECT 1").map(_.asMap).single.
          aka("column map") must_== Map(".foo" -> null)

      }

    // TODO: nullable specs
  }

  // ---

  def withQueryResult[A](r: QueryResult)(f: java.sql.Connection => A): A =
    f(connection(handleQuery { _ => r }))

}
