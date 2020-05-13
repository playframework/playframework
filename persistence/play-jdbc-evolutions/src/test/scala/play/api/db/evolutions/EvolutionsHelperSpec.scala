/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db.evolutions

import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragment

class EvolutionsHelperSpec extends Specification {
  val createEvolutionsTableWithoutCustomization =
    """
      create table ${schema}${evolutions_table} (
          id int not null primary key,
          hash varchar(255) not null,
          applied_at datetime not null,
          apply_script text,
          revert_script text,
          state varchar(255),
          last_problem text
      )
    """

  val createEvolutionsTableWithCustomSchemaAndTable =
    """
      create table test_schema.sample_play_evolutions (
          id int not null primary key,
          hash varchar(255) not null,
          applied_at datetime not null,
          apply_script text,
          revert_script text,
          state varchar(255),
          last_problem text
      )
    """

  val createEvolutionsTableWithCustomSchema =
    """
      create table test_schema.play_evolutions (
          id int not null primary key,
          hash varchar(255) not null,
          applied_at datetime not null,
          apply_script text,
          revert_script text,
          state varchar(255),
          last_problem text
      )
    """

  private def testSubstituteVariables(
      sql: String,
      substitutions: Map[String, String],
      prefix: String,
      expectedEscapeDisabled: String,
      expectedEscapeEnabled: String
  ): MatchResult[Any] = {
    EvolutionsHelper.substituteVariables(
      sql = sql,
      substitutions = substitutions,
      prefix = prefix,
      escape = false
    ) mustEqual expectedEscapeDisabled
    EvolutionsHelper.substituteVariables(
      sql = sql,
      substitutions = substitutions,
      prefix = prefix,
      escape = true
    ) mustEqual expectedEscapeEnabled
  }

