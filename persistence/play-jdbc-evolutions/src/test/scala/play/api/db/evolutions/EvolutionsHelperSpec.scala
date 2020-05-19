/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db.evolutions

import org.specs2.mutable.Specification

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
  }

}
