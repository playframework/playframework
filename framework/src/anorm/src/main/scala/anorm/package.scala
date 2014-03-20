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

  /**
   * Marker trait to indicate that even if a type T accept null as value,
   * it must be refused in some Anorm context.
   */
  trait NotNullGuard

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
   * It is a 1-step alternative for SQL("...").on(...) functions.
   *
   * {{{
   * SQL"""
   *   UPDATE computer SET name = ${computer.name},
   *   introduced = ${computer.introduced},
   *   discontinued = ${computer.discontinued},
   *   company_id = ${computer.companyId}
   *   WHERE id = $id
   * """.executeUpdate()
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

    /**
     * Unsafe conversion from untyped value to statement parameter.
     * Value will be passed using setObject.
     *
     * It's not recommanded to use it as it can hide conversion issue.
     *
     * {{{
     * // For backward compatibility
     * import anorm.features.anyToStatement
     *
     * val d = new java.util.Date()
     * val params: Seq[NamedParameter] = Seq("mod" -> d, "id" -> "idv")
     * // Values as Any as heterogenous
     *
     * SQL("UPDATE item SET last_modified = {mod} WHERE id = {id}").
     *   on(params:_*)
     * // date and string passed with setObject, rather than
     * // setDate and setString.
     * }}}
     */
    @deprecated(
      message = "Do not passed parameter as untyped/Any value",
      since = "2.3.0")
    implicit def anyToStatement[T] = new ToStatement[T] {
      def set(s: java.sql.PreparedStatement, i: Int, any: T): Unit =
        s.setObject(i, any)
    }
  }
}