  import EvolutionsHelper._
  "EvolutionsHelper" should {
    "include schema in table definition" in {
      applySchemaAndTable(
        sql = createEvolutionsTableWithoutCustomization,
        schema = "test_schema",
        table = "play_evolutions"
      ) mustEqual createEvolutionsTableWithCustomSchema
    }
    "include table name and schema in table definition" in {
      applySchemaAndTable(
        sql = createEvolutionsTableWithoutCustomization,
        schema = "test_schema",
        table = "sample_play_evolutions"
      ) mustEqual createEvolutionsTableWithCustomSchemaAndTable
    }
    "include table name and schema in statements" in {
      applySchemaAndTable(
        sql = "select lock from ${schema}${evolutions_table}_lock",
        schema = "test_schema",
        table = "sample_play_evolutions"
      ) mustEqual "select lock from test_schema.sample_play_evolutions_lock"
    }
    "substitute variables" in {
      testSubstituteVariables(
        sql = "INSERT INTO ${table}(${field}) VALUES ('${value}')",
        substitutions = Map("table" -> "users", "field" -> "username", "value" -> "John Doe"),
        prefix = "$",
        expectedEscapeDisabled = "INSERT INTO users(username) VALUES ('John Doe')",
        expectedEscapeEnabled = "INSERT INTO users(username) VALUES ('John Doe')"
      )
    }
    "don't substitute escaped variables" in {
      testSubstituteVariables(
        sql = "INSERT INTO ${!table}(${!field}, ${!field}) VALUES ('${!value}', ${!value})",
        substitutions = Map("table" -> "users", "field" -> "username", "value" -> "John Doe"),
        prefix = "$",
        expectedEscapeDisabled = "INSERT INTO ${!table}(${!field}, ${!field}) VALUES ('${!value}', ${!value})",
        expectedEscapeEnabled = "INSERT INTO ${table}(${field}, ${field}) VALUES ('${value}', ${value})"
      )
    }
    "escape variables even when no substitution mappings exist" in {
      testSubstituteVariables(
        sql = "INSERT INTO ${!table}(${!field}, ${!field}) VALUES ('${!value}', ${!value})",
        substitutions = Map.empty,
        prefix = "$",
        expectedEscapeDisabled = "INSERT INTO ${!table}(${!field}, ${!field}) VALUES ('${!value}', ${!value})",
        expectedEscapeEnabled = "INSERT INTO ${table}(${field}, ${field}) VALUES ('${value}', ${value})"
      )
    }
    "throw exception when variable mapping name contains" in {
      Fragment.foreach(Seq("{", "}", "$", "\\", "!")) { badChar =>
        s"the character '${badChar}'" >> {
          testSubstituteVariables(
            sql = "does not matter 1",
            substitutions = Map(s"abc${badChar}xyz" -> "does not matter 2"),
            prefix = "$",
            expectedEscapeDisabled = "does not matter 3",
            expectedEscapeEnabled = "does not matter 4"
          ) must throwA[RuntimeException].like {
            case e =>
              e.getMessage must_=== s"Evolution mapping key abc${badChar}xyz contains a disallowed character: {, }, $$, \\ or !"
          }
        }
      }
    }
    "don't escape variables when variable name contains { or ! or $ or \\" in {
      testSubstituteVariables(
        sql = "${!tab{le} ${!fi!eld} ${!fi$eld} ${!fi\\eld}",
        substitutions = Map.empty,
        prefix = "$",
        expectedEscapeDisabled = "${!tab{le} ${!fi!eld} ${!fi$eld} ${!fi\\eld}",
        expectedEscapeEnabled = "${!tab{le} ${!fi!eld} ${!fi$eld} ${!fi\\eld}"
      )
    }
    "escape variables even when variable name contains special regex characters" in {
      testSubstituteVariables(
        sql = """${!/g/d/e^/hn} ${![^abc]} ${!.*} ${!va/g/d/e^/lue} ${!use'rs} ${!+*} ${!Jo/g/d/e^/hn}""",
        substitutions = Map.empty,
        prefix = "$",
        expectedEscapeDisabled =
          """${!/g/d/e^/hn} ${![^abc]} ${!.*} ${!va/g/d/e^/lue} ${!use'rs} ${!+*} ${!Jo/g/d/e^/hn}""",
        expectedEscapeEnabled = """${/g/d/e^/hn} ${[^abc]} ${.*} ${va/g/d/e^/lue} ${use'rs} ${+*} ${Jo/g/d/e^/hn}"""
      )
    }
    "don't escape variable if it contains new line" in {
      testSubstituteVariables(
        sql = """INSERT INTO ${!ta
                |ble}(${!field}) VALUES ('${!value}')""".stripMargin,
        substitutions = Map("table" -> """us$e\'rs""", "field" -> """u\$sername""", "value" -> """Jo/g/d/e^/hn Doe"""),
        prefix = "$",
        expectedEscapeDisabled = """INSERT INTO ${!ta
                                   |ble}(${!field}) VALUES ('${!value}')""".stripMargin,
        expectedEscapeEnabled = """INSERT INTO ${!ta
                                  |ble}(${field}) VALUES ('${value}')""".stripMargin
      )
    }
    "substitute variables ignoring case of the variable names" in {
      testSubstituteVariables(
        sql = "INSERT INTO ${TABLE}(${fIeLd}) VALUES ('${VaLuE}')",
        substitutions = Map("taBlE" -> "users", "FIELD" -> "username", "vAlUe" -> "John Doe"),
        prefix = "$",
        expectedEscapeDisabled = "INSERT INTO users(username) VALUES ('John Doe')",
        expectedEscapeEnabled = "INSERT INTO users(username) VALUES ('John Doe')"
      )
    }
    "don't substitute escaped variables ignoring case of the variable names" in {
      testSubstituteVariables(
        sql = "INSERT INTO ${!TABLE}(${!fIeLd}) VALUES ('${!VaLuE}')",
        substitutions = Map("taBlE" -> "users", "FIELD" -> "username", "vAlUe" -> "John Doe"),
        prefix = "$",
        expectedEscapeDisabled = "INSERT INTO ${!TABLE}(${!fIeLd}) VALUES ('${!VaLuE}')",
        expectedEscapeEnabled = "INSERT INTO ${TABLE}(${fIeLd}) VALUES ('${VaLuE}')"
      )
    }
    "substitute only variables that are defined, ignoring undefined ones" in {
      testSubstituteVariables(
        sql = "INSERT INTO ${table}(${field}) VALUES ('${anothervalue}')",
        substitutions = Map("table" -> "users", "field" -> "username", "value" -> "John Doe"),
        prefix = "$",
        expectedEscapeDisabled = "INSERT INTO users(username) VALUES ('${anothervalue}')",
        expectedEscapeEnabled = "INSERT INTO users(username) VALUES ('${anothervalue}')"
      )
    }
    "substitute variables multiple times" in {
      testSubstituteVariables(
        sql = "INSERT INTO ${myvar}(${myvar}) VALUES ('${myvar}')",
        substitutions = Map("myvar" -> "user"),
        prefix = "$",
        expectedEscapeDisabled = "INSERT INTO user(user) VALUES ('user')",
        expectedEscapeEnabled = "INSERT INTO user(user) VALUES ('user')"
      )
    }
    "don't substitute variables if no substitute are passed" in {
      testSubstituteVariables(
        sql = "INSERT INTO ${myvar}(${myvar}) VALUES ('${myvar}')",
        substitutions = Map.empty,
        prefix = "$",
        expectedEscapeDisabled = "INSERT INTO ${myvar}(${myvar}) VALUES ('${myvar}')",
        expectedEscapeEnabled = "INSERT INTO ${myvar}(${myvar}) VALUES ('${myvar}')"
      )
    }
    "substitute variables with multi-char prefix" in {
      testSubstituteVariables(
        sql = "INSERT INTO $@§{table}($@§{field}) VALUES ('$@§{value}')",
        substitutions = Map("table" -> "users", "field" -> "username", "value" -> "John Doe"),
        prefix = "$@§",
        expectedEscapeDisabled = "INSERT INTO users(username) VALUES ('John Doe')",
        expectedEscapeEnabled = "INSERT INTO users(username) VALUES ('John Doe')"
      )
    }
    "don't substitute escaped variables with multi-char prefix" in {
      testSubstituteVariables(
        sql = "INSERT INTO $@§{!table}($@§{!field}) VALUES ('$@§{!value}')",
        substitutions = Map("table" -> "users", "field" -> "username", "value" -> "John Doe"),
        prefix = "$@§",
        expectedEscapeDisabled = "INSERT INTO $@§{!table}($@§{!field}) VALUES ('$@§{!value}')",
        expectedEscapeEnabled = "INSERT INTO $@§{table}($@§{field}) VALUES ('$@§{value}')"
      )
    }
    "substitute variables that contain special regex characters" in {
      testSubstituteVariables(
        sql = """INSERT INTO ${tab'le}(${[^abc]}, ${+*}, ${.*}) VALUES ('${va/g/d/e^/lue}')""",
        substitutions = Map(
          """tab'le"""        -> "users",
          "[^abc]"            -> "firstname",
          "+*"                -> "lastname",
          ".*"                -> "age",
          """field"""         -> "username",
          """va/g/d/e^/lue""" -> "John Doe"
        ),
        prefix = "$",
        expectedEscapeDisabled = "INSERT INTO users(firstname, lastname, age) VALUES ('John Doe')",
        expectedEscapeEnabled = "INSERT INTO users(firstname, lastname, age) VALUES ('John Doe')"
      )
    }
    "substitute variables whose replacements contain special regex characters" in {
      // If we wouldn't use Matcher.quoteReplacement(...) that test would fail. Therefore a very important test!
      testSubstituteVariables(
        sql = "INSERT INTO ${table}(${field}) VALUES ('${value}')",
        substitutions = Map(
          "table" -> """us$e\'rs""",
          "field" -> """u\$sern[^abc]ame""",
          "value" -> """Jo/g/d/e^/hn +*D(+*)o(.*)e"""
        ),
        prefix = "$",
        expectedEscapeDisabled = """INSERT INTO us$e\'rs(u\$sern[^abc]ame) VALUES ('Jo/g/d/e^/hn +*D(+*)o(.*)e')""",
        expectedEscapeEnabled = """INSERT INTO us$e\'rs(u\$sern[^abc]ame) VALUES ('Jo/g/d/e^/hn +*D(+*)o(.*)e')"""
      )
    }
    "substitute variables whose replacements are multi-line" in {
      testSubstituteVariables(
        sql = "INSERT INTO ${table}(${field}) VALUES ('${value}')",
        substitutions = Map(
          "table" -> "users",
          "field" -> "note",
          "value" ->
            """This
              |is
              |a
              |random
              |note""".stripMargin
        ),
        prefix = "$",
        expectedEscapeDisabled = """INSERT INTO users(note) VALUES ('This
                                   |is
                                   |a
                                   |random
                                   |note')""".stripMargin,
        expectedEscapeEnabled = """INSERT INTO users(note) VALUES ('This
                                  |is
                                  |a
                                  |random
                                  |note')""".stripMargin
      )
    }
  }

}
