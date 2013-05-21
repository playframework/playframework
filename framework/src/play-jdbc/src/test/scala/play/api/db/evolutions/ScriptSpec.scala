package play.api.db.evolutions

import org.specs2.mutable.Specification

object ScriptSpec extends Specification {
  "Script.statements" should {

    "separate SQL into semicolon-delimited statements" in {
      val statements = IndexedSeq("FIRST", "SECOND", "THIRD", "FOURTH")
      
      val scriptStatements = ScriptSansEvolution(s"""
        ${statements(0)};

        ${statements(1)}; ${statements(2)};${statements(3)};""").statements

      scriptStatements.toList must beEqualTo(statements.toList)
    }

    "not delimit statements on double-semicolons, rather escaping them to a single semicolon" in {
      val statements = IndexedSeq(
        "SELECT * FROM punctuation WHERE characters = ';' OR characters = ';;'", 
        "DROP the_beat"
      )

      // double the semicolons
      val statementsWithEscapeSequence = statements.map(_.replace(";", ";;"))

      val scriptStatements = ScriptSansEvolution(s"""
        ${statementsWithEscapeSequence(0)}; 
        ${statementsWithEscapeSequence(1)};""").statements

      scriptStatements.toList must beEqualTo(statements.toList)
    }

    "not produce an empty-string trailing statement if the script ends with a new-line" in {
      val statement = "SELECT cream_filling FROM twinkies"

      val scriptStatements = ScriptSansEvolution(s"""
        $statement;
      """).statements

      scriptStatements.toList must beEqualTo(List(statement))
    }

  }  


  private case class ScriptSansEvolution(sql: String) extends Script {
    override val evolution = Evolution(0, "", "")
  }


  "Conflicts" should {

    "not be noticed if there aren't any" in {

      val downRest = (9 to 1).reverse.map(i=> Evolution(i, s"DummySQLUP$i",s"DummySQLDOWN$i"))
      val upRest = downRest        

      val (conflictingDowns, conflictingUps) = Evolutions.conflictings(downRest, upRest)

      conflictingDowns.size must beEqualTo(0)
      conflictingUps.size must beEqualTo(0)
    }

    "be noticed on the most recent one" in {

      val downRest = (1 to 9).reverse.map(i=> Evolution(i, s"DummySQLUP$i",s"DummySQLDOWN$i"))
      val upRest = Evolution(9, "DifferentDummySQLUP", "DifferentDummySQLDOWN") +: (1 to 8).reverse.map(i=> Evolution(i, s"DummySQLUP$i",s"DummySQLDOWN$i"))  
      
      val (conflictingDowns, conflictingUps) = Evolutions.conflictings(downRest, upRest)


      conflictingDowns.size must beEqualTo(1)
      conflictingUps.size must beEqualTo(1)
      conflictingDowns(0).revision must beEqualTo(9)
      conflictingUps(0).revision must beEqualTo(9)
    }

    "be noticed in the middle" in {

      val downRest = (1 to 9).reverse.map(i=> Evolution(i, s"DummySQLUP$i",s"DummySQLDOWN$i"))
      val upRest = (6 to 9).reverse.map(i=> Evolution(i, s"DummySQLUP$i",s"DummySQLDOWN$i")) ++: Evolution(5, "DifferentDummySQLUP", "DifferentDummySQLDOWN") +: (1 to 4).reverse.map(i=> Evolution(i, s"DummySQLUP$i",s"DummySQLDOWN$i"))  
      
      val (conflictingDowns, conflictingUps) = Evolutions.conflictings(downRest, upRest)

      conflictingDowns.size must beEqualTo(5)
      conflictingUps.size must beEqualTo(5)
      conflictingDowns(0).revision must beEqualTo(9)
      conflictingUps(0).revision must beEqualTo(9)
      conflictingDowns(4).revision must beEqualTo(5)
      conflictingUps(4).revision must beEqualTo(5)
    }

    "be noticed on the first" in {

      val downRest = (1 to 9).reverse.map(i=> Evolution(i, s"DummySQLUP$i",s"DummySQLDOWN$i"))
      val upRest = (2 to 9).reverse.map(i=> Evolution(i, s"DummySQLUP$i",s"DummySQLDOWN$i")) ++: List(Evolution(1, "DifferentDummySQLUP", "DifferentDummySQLDOWN"))
      
      val (conflictingDowns, conflictingUps) = Evolutions.conflictings(downRest, upRest)

      conflictingDowns.size must beEqualTo(9)
      conflictingUps.size must beEqualTo(9)
      conflictingDowns(0).revision must beEqualTo(9)
      conflictingUps(0).revision must beEqualTo(9)
      conflictingDowns(8).revision must beEqualTo(1)
      conflictingUps(8).revision must beEqualTo(1)
    }

  }
}
