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

  implicit def toParameterValue[A](a: A)(implicit s: ToSql[A] = null, p: ToStatement[A]): ParameterValue = ParameterValue(a, s, p)

  /**
   * Creates an SQL query with given statement.
   * @param stmt SQL statement
   *
   * {{{
   * val query = SQL("SELECT * FROM Country")
   * }}}
   */
  def SQL(stmt: String): SqlQuery = Sql.sql(stmt)

  /**
   * Creates an SQL query using String Interpolation feature.
   * It is a 1-step alternative for SQL().on() functions.
   *
   * {{{
   * val query = SQL"SELECT * FROM Country"
   * }}}
   */
  implicit class SqlStringInterpolation(val sc: StringContext) extends AnyVal {
    def SQL(args: ParameterValue*) = prepare(args)

    private def prepare(params: Seq[ParameterValue]) = {
      // Generates the string query with "%s" for each parameter placeholder
      val sql = sc.parts.mkString("%s")

      val (ns, ps): (List[String], Map[String, ParameterValue]) =
        namedParams(params)

      SimpleSql(SqlQuery(sql, ns), ps,
        defaultParser = RowParser(row => Success(row)))
    }
  }

  /* Prepares parameter mappings, arbitrary names and converted values. */
  @annotation.tailrec
  private[anorm] def namedParams(params: Seq[ParameterValue], i: Int = 0, names: List[String] = List.empty, named: Map[String, ParameterValue] = Map.empty): (List[String], Map[String, ParameterValue]) = params.headOption match {
    case Some(p) =>
      val n = '_'.toString + i
      namedParams(params.tail, i + 1, names :+ n, named + (n -> p))
    case _ => (names, named)
  }

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
