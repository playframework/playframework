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
        "SELECT * FROM schema.table          WHERE (name = ? AND category = ?) OR id = ?" -> List("name", "cat"))
    }

    "detect missing query parameter" in withQueryResult(stringList :+ "test") {
      implicit con =>
        SQL("SELECT name FROM t WHERE id = {id}").apply().
          aka("query") must throwA[java.util.NoSuchElementException]
    }

    "detect missing update parameter" in withQueryResult(stringList :+ "test") {
      implicit con =>
        SQL("EXEC proc {lang}, {c}").asBatch.addBatch("lang" -> "en").
          execute() aka "execute" must throwA[java.util.NoSuchElementException]
    }
  }
}
