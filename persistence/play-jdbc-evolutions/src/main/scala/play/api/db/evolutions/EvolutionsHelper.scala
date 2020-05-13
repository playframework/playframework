/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db.evolutions

import java.util.regex.Matcher
import java.util.regex.Pattern

private[evolutions] object EvolutionsHelper {

  def substituteVariables(sql: String, substitutions: Map[String, String], prefix: String, escape: Boolean): String = {
    var result: String = sql;
    for ((k, v) <- substitutions) yield {
      if (Seq('{', '}', '!', '$', '\\').exists(k.contains(_))) {
        throw new RuntimeException(s"Evolution mapping key $k contains a disallowed character: {, }, $$, \\ or !")
      }
      result = result.replaceAll("(?i)" + Pattern.quote(prefix + "{" + k + "}"), Matcher.quoteReplacement(v))
    }
    if (escape) {
      result.replaceAll(
        "(?i)" + Pattern.quote(prefix) + "\\{!([^!\\{\\}\\$\\\\\\n\\r]+)\\}",
        Matcher.quoteReplacement(prefix) + "{$1}"
      )
    } else {
      result
    }
  }

  def applySchemaAndTable(sql: String, schema: String, table: String): String = {
    val withSchema = applySchema(sql, schema)
    applyTableName(withSchema, table)
  }

  def applyConfig(sql: String, config: EvolutionsDatasourceConfig): String = {
    applySchemaAndTable(sql, schema = config.schema, table = config.metaTable)
  }

  def applySchema(sql: String, schema: String): String = {
    sql.replaceAll("\\$\\{schema}", Option(schema).filter(_.trim.nonEmpty).map(_.trim + ".").getOrElse(""))
  }

  def applyTableName(sql: String, table: String): String = {
    sql.replaceAll("\\$\\{evolutions_table}", getPreparedTableName(table))
  }

  private def getPreparedTableName(tableName: String): String = {
    Option(tableName).filter(_.trim.nonEmpty).getOrElse("play_evolutions")
  }
}
