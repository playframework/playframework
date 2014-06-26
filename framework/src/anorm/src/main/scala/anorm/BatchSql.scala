package anorm

import java.sql.{ Connection, PreparedStatement }

private[anorm] object BatchSqlErrors {
  val HeterogeneousParameterMaps = "if each map hasn't same parameter names"
  val ParameterNamesNotMatchingPlaceholders =
    "if parameter names don't match query placeholders"
  val UnexpectedParameterName = "if `args` contains unexpected parameter name"
  val MissingParameter = "if `args` is missing some expected parameter names"
}

/** SQL batch */
sealed trait BatchSql {
  /** SQL query */
  def sql: SqlQuery

  /** Names of parameter expected for each parameter map */
  def names: Set[String]

  /** Named parameters */
  def params: Seq[Map[String, ParameterValue]] // checked: maps have same keys

  @throws[IllegalArgumentException](BatchSqlErrors.UnexpectedParameterName)
  @throws[IllegalArgumentException](BatchSqlErrors.MissingParameter)
  @throws[IllegalArgumentException](BatchSqlErrors.ParameterNamesNotMatchingPlaceholders)
  @deprecated(message = "Use [[addBatchParams]]", since = "2.3.0")
  def addBatch(args: NamedParameter*): BatchSql = {
    if (params.isEmpty) { // first parameter map
      val ps = toMap(args)
      val ks = ps.keySet

      if (!BatchSql.matchPlaceholders(sql, ks))
        throw new IllegalArgumentException(s"""Expected parameter names don't correspond to placeholders in query: ${ks mkString ", "} not matching ${sql.argsInitialOrder mkString ", "}""")

      copy(names = ks, params = Seq(ps))
    } else copy(params = this.params :+ checkedMap(args))
  }

  @throws[IllegalArgumentException](BatchSqlErrors.UnexpectedParameterName)
  @throws[IllegalArgumentException](BatchSqlErrors.MissingParameter)
  @throws[IllegalArgumentException](BatchSqlErrors.HeterogeneousParameterMaps)
  @throws[IllegalArgumentException](BatchSqlErrors.ParameterNamesNotMatchingPlaceholders)
  @deprecated(message = "Use [[addBatchParamsList]]", since = "2.3.0")
  def addBatchList(args: Traversable[Seq[NamedParameter]]): BatchSql = {
    if (params.isEmpty) BatchSql.Checked(sql, args.map(_.map(_.tupled).toMap))
    else copy(params = this.params ++ args.map(checkedMap))
  }

  /**
   * Adds a parameter map, created by zipping values with query placeholders
   * ([[SqlQuery.argsInitialOrder]]). If parameter is used for more than one
   * placeholder, it will result in a parameter map with smaller size than
   * given arguments (as duplicate entry are removed from map).
   */
  @throws[IllegalArgumentException](BatchSqlErrors.MissingParameter)
  @throws[IllegalArgumentException](BatchSqlErrors.ParameterNamesNotMatchingPlaceholders)
  def addBatchParams(args: ParameterValue*): BatchSql = {
    if (params.isEmpty) {
      BatchSql.Checked(sql,
        Seq(Sql.zipParams(sql.argsInitialOrder, args, Map.empty)))
    } else {
      val m = checkedMap(sql.argsInitialOrder.zip(args).
        foldLeft(Seq[NamedParameter]())((ps, t) =>
          ps :+ implicitly[NamedParameter](t)))

      copy(params = this.params :+ m)
    }
  }

  /**
   * Adds a parameter maps, created by zipping values with query placeholders
   * ([[SqlQuery.argsInitialOrder]]). If parameter is used for more than one
   * placeholder, it will result in parameter maps with smaller size than
   * given arguments (as duplicate entry are removed from map).
   */
  @throws[IllegalArgumentException](BatchSqlErrors.MissingParameter)
  @throws[IllegalArgumentException](BatchSqlErrors.ParameterNamesNotMatchingPlaceholders)
  def addBatchParamsList(args: Traversable[Seq[ParameterValue]]): BatchSql = {
    if (params.isEmpty) {
      BatchSql.Checked(sql,
        args.map(Sql.zipParams(sql.argsInitialOrder, _, Map.empty)))

    } else {
      val ms = args.map(x => checkedMap(sql.argsInitialOrder.zip(x).
        foldLeft(Seq[NamedParameter]())((ps, t) =>
          ps :+ implicitly[NamedParameter](t))))

      copy(params = this.params ++ ms)
    }
  }

  def getFilledStatement(connection: Connection, getGeneratedKeys: Boolean = false) = fill(connection, null, getGeneratedKeys, params)

  @deprecated(message = "Use [[getFilledStatement]]", since = "2.3.0")
  def filledStatement(implicit connection: Connection) =
    getFilledStatement(connection)

  def execute()(implicit connection: Connection): Array[Int] =
    getFilledStatement(connection).executeBatch()

  def withQueryTimeout(seconds: Option[Int]): BatchSql =
    copy(sql = sql.withQueryTimeout(seconds))

