package anorm

import acolyte.RowLists.stringList
import acolyte.Acolyte.withQueryResult
import acolyte.Implicits._

object StatementParserSpec extends org.specs2.mutable.Specification {
  "SQL statement parser" title

  "Statement" should {
    "be parsed with 'name' and 'cat' parameters" in {
      SqlStatementParser.parse("""
        SELECT * FROM schema.table 
        WHERE (name = {name} AND category = {cat}) OR id = ?
      """) aka "updated statement and parameters" mustEqual (
        "SELECT * FROM schema.table          WHERE (name = %s AND category = %s) OR id = ?" -> List("name", "cat"))
    }

    "detect missing query parameter" in withQueryResult(stringList :+ "test") {
      implicit con =>
        SQL("SELECT name FROM t WHERE id = {id}").apply().
          aka("query") must throwA[java.util.NoSuchElementException]
    }
  }

  "Value" should {
    def frag[A](v: A)(implicit c: ToSql[A] = null): (String, Int) =
      Option(c).fold("?" -> 1)(_.fragment(v))

    "give single-value '?' SQL fragment" in {
      frag("str").aka("SQL fragment") mustEqual ("?" -> 1)
    }

    "give multi-value '?, ?, ?' SQL fragment" in {
      frag(Seq("A", "B", "C")) aka "SQL fragment" mustEqual ("?, ?, ?" -> 3)
    }

    "give multi-value 'x=? OR x=? OR x=?' SQL fragment" in {
      frag(SeqParameter(Seq("A", "B", "C"), " OR ", "x=")).
        aka("SQL fragment") mustEqual ("x=? OR x=? OR x=?" -> 3)
    }
  }

  "Rewriting" should {
    "return some prepared query with updated statement" in {
      Sql.rewrite("SELECT * FROM t WHERE c IN (%s) AND id = %s", "?, ?") must {
        beSome.which { rewrited =>
          (rewrited aka "first rewrite" mustEqual (
            "SELECT * FROM t WHERE c IN (?, ?) AND id = %s")).
            and(Sql.rewrite(rewrited, "?") aka "second rewrite" must beSome(
              "SELECT * FROM t WHERE c IN (?, ?) AND id = ?"))
        }
      }
    }

    "return no prepared query" in {
      Sql.rewrite("SELECT * FROM Test WHERE id = ?", "x").
        aka("rewrited") must beNone
    }
  }
}
