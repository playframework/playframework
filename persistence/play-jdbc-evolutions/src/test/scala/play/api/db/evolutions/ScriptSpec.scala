/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db.evolutions

import org.specs2.mutable.Specification

class ScriptSpec extends Specification {
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

    "not split on semicolons between !split-semicolon never and always" in {
      val scriptStatements = ScriptSansEvolution(
        Seq(
          "DROP PROCEDURE IF EXISTS answer;",
          "CREATE PROCEDURE answer()",
          "-- !split-semicolon: never",
          "BEGIN",
          "  DECLARE x INT;",
          "  SET x = 42;",
          "-- !split-semicolon: always",
          "END;",
          "SELECT 1; SELECT 2;"
        ).mkString("\n")
      ).statements

      scriptStatements.toList must beEqualTo(
        List(
          "DROP PROCEDURE IF EXISTS answer",
          "CREATE PROCEDURE answer()\nBEGIN\n  DECLARE x INT;\n  SET x = 42;\nEND",
          "SELECT 1",
          "SELECT 2"
        )
      )
    }

    "not split on any semicolon until the end of the script with !split-semicolon never" in {
      val scriptStatements =
        ScriptSansEvolution("SELECT 1;\n-- !split-semicolon: never\nSELECT ';'; SELECT 2;").statements

      scriptStatements.toList must beEqualTo(List("SELECT 1", "SELECT ';'; SELECT 2;"))
    }

    "only split on semicolons that end a line with !split-semicolon last" in {
      val scriptStatements = ScriptSansEvolution(
        Seq(
          "-- !split-semicolon: last",
          "INSERT INTO foo VALUES ('abc; def', 'ghi; jkl');",
          "INSERT INTO foo",
          "  VALUES ('mno; pqr');  ",
          "INSERT INTO foo VALUES (';;'); INSERT INTO foo VALUES (';;');",
          "-- !split-semicolon: always",
          "SELECT 1; SELECT 2;"
        ).mkString("\n")
      ).statements

      scriptStatements.toList must beEqualTo(
        List(
          "INSERT INTO foo VALUES ('abc; def', 'ghi; jkl')",
          "INSERT INTO foo\n  VALUES ('mno; pqr')",
          "INSERT INTO foo VALUES (';;'); INSERT INTO foo VALUES (';;')",
          "SELECT 1",
          "SELECT 2"
        )
      )
    }

    "keep double-semicolons with !split-semicolon never and last" in {
      val scriptStatements = ScriptSansEvolution(
        Seq(
          "-- !split-semicolon: never",
          "SELECT ';;'",
          "-- !split-semicolon: last",
          ";",
          "SELECT ';;';",
          "-- !split-semicolon: always",
          "SELECT ';;';"
        ).mkString("\n")
      ).statements

      scriptStatements.toList must beEqualTo(List("SELECT ';;'", "SELECT ';;'", "SELECT ';'"))
    }

    "accept !split-semicolon in # comments, in any case and with whitespace, also with Windows line endings" in {
      val scriptStatements =
        ScriptSansEvolution("SELECT 1;\r\n  #  !SPLIT-SEMICOLON :  Never  \r\nSELECT ';';\r\n").statements

      scriptStatements.toList must beEqualTo(List("SELECT 1", "SELECT ';';"))
    }

    "not make the !split-semicolon comment lines part of the statements" in {
      val scriptStatements =
        ScriptSansEvolution("-- !split-semicolon: never\nSELECT 1\n-- !split-semicolon: always\n;").statements

      scriptStatements.toList must beEqualTo(List("SELECT 1"))
    }

    "fail on an unknown !split-semicolon mode" in {
      ScriptSansEvolution("-- !split-semicolon: sometimes\nSELECT 1;").statements must throwAn[
        IllegalArgumentException
      ](
        "Unknown mode 'sometimes' of !split-semicolon"
      )
    }
  }

  private case class ScriptSansEvolution(sql: String) extends Script {
    override val evolution = Evolution(0, "", "")
  }

  "Conflicts" should {
    "not be noticed if there aren't any" in {
      val downRest = (9 to 1).reverse.map(i => Evolution(i, s"DummySQLUP$i", s"DummySQLDOWN$i"))
      val upRest   = downRest

      val (conflictingDowns, conflictingUps) = Evolutions.conflictings(downRest, upRest)

      conflictingDowns.size must beEqualTo(0)
      conflictingUps.size must beEqualTo(0)
    }

    "be noticed on the most recent one" in {
      val downRest = (1 to 9).reverse.map(i => Evolution(i, s"DummySQLUP$i", s"DummySQLDOWN$i"))
      val upRest   = Evolution(9, "DifferentDummySQLUP", "DifferentDummySQLDOWN") +: (1 to 8).reverse
        .map(i => Evolution(i, s"DummySQLUP$i", s"DummySQLDOWN$i"))

      val (conflictingDowns, conflictingUps) = Evolutions.conflictings(downRest, upRest)

      conflictingDowns.size must beEqualTo(1)
      conflictingUps.size must beEqualTo(1)
      conflictingDowns(0).revision must beEqualTo(9)
      conflictingUps(0).revision must beEqualTo(9)
    }

    "be noticed in the middle" in {
      val downRest = (1 to 9).reverse.map(i => Evolution(i, s"DummySQLUP$i", s"DummySQLDOWN$i"))
      val upRest   = (6 to 9).reverse.map(i => Evolution(i, s"DummySQLUP$i", s"DummySQLDOWN$i")) ++: Evolution(
        5,
        "DifferentDummySQLUP",
        "DifferentDummySQLDOWN"
      ) +: (1 to 4).reverse.map(i => Evolution(i, s"DummySQLUP$i", s"DummySQLDOWN$i"))

      val (conflictingDowns, conflictingUps) = Evolutions.conflictings(downRest, upRest)

      conflictingDowns.size must beEqualTo(5)
      conflictingUps.size must beEqualTo(5)
      conflictingDowns(0).revision must beEqualTo(9)
      conflictingUps(0).revision must beEqualTo(9)
      conflictingDowns(4).revision must beEqualTo(5)
      conflictingUps(4).revision must beEqualTo(5)
    }

    "be noticed on the first" in {
      val downRest = (1 to 9).reverse.map(i => Evolution(i, s"DummySQLUP$i", s"DummySQLDOWN$i"))
      val upRest   = (2 to 9).reverse.map(i => Evolution(i, s"DummySQLUP$i", s"DummySQLDOWN$i")) ++: List(
        Evolution(1, "DifferentDummySQLUP", "DifferentDummySQLDOWN")
      )

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