  /** Add batch parameters to given statement. */
  private def addBatchParams(stmt: PreparedStatement, ps: Seq[(Int, ParameterValue)]): PreparedStatement = {
    ps foreach { p =>
      val (i, v) = p
      v.set(stmt, i + 1)
    }
    stmt.addBatch()
    stmt
  }

  @annotation.tailrec
  private def fill(con: Connection, statement: PreparedStatement, getGeneratedKeys: Boolean = false, pm: Seq[Map[String, ParameterValue]]): PreparedStatement =
    (statement, pm.headOption) match {
      case (null, Some(ps)) => { // First
        val st: (String, Seq[(Int, ParameterValue)]) =
          Sql.prepareQuery(sql.query, 0, sql.argsInitialOrder.map(ps), Nil)

        val stmt = if (getGeneratedKeys) con.prepareStatement(sql.query, java.sql.Statement.RETURN_GENERATED_KEYS) else con.prepareStatement(sql.query)

        sql.queryTimeout.foreach(timeout => stmt.setQueryTimeout(timeout))

        fill(con, addBatchParams(stmt, st._2), getGeneratedKeys, pm.tail)
      }
      case (stmt, Some(ps)) => {
        val vs: Seq[(Int, ParameterValue)] =
          Sql.prepareQuery(sql.query, 0, sql.argsInitialOrder.map(ps), Nil)._2

        fill(con, addBatchParams(stmt, vs), getGeneratedKeys, pm.tail)
      }
      case _ => statement
    }

  @inline private def toMap(args: Seq[NamedParameter]): Map[String, ParameterValue] = args.foldLeft(Map[String, ParameterValue]())((m, np) => m + np.tupled)

  @throws[IllegalArgumentException](BatchSqlErrors.UnexpectedParameterName)
  @throws[IllegalArgumentException](BatchSqlErrors.MissingParameter)
  @inline private def checkedMap(args: Seq[NamedParameter]): Map[String, ParameterValue] = {
    val ps = args.foldLeft(Map[String, ParameterValue]()) { (m, np) =>
      if (!names.contains(np.name)) throw new IllegalArgumentException(s"""Unexpected parameter name: ${np.name} != expected ${names mkString ", "}""")
      else m + np.tupled
    }

    if (ps.size != names.size) throw new IllegalArgumentException(s"""Missing parameters: ${names.filterNot(ps.contains(_)) mkString ", "}""")

    ps
  }

  private def copy(sql: SqlQuery = this.sql, names: Set[String] = this.names, params: Seq[Map[String, ParameterValue]] = this.params) = BatchSql.Copy(sql, names, params)
}

/** SQL batch companion */
object BatchSql {
  @throws[IllegalArgumentException](BatchSqlErrors.HeterogeneousParameterMaps)
  @throws[IllegalArgumentException](BatchSqlErrors.ParameterNamesNotMatchingPlaceholders)
  def apply(query: SqlQuery, ps: Seq[Seq[NamedParameter]] = Nil): BatchSql =
    Checked(query, ps.map(_.map(_.tupled).toMap))

  @throws[IllegalArgumentException](BatchSqlErrors.HeterogeneousParameterMaps)
  @throws[IllegalArgumentException](BatchSqlErrors.ParameterNamesNotMatchingPlaceholders)
  private[anorm] def Checked[M](query: SqlQuery, ps: Traversable[Map[String, ParameterValue]]): BatchSql = ps.headOption.
    fold(Copy(query, Set.empty, Nil)) { m =>
      val ks = m.keySet

      if (!matchPlaceholders(query, ks))
        throw new IllegalArgumentException(s"""Expected parameter names don't correspond to placeholders in query: ${ks mkString ", "} not matching ${query.argsInitialOrder mkString ", "}""")

      paramNames(ps.tail, m.keySet) match {
        case Left(err) => throw new IllegalArgumentException(err)
        case Right(ns) => Copy(query, ns, ps.toSeq)
      }
    }

  /** Checks whether parameter `names` matches [[SqlQuery.argsInitialOrder]] */
  @inline private[anorm] def matchPlaceholders(query: SqlQuery, names: Set[String]): Boolean = {
    val pl = query.argsInitialOrder.toSet
    (pl.size == names.size && pl.intersect(names).size == names.size)
  }

  /** Get parameter names */
  @annotation.tailrec
  private def paramNames(ps: Traversable[Map[String, ParameterValue]], ns: Set[String]): Either[String, Set[String]] = ps.headOption match {
    case Some(m) =>
      if (ns.intersect(m.keySet).size != m.size)
        Left(s"""Unexpected parameter names: ${m.keySet mkString ", "} != expected ${ns mkString ", "}""")
      else paramNames(ps.tail, ns)
    case _ => Right(ns)
  }

  private[anorm] case class Copy(sql: SqlQuery, names: Set[String], params: Seq[Map[String, ParameterValue]]) extends BatchSql
}
