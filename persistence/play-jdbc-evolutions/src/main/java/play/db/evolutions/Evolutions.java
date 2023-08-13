/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.evolutions;

import java.util.*;
import play.api.db.evolutions.DatabaseEvolutions;
import play.db.Database;
import play.libs.Scala;

/** Utilities for working with evolutions. */
public class Evolutions {

  private static final String DEFAULT_SCHEMA = "";
  private static final boolean DEFAULT_AUTOCOMMIT = true;
  private static final String DEFAULT_METATABLE = "play_evolutions";

  /**
   * Create an evolutions reader that reads evolution files from this class's own classloader.
   *
   * <p>Only useful in simple classloading environments, such as when the classloader structure is
   * flat.
   *
   * @return the evolutions reader.
   */
  public static play.api.db.evolutions.EvolutionsReader fromClassLoader() {
    return fromClassLoader(Evolutions.class.getClassLoader());
  }

  /**
   * Create an evolutions reader that reads evolution files from a classloader.
   *
   * @param classLoader The classloader to read from.
   * @return the evolutions reader.
   */
  public static play.api.db.evolutions.EvolutionsReader fromClassLoader(ClassLoader classLoader) {
    return fromClassLoader(classLoader, "");
  }

  /**
   * Create an evolutions reader that reads evolution files from a classloader.
   *
   * @param classLoader The classloader to read from.
   * @param prefix A prefix that gets added to the resource file names, for example, this could be
   *     used to namespace evolutions in different environments to work with different databases.
   * @return the evolutions reader.
   */
  public static play.api.db.evolutions.EvolutionsReader fromClassLoader(
      ClassLoader classLoader, String prefix) {
    return new play.api.db.evolutions.ClassLoaderEvolutionsReader(classLoader, prefix);
  }

  /**
   * Create an evolutions reader based on a simple map of database names to evolutions.
   *
   * @param evolutions The map of database names to evolutions.
   * @return the evolutions reader.
   */
  public static play.api.db.evolutions.EvolutionsReader fromMap(
      Map<String, List<Evolution>> evolutions) {
    return new SimpleEvolutionsReader(evolutions);
  }

