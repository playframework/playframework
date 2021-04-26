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
      suffix: String,
      expectedEscapeDisabled: String,
      expectedEscapeEnabled: String
  ): MatchResult[Any] = {
    EvolutionsHelper.substituteVariables(
      sql = sql,
      substitutionsMappings = substitutions,
      prefix = prefix,
      suffix = suffix,
      escape = false
    ) mustEqual expectedEscapeDisabled
    EvolutionsHelper.substituteVariables(
      sql = sql,
      substitutionsMappings = substitutions,
      prefix = prefix,
      suffix = suffix,
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
    "substitute variables with $evolutions{{{...}}} syntax" in {
      testSubstituteVariables(
        sql =
          "INSERT INTO $evolutions{{{table}}}($evolutions{{{field}}}) VALUES ('$evolutions{{{value}}} $evolutions{{{does_not_exist}}} $evolutions{{{}}}')",
        substitutions = Map("table" -> "users", "field" -> "username", "value" -> "John Doe"),
        prefix = "$evolutions{{{",
        suffix = "}}}",
        expectedEscapeDisabled =
          "INSERT INTO users(username) VALUES ('John Doe $evolutions{{{does_not_exist}}} $evolutions{{{}}}')",
        expectedEscapeEnabled =
          "INSERT INTO users(username) VALUES ('John Doe $evolutions{{{does_not_exist}}} $evolutions{{{}}}')"
      )
    }
    "substitute variables with ${...} syntax" in {
      testSubstituteVariables(
        sql = "INSERT INTO ${table}(${field}) VALUES ('${value} ${does_not_exist} ${}')",
        substitutions = Map("table" -> "users", "field" -> "username", "value" -> "John Doe"),
        prefix = "${",
        suffix = "}",
        expectedEscapeDisabled = "INSERT INTO users(username) VALUES ('John Doe ${does_not_exist} ${}')",
        expectedEscapeEnabled = "INSERT INTO users(username) VALUES ('John Doe ${does_not_exist} ${}')"
      )
    }
    "don't substitute escaped variables" in {
      testSubstituteVariables(
        sql = "INSERT INTO !${table}(!${field}, !${field}) VALUES ('!${value}', !${value}, !${})",
        substitutions = Map("table" -> "users", "field" -> "username", "value" -> "John Doe"),
        prefix = "${",
        suffix = "}",
        expectedEscapeDisabled = "INSERT INTO !${table}(!${field}, !${field}) VALUES ('!${value}', !${value}, !${})",
        expectedEscapeEnabled = "INSERT INTO ${table}(${field}, ${field}) VALUES ('${value}', ${value}, ${})"
      )
    }
    "escape variables even when no substitution mappings exist" in {
      testSubstituteVariables(
        sql = "INSERT INTO !${table}(!${field}, !${field}) VALUES ('!${value}', !${value})",
        substitutions = Map.empty,
        prefix = "${",
        suffix = "}",
        expectedEscapeDisabled = "INSERT INTO !${table}(!${field}, !${field}) VALUES ('!${value}', !${value})",
        expectedEscapeEnabled = "INSERT INTO ${table}(${field}, ${field}) VALUES ('${value}', ${value})"
      )
    }
    "works when variable mapping name contains" in {
      Fragment.foreach(Seq("{", "}", "$", "\\", "!")) { randomChar =>
        s"the character '${randomChar}'" >> {
          testSubstituteVariables(
            sql = s"$${abc${randomChar}xyz} !$${abc${randomChar}xyz}", // e.g. "${abc}xyz} !${abc}xyz}"
            substitutions = Map(s"abc${randomChar}xyz" -> "something"), // e.g. "abc}xyz" -> "something"
            prefix = "${",
            suffix = "}",
            expectedEscapeDisabled = s"something !$${abc${randomChar}xyz}",
            expectedEscapeEnabled = s"something $${abc${randomChar}xyz}"
          )
        }
      }
    }
    "escape variables when variable name contains { or ! or $ or \\" in {
      testSubstituteVariables(
        sql = "!${tab{le} !${fi!eld} !${fi$eld} !${fi\\eld}",
        substitutions = Map.empty,
        prefix = "${",
        suffix = "}",
        expectedEscapeDisabled = "!${tab{le} !${fi!eld} !${fi$eld} !${fi\\eld}",
        expectedEscapeEnabled = "${tab{le} ${fi!eld} ${fi$eld} ${fi\\eld}"
      )
    }
    "escape variables even when variable name contains special regex characters" in {
      testSubstituteVariables(
        sql = """!${/g/d/e^/hn} !${[^abc]} !${.*} !${va/g/d/e^/lue} !${use'rs} !${+*} !${Jo/g/d/e^/hn}""",
        substitutions = Map.empty,
        prefix = "${",
        suffix = "}",
        expectedEscapeDisabled =
          """!${/g/d/e^/hn} !${[^abc]} !${.*} !${va/g/d/e^/lue} !${use'rs} !${+*} !${Jo/g/d/e^/hn}""",
        expectedEscapeEnabled = """${/g/d/e^/hn} ${[^abc]} ${.*} ${va/g/d/e^/lue} ${use'rs} ${+*} ${Jo/g/d/e^/hn}"""
      )
    }
    "escape variable if it contains new line" in {
      testSubstituteVariables(
        sql = """INSERT INTO !${ta
                |ble}(!${field}) VALUES ('!${value}')""".stripMargin,
        substitutions = Map("table" -> """us$e\'rs""", "field" -> """u\$sername""", "value" -> """Jo/g/d/e^/hn Doe"""),
        prefix = "${",
        suffix = "}",
        expectedEscapeDisabled = """INSERT INTO !${ta
                                   |ble}(!${field}) VALUES ('!${value}')""".stripMargin,
        expectedEscapeEnabled = """INSERT INTO ${ta
                                  |ble}(${field}) VALUES ('${value}')""".stripMargin
      )
    }
    "substitute variables ignoring case of the variable names" in {
      testSubstituteVariables(
        sql = "INSERT INTO ${TABLE}(${fIeLd}) VALUES ('${VaLuE}')",
        substitutions = Map("taBlE" -> "users", "FIELD" -> "username", "vAlUe" -> "John Doe"),
        prefix = "${",
        suffix = "}",
        expectedEscapeDisabled = "INSERT INTO users(username) VALUES ('John Doe')",
        expectedEscapeEnabled = "INSERT INTO users(username) VALUES ('John Doe')"
      )
    }
    "don't substitute escaped variables ignoring case of the variable names" in {
      testSubstituteVariables(
        sql = "INSERT INTO !${TABLE}(!${fIeLd}) VALUES ('!${VaLuE}')",
        substitutions = Map("taBlE" -> "users", "FIELD" -> "username", "vAlUe" -> "John Doe"),
        prefix = "${",
        suffix = "}",
        expectedEscapeDisabled = "INSERT INTO !${TABLE}(!${fIeLd}) VALUES ('!${VaLuE}')",
        expectedEscapeEnabled = "INSERT INTO ${TABLE}(${fIeLd}) VALUES ('${VaLuE}')"
      )
    }
    "substitute only variables that are defined, ignoring undefined ones" in {
      testSubstituteVariables(
        sql = "INSERT INTO ${table}(${field}) VALUES ('${anothervalue}')",
        substitutions = Map("table" -> "users", "field" -> "username", "value" -> "John Doe"),
        prefix = "${",
        suffix = "}",
        expectedEscapeDisabled = "INSERT INTO users(username) VALUES ('${anothervalue}')",
        expectedEscapeEnabled = "INSERT INTO users(username) VALUES ('${anothervalue}')"
      )
    }
    "substitute variables multiple times" in {
      testSubstituteVariables(
        sql = "INSERT INTO ${myvar}(${myvar}) VALUES ('${myvar}')",
        substitutions = Map("myvar" -> "user"),
        prefix = "${",
        suffix = "}",
        expectedEscapeDisabled = "INSERT INTO user(user) VALUES ('user')",
        expectedEscapeEnabled = "INSERT INTO user(user) VALUES ('user')"
      )
    }
    "don't substitute variables multiple times if no substitute are passed" in {
      testSubstituteVariables(
        sql = "INSERT INTO ${myvar}(${myvar}) VALUES ('${myvar}')",
        substitutions = Map.empty,
        prefix = "${",
        suffix = "}",
        expectedEscapeDisabled = "INSERT INTO ${myvar}(${myvar}) VALUES ('${myvar}')",
        expectedEscapeEnabled = "INSERT INTO ${myvar}(${myvar}) VALUES ('${myvar}')"
      )
    }
    "substitute variables with multi-char prefix" in {
      testSubstituteVariables(
        sql = "INSERT INTO $@§{table}($@§{field}) VALUES ('$@§{value}')",
        substitutions = Map("table" -> "users", "field" -> "username", "value" -> "John Doe"),
        prefix = "$@§{",
        suffix = "}",
        expectedEscapeDisabled = "INSERT INTO users(username) VALUES ('John Doe')",
        expectedEscapeEnabled = "INSERT INTO users(username) VALUES ('John Doe')"
      )
    }
    "don't substitute escaped variables with multi-char prefix" in {
      testSubstituteVariables(
        sql = "INSERT INTO !$@§{table}(!$@§{field}) VALUES ('!$@§{value}')",
        substitutions = Map("table" -> "users", "field" -> "username", "value" -> "John Doe"),
        prefix = "$@§{",
        suffix = "}",
        expectedEscapeDisabled = "INSERT INTO !$@§{table}(!$@§{field}) VALUES ('!$@§{value}')",
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
        prefix = "${",
        suffix = "}",
        expectedEscapeDisabled = "INSERT INTO users(firstname, lastname, age) VALUES ('John Doe')",
        expectedEscapeEnabled = "INSERT INTO users(firstname, lastname, age) VALUES ('John Doe')"
      )
    }
    "don't substitute escaped variables that contain special regex characters" in {
      testSubstituteVariables(
        sql = """INSERT INTO !${tab'le}(!${[^abc]}, !${+*}, !${.*}) VALUES ('!${va/g/d/e^/lue}')""",
        substitutions = Map(
          """tab'le"""        -> "users",
          "[^abc]"            -> "firstname",
          "+*"                -> "lastname",
          ".*"                -> "age",
          """field"""         -> "username",
          """va/g/d/e^/lue""" -> "John Doe"
        ),
        prefix = "${",
        suffix = "}",
        expectedEscapeDisabled = """INSERT INTO !${tab'le}(!${[^abc]}, !${+*}, !${.*}) VALUES ('!${va/g/d/e^/lue}')""",
        expectedEscapeEnabled = """INSERT INTO ${tab'le}(${[^abc]}, ${+*}, ${.*}) VALUES ('${va/g/d/e^/lue}')"""
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
        prefix = "${",
        suffix = "}",
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
        prefix = "${",
        suffix = "}",
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
