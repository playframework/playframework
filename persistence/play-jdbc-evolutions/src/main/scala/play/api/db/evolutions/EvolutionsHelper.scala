/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db.evolutions

private[evolutions] object EvolutionsHelper {
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
