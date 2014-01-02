/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

/**
 * Anorm API
 *
 * Use the SQL method to start an SQL query
 *
 * {{{
 * import anorm._
 *
 * SQL("Select 1")
 * }}}
 */
package object anorm {
  import scala.language.implicitConversions

  implicit def sqlToSimple(sql: SqlQuery): SimpleSql[Row] = sql.asSimple
  implicit def sqlToBatch(sql: SqlQuery): BatchSql = sql.asBatch

  implicit def implicitID[ID](id: Id[ID] with NotNull): ID = id.id

  implicit def toParameterValue[A](a: A)(implicit p: ToStatement[A]): ParameterValue = ParameterValue(a, p)

  /**
   * Creates an SQL query with given statement.
   * @param stmt SQL statement
   *
   * {{{
   * val query = SQL("SELECT * FROM Country")
   * }}}
   */
  def SQL(stmt: String): SqlQuery = Sql.sql(stmt)

  /** Activable features */
  object features {

    /**
     * Conversion for parameter with untyped named.
     *
     * {{{
     * // For backward compatibility
     * import anorm.features.parameterWithUntypedName
     *
     * val untyped: Any = "name"
     * SQL("SELECT * FROM Country WHERE {p}").on(untyped -> "val")
     * }}}
     */
    @deprecated(
      message = "Use typed name for parameter, either string or symbol",
      since = "2.3.0")
    implicit def parameterWithUntypedName[V](t: (Any, V))(implicit c: V => ParameterValue): NamedParameter = NamedParameter(t._1.toString, c(t._2))

  }
}