  /**
   * Create an evolutions reader for the default database from a list of evolutions.
   *
   * @param evolutions The list of evolutions.
   * @return the evolutions reader.
   */
  public static play.api.db.evolutions.EvolutionsReader forDefault(Evolution... evolutions) {
    Map<String, List<Evolution>> map = new HashMap<>();
    map.put("default", Arrays.asList(evolutions));
    return fromMap(map);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   */
  public static void applyEvolutions(Database database) {
    applyEvolutions(database, fromClassLoader());
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param autocommit Whether autocommit should be used.
   */
  public static void applyEvolutions(Database database, boolean autocommit) {
    applyEvolutions(database, fromClassLoader(), autocommit);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param schema The schema where all the play evolution tables are saved in
   */
  public static void applyEvolutions(Database database, String schema) {
    applyEvolutions(database, fromClassLoader(), schema);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void applyEvolutions(
      Database database,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    applyEvolutions(
        database,
        fromClassLoader(),
        substitutionsMappings,
        substitutionsPrefix,
        substitutionsSuffix,
        substitutionsEscape);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param metaTable Table to keep evolutions' meta data
   * @param database The database to apply the evolutions to.
   */
  public static void applyEvolutions(String metaTable, Database database) {
    applyEvolutions(metaTable, database, fromClassLoader());
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param schema The schema that all the play evolution tables are saved in
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void applyEvolutions(
      Database database,
      String schema,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    applyEvolutions(
        database,
        fromClassLoader(),
        schema,
        substitutionsMappings,
        substitutionsPrefix,
        substitutionsSuffix,
        substitutionsEscape);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param metaTable Table to keep evolutions' meta data
   * @param database The database to apply the evolutions to.
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void applyEvolutions(
      String metaTable,
      Database database,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    applyEvolutions(
        metaTable,
        database,
        fromClassLoader(),
        substitutionsMappings,
        substitutionsPrefix,
        substitutionsSuffix,
        substitutionsEscape);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param schema The schema where all the play evolution tables are saved in
   * @param metaTable Table to keep evolutions' meta data
   */
  public static void applyEvolutions(Database database, String schema, String metaTable) {
    applyEvolutions(database, fromClassLoader(), schema, metaTable);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param schema The schema that all the play evolution tables are saved in
   * @param metaTable Table to keep evolutions' meta data
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void applyEvolutions(
      Database database,
      String schema,
      String metaTable,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    applyEvolutions(
        database,
        fromClassLoader(),
        schema,
        metaTable,
        substitutionsMappings,
        substitutionsPrefix,
        substitutionsSuffix,
        substitutionsEscape);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param metaTable Table to keep evolutions' meta data
   * @param database The database to apply the evolutions to.
   * @param autocommit Whether autocommit should be used.
   */
  public static void applyEvolutions(String metaTable, Database database, boolean autocommit) {
    applyEvolutions(metaTable, database, fromClassLoader(), autocommit);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param autocommit Whether autocommit should be used.
   * @param schema The schema that all the play evolution tables are saved in
   */
  public static void applyEvolutions(Database database, boolean autocommit, String schema) {
    applyEvolutions(database, fromClassLoader(), autocommit, schema);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param autocommit Whether autocommit should be used.
   * @param schema The schema that all the play evolution tables are saved in
   * @param metaTable Table to keep evolutions' meta data
   */
  public static void applyEvolutions(
      Database database, boolean autocommit, String schema, String metaTable) {
    applyEvolutions(database, fromClassLoader(), autocommit, schema, metaTable);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param autocommit Whether autocommit should be used.
   * @param schema The schema that all the play evolution tables are saved in
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void applyEvolutions(
      Database database,
      boolean autocommit,
      String schema,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    applyEvolutions(
        database,
        fromClassLoader(),
        autocommit,
        schema,
        substitutionsMappings,
        substitutionsPrefix,
        substitutionsSuffix,
        substitutionsEscape);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param metaTable Table to keep evolutions' meta data
   * @param database The database to apply the evolutions to.
   * @param autocommit Whether autocommit should be used.
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void applyEvolutions(
      String metaTable,
      Database database,
      boolean autocommit,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    applyEvolutions(
        metaTable,
        database,
        fromClassLoader(),
        autocommit,
        substitutionsMappings,
        substitutionsPrefix,
        substitutionsSuffix,
        substitutionsEscape);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param autocommit Whether autocommit should be used.
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void applyEvolutions(
      Database database,
      boolean autocommit,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    applyEvolutions(
        database,
        fromClassLoader(),
        autocommit,
        substitutionsMappings,
        substitutionsPrefix,
        substitutionsSuffix,
        substitutionsEscape);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param autocommit Whether autocommit should be used.
   * @param schema The schema that all the play evolution tables are saved in
   * @param metaTable Table to keep evolutions' meta data
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void applyEvolutions(
      Database database,
      boolean autocommit,
      String schema,
      String metaTable,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    applyEvolutions(
        database,
        fromClassLoader(),
        autocommit,
        schema,
        metaTable,
        substitutionsMappings,
        substitutionsPrefix,
        substitutionsSuffix,
        substitutionsEscape);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param reader The reader to read the evolutions.
   */
  public static void applyEvolutions(
      Database database, play.api.db.evolutions.EvolutionsReader reader) {
    applyEvolutions(database, reader, DEFAULT_AUTOCOMMIT);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param reader The reader to read the evolutions.
   * @param autocommit Whether autocommit should be used.
   */
  public static void applyEvolutions(
      Database database, play.api.db.evolutions.EvolutionsReader reader, boolean autocommit) {
    applyEvolutions(database, reader, autocommit, DEFAULT_SCHEMA);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param reader The reader to read the evolutions.
   * @param schema The schema where all the play evolution tables are saved in
   */
  public static void applyEvolutions(
      Database database, play.api.db.evolutions.EvolutionsReader reader, String schema) {
    applyEvolutions(database, reader, DEFAULT_AUTOCOMMIT, schema);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param reader The reader to read the evolutions.
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void applyEvolutions(
      Database database,
      play.api.db.evolutions.EvolutionsReader reader,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    applyEvolutions(
        database,
        reader,
        DEFAULT_SCHEMA,
        substitutionsMappings,
        substitutionsPrefix,
        substitutionsSuffix,
        substitutionsEscape);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param metaTable Table to keep evolutions' meta data
   * @param database The database to apply the evolutions to.
   * @param reader The reader to read the evolutions.
   */
  public static void applyEvolutions(
      String metaTable, Database database, play.api.db.evolutions.EvolutionsReader reader) {
    applyEvolutions(database, reader, DEFAULT_SCHEMA, metaTable);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param reader The reader to read the evolutions.
   * @param schema The schema that all the play evolution tables are saved in
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void applyEvolutions(
      Database database,
      play.api.db.evolutions.EvolutionsReader reader,
      String schema,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    applyEvolutions(
        database,
        reader,
        schema,
        DEFAULT_METATABLE,
        substitutionsMappings,
        substitutionsPrefix,
        substitutionsSuffix,
        substitutionsEscape);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param metaTable Table to keep evolutions' meta data
   * @param database The database to apply the evolutions to.
   * @param reader The reader to read the evolutions.
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void applyEvolutions(
      String metaTable,
      Database database,
      play.api.db.evolutions.EvolutionsReader reader,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    applyEvolutions(
        database,
        reader,
        DEFAULT_SCHEMA,
        metaTable,
        substitutionsMappings,
        substitutionsPrefix,
        substitutionsSuffix,
        substitutionsEscape);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param reader The reader to read the evolutions.
   * @param schema The schema where all the play evolution tables are saved in
   * @param metaTable Table to keep evolutions' meta data
   */
  public static void applyEvolutions(
      Database database,
      play.api.db.evolutions.EvolutionsReader reader,
      String schema,
      String metaTable) {
    applyEvolutions(database, reader, DEFAULT_AUTOCOMMIT, schema, metaTable);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param reader The reader to read the evolutions.
   * @param schema The schema that all the play evolution tables are saved in
   * @param metaTable Table to keep evolutions' meta data
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void applyEvolutions(
      Database database,
      play.api.db.evolutions.EvolutionsReader reader,
      String schema,
      String metaTable,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    applyEvolutions(
        database,
        reader,
        DEFAULT_AUTOCOMMIT,
        schema,
        metaTable,
        substitutionsMappings,
        substitutionsPrefix,
        substitutionsSuffix,
        substitutionsEscape);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param metaTable Table to keep evolutions' meta data
   * @param database The database to apply the evolutions to.
   * @param reader The reader to read the evolutions.
   * @param autocommit Whether autocommit should be used.
   */
  public static void applyEvolutions(
      String metaTable,
      Database database,
      play.api.db.evolutions.EvolutionsReader reader,
      boolean autocommit) {
    applyEvolutions(database, reader, autocommit, DEFAULT_SCHEMA, metaTable);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param reader The reader to read the evolutions.
   * @param autocommit Whether autocommit should be used.
   * @param schema The schema where all the play evolution tables are saved in
   */
  public static void applyEvolutions(
      Database database,
      play.api.db.evolutions.EvolutionsReader reader,
      boolean autocommit,
      String schema) {
    DatabaseEvolutions evolutions = new DatabaseEvolutions(database.asScala(), schema);
    evolutions.evolve(evolutions.scripts(reader), autocommit);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param reader The reader to read the evolutions.
   * @param autocommit Whether autocommit should be used.
   * @param schema The schema where all the play evolution tables are saved in
   * @param metaTable Table to keep evolutions' meta data
   */
  public static void applyEvolutions(
      Database database,
      play.api.db.evolutions.EvolutionsReader reader,
      boolean autocommit,
      String schema,
      String metaTable) {
    DatabaseEvolutions evolutions = new DatabaseEvolutions(database.asScala(), schema, metaTable);
    evolutions.evolve(evolutions.scripts(reader), autocommit);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param reader The reader to read the evolutions.
   * @param autocommit Whether autocommit should be used.
   * @param schema The schema that all the play evolution tables are saved in
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void applyEvolutions(
      Database database,
      play.api.db.evolutions.EvolutionsReader reader,
      boolean autocommit,
      String schema,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    applyEvolutions(
        database,
        reader,
        autocommit,
        schema,
        DEFAULT_METATABLE,
        substitutionsMappings,
        substitutionsPrefix,
        substitutionsSuffix,
        substitutionsEscape);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param metaTable Table to keep evolutions' meta data
   * @param database The database to apply the evolutions to.
   * @param reader The reader to read the evolutions.
   * @param autocommit Whether autocommit should be used.
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void applyEvolutions(
      String metaTable,
      Database database,
      play.api.db.evolutions.EvolutionsReader reader,
      boolean autocommit,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    applyEvolutions(
        database,
        reader,
        autocommit,
        DEFAULT_SCHEMA,
        metaTable,
        substitutionsMappings,
        substitutionsPrefix,
        substitutionsSuffix,
        substitutionsEscape);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param reader The reader to read the evolutions.
   * @param autocommit Whether autocommit should be used.
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void applyEvolutions(
      Database database,
      play.api.db.evolutions.EvolutionsReader reader,
      boolean autocommit,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    applyEvolutions(
        database,
        reader,
        autocommit,
        DEFAULT_SCHEMA,
        substitutionsMappings,
        substitutionsPrefix,
        substitutionsSuffix,
        substitutionsEscape);
  }

  /**
   * Apply evolutions for the given database.
   *
   * @param database The database to apply the evolutions to.
   * @param reader The reader to read the evolutions.
   * @param autocommit Whether autocommit should be used.
   * @param schema The schema that all the play evolution tables are saved in
   * @param metaTable Table to keep evolutions' meta data
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void applyEvolutions(
      Database database,
      play.api.db.evolutions.EvolutionsReader reader,
      boolean autocommit,
      String schema,
      String metaTable,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    DatabaseEvolutions evolutions =
        new DatabaseEvolutions(
            database.asScala(),
            schema,
            metaTable,
            Scala.asScala(substitutionsMappings),
            substitutionsPrefix,
            substitutionsSuffix,
            substitutionsEscape);
    evolutions.evolve(evolutions.scripts(reader), autocommit);
  }

  /**
   * Cleanup evolutions for the given database.
   *
   * <p>This will run the down scripts for all the applied evolutions.
   *
   * @param database The database to apply the evolutions to.
   * @param autocommit Whether autocommit should be used.
   * @param schema The schema where all the play evolution tables are saved in
   */
  public static void cleanupEvolutions(Database database, boolean autocommit, String schema) {
    DatabaseEvolutions evolutions = new DatabaseEvolutions(database.asScala(), schema);
    evolutions.evolve(evolutions.resetScripts(), autocommit);
  }

  /**
   * Cleanup evolutions for the given database.
   *
   * <p>This will run the down scripts for all the applied evolutions.
   *
   * @param database The database to apply the evolutions to.
   * @param autocommit Whether autocommit should be used.
   * @param schema The schema where all the play evolution tables are saved in
   * @param metaTable Table to keep evolutions' meta data
   */
  public static void cleanupEvolutions(
      Database database, boolean autocommit, String schema, String metaTable) {
    DatabaseEvolutions evolutions = new DatabaseEvolutions(database.asScala(), schema, metaTable);
    evolutions.evolve(evolutions.resetScripts(), autocommit);
  }

  /**
   * Cleanup evolutions for the given database.
   *
   * <p>This will run the down scripts for all the applied evolutions.
   *
   * @param database The database to apply the evolutions to.
   * @param autocommit Whether autocommit should be used.
   * @param schema The schema that all the play evolution tables are saved in
   * @param metaTable Table to keep evolutions' meta data
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void cleanupEvolutions(
      Database database,
      boolean autocommit,
      String schema,
      String metaTable,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    DatabaseEvolutions evolutions =
        new DatabaseEvolutions(
            database.asScala(),
            schema,
            metaTable,
            Scala.asScala(substitutionsMappings),
            substitutionsPrefix,
            substitutionsSuffix,
            substitutionsEscape);
    evolutions.evolve(evolutions.resetScripts(), autocommit);
  }

  /**
   * Cleanup evolutions for the given database.
   *
   * <p>This will run the down scripts for all the applied evolutions.
   *
   * @param database The database to apply the evolutions to.
   * @param autocommit Whether autocommit should be used.
   */
  public static void cleanupEvolutions(Database database, boolean autocommit) {
    cleanupEvolutions(database, autocommit, DEFAULT_SCHEMA);
  }

  /**
   * Cleanup evolutions for the given database.
   *
   * <p>This will run the down scripts for all the applied evolutions.
   *
   * @param database The database to apply the evolutions to.
   * @param schema The schema where all the play evolution tables are saved in
   */
  public static void cleanupEvolutions(Database database, String schema) {
    cleanupEvolutions(database, DEFAULT_AUTOCOMMIT, schema);
  }

  /**
   * Cleanup evolutions for the given database.
   *
   * <p>This will run the down scripts for all the applied evolutions.
   *
   * @param database The database to apply the evolutions to.
   * @param schema The schema where all the play evolution tables are saved in
   * @param metaTable Table to keep evolutions' meta data
   */
  public static void cleanupEvolutions(Database database, String schema, String metaTable) {
    cleanupEvolutions(database, DEFAULT_AUTOCOMMIT, schema, metaTable);
  }

  /**
   * Cleanup evolutions for the given database.
   *
   * <p>This will run the down scripts for all the applied evolutions.
   *
   * @param database The database to apply the evolutions to.
   * @param schema The schema that all the play evolution tables are saved in
   * @param metaTable Table to keep evolutions' meta data
   * @param substitutionsMappings Mappings of variables (without the prefix and suffix) and their
   *     replacements.
   * @param substitutionsPrefix Prefix of the variable to substitute, e.g. "$evolutions{{{".
   * @param substitutionsSuffix Suffix of the variable to substitute, e.g. "}}}".
   * @param substitutionsEscape Whetever escaping of variables is enabled via a preceding "!". E.g.
   *     "!$evolutions{{{my_variable}}}" ends up as "$evolutions{{{my_variable}}}" in the final sql
   *     instead of replacing it with its substitution.
   */
  public static void cleanupEvolutions(
      Database database,
      String schema,
      String metaTable,
      Map<String, String> substitutionsMappings,
      String substitutionsPrefix,
      String substitutionsSuffix,
      boolean substitutionsEscape) {
    cleanupEvolutions(
        database,
        DEFAULT_AUTOCOMMIT,
        schema,
        metaTable,
        substitutionsMappings,
        substitutionsPrefix,
        substitutionsSuffix,
        substitutionsEscape);
  }

  /**
   * Cleanup evolutions for the given database.
   *
   * <p>This will run the down scripts for all the applied evolutions.
   *
   * @param database The database to apply the evolutions to.
   */
  public static void cleanupEvolutions(Database database) {
    cleanupEvolutions(database, DEFAULT_SCHEMA);
  }
}
