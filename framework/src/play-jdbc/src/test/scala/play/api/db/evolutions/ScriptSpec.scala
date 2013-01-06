package play.api.db.evolutions

import org.specs2.mutable.Specification

object ScriptSpec extends Specification {
  "Script" should {

    "separate SQL into semicolon-delimited statements" in {
      val statements = IndexedSeq("FIRST", "SECOND", "THIRD", "FOURTH")
      
      val scriptStatements = ScriptSansEvolution(s"""
        ${statements(0)};

        ${statements(1)}; ${statements(2)};${statements(3)};
      """).statements

      scriptStatements.toList must beEqualTo(statements.toList)
    }

    "not delimit statements on double-semicolons, rather escaping them to a single semicolon" in {
      val statements = IndexedSeq(
        "SELECT * FROM punctuation WHERE character = ';'", 
        "DROP the_beat"
      )

      val statementsWithEscapeSequence = statements.map(_.replace(";", ";;"))

      val scriptStatements = ScriptSansEvolution(s"""
        ${statementsWithEscapeSequence(0)}; 
        ${statementsWithEscapeSequence(1)};
      """).statements

      scriptStatements.toList must beEqualTo(statements.toList)
    }

  }


  private case class ScriptSansEvolution(sql: String) extends Script {
    override val evolution = Evolution(0, "", "")
  }
}
