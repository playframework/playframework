package anorm

import scala.util.Try

import acolyte.jdbc.AcolyteDSL.withQueryResult
import acolyte.jdbc.RowLists.{ rowList2, rowList1, stringList }
import acolyte.jdbc.Implicits._

object RowSpec extends org.specs2.mutable.Specification {
  "Row" title

  "List of column values" should {
    "be expected one" in withQueryResult(rowList2(
      classOf[String] -> "foo", classOf[Int] -> "bar") :+ ("row1", 100)) {
      implicit c =>
        SQL("SELECT * FROM test").map(_.asList).single.
          aka("column list") must_== List("row1", 100)
    }

    "keep null if not nullable" in withQueryResult(stringList :+ null) {
      implicit c =>
        SQL("SELECT 1").map(_.asList).single.
          aka("column list") must_== List(null)

    }

    "turn null into None if nullable" in withQueryResult(
      stringList.withNullable(1, true) :+ null) { implicit c =>
        SQL("SELECT 1").map(_.asList).single.
          aka("column list") must_== List(None)

      }

    "turn value into Some(X) if nullable" in withQueryResult(
      stringList.withNullable(1, true) :+ "str") { implicit c =>
        SQL("SELECT 1").map(_.asList).single.
          aka("column list") must_== List(Some("str"))

      }
  }

  "Column dictionary" should {
    "be expected one" in withQueryResult(rowList2(
      classOf[String] -> "foo", classOf[Int] -> "bar") :+ ("row1", 100)) {
      implicit c =>
        SQL("SELECT * FROM test").map(_.asMap).single.
          aka("column map") must_== Map(".foo" -> "row1", ".bar" -> 100)

    }

    "keep null if not nullable" in withQueryResult(
      rowList1(classOf[String] -> "foo") :+ null) { implicit c =>
        SQL("SELECT 1").map(_.asMap).single.
          aka("column map") must_== Map(".foo" -> null)

      }

    "turn null into None if nullable" in withQueryResult(
      rowList1(classOf[String] -> "foo").withNullable(1, true) :+ null) {
        implicit c =>
          SQL("SELECT 1").map(_.asMap).single.
            aka("column map") must_== Map(".foo" -> None)

      }

    "turn value into Some(X) if nullable" in withQueryResult(
      rowList1(classOf[String] -> "foo").withNullable(1, true) :+ "str") {
        implicit c =>
          SQL("SELECT 1").map(_.asMap).single.
            aka("column map") must_== Map(".foo" -> Some("str"))

      }
  }

  "Column" should {
    "be extracted by name" in withQueryResult(
      rowList1(classOf[String] -> "foo") :+ "byName") { implicit c =>
        SQL("SELECT *").map(_.apply[String]("foo")).single.
          aka("column by name") must_== "byName"
      }

    "be extracted by position" in withQueryResult(stringList :+ "byPos") {
      implicit c =>
        SQL("SELECT *").map(_.apply[String](1)).single.
          aka("column by name") must_== "byPos"
    }
  }

  "Row" should {
    "successfully be parsed" in withQueryResult(rowList2(
      classOf[String] -> "foo", classOf[Int] -> "num") :+ ("str", 2)) {
      implicit c =>

        SQL"SELECT *".withResult(_.map(_.row.as(
          SqlParser.str("foo") ~ SqlParser.int(2) map {
            case a ~ b => b -> a
          }))) aka "streaming result" must beRight[Option[Try[(Int, String)]]].
          which {
            _ aka "first row" must beSome[Try[(Int, String)]].which {
              _ aka "parsed value" must beSuccessfulTry(2 -> "str")
            }
          }
    }
  }
}
