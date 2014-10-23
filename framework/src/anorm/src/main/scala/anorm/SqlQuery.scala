package anorm

import java.sql.{ Connection, PreparedStatement }
import resource.ManagedResource

/** Initial SQL query, without parameter values. */
sealed trait SqlQuery {
  /** SQL statement */
  def statement: String

  @deprecated(message = "Use [[statement]]", since = "2.3.2")
  final def query = statement

  /** Names of parameters in initial order */
  def paramsInitialOrder: List[String]

  @deprecated(message = "Use [[paramsInitialOrder]]", since = "2.3.2")
  final def argsInitialOrder = paramsInitialOrder

  /** Execution timeout */
  def timeout: Option[Int]

  @deprecated(message = "Use [[timeout]]", since = "2.3.2")
  final def queryTimeout = timeout

  @deprecated(message = "Use [[Sql.preparedStatement]]", since = "2.3.6")
  def getFilledStatement(connection: Connection, getGeneratedKeys: Boolean = false): PreparedStatement = asSimple.getFilledStatement(connection, getGeneratedKeys)

  /** Returns this query with timeout updated to `seconds` delay. */
  def withQueryTimeout(seconds: Option[Int]): SqlQuery = SqlQuery.prepare(statement, paramsInitialOrder, seconds)

  private[anorm] def asSimple: SimpleSql[Row] = asSimple(defaultParser)

  /**
   * Prepares query as a simple one.
   * @param parser Row parser
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * SQL("SELECT 1").asSimple(SqlParser.scalar[Int])
   * }}}
   */
  def asSimple[T](parser: RowParser[T] = defaultParser): SimpleSql[T] =
    SimpleSql(this, Map.empty, parser)

  @deprecated(message = """Directly use BatchSql("stmt")""", since = "2.3.2")
  def asBatch[T]: BatchSql = BatchSql(this.query, Nil)

  private def defaultParser: RowParser[Row] = RowParser(Success(_))

  // TODO: Make it private
  @deprecated(
    "Use anorm.SQL(…) as SqlQuery should not be directly created", "2.3.2")
  def copy(statement: String = this.statement, paramsInitialOrder: List[String] = this.paramsInitialOrder, timeout: Option[Int] = this.timeout) = SqlQuery(statement, paramsInitialOrder, timeout)
}

/* TODO: Make it private[anorm] to prevent SqlQuery from being created with
 unchecked properties (e.g. unchecked/unparsed statement). */
object SqlQuery {

  @deprecated(message = "Use anorm.SQL(…)", since = "2.3.2")
  def apply(st: String, params: List[String] = List.empty, tmout: Option[Int] = None): SqlQuery = prepare(st, params, tmout)

  /**
   * Returns prepared SQL query.
   *
   * @param st SQL statement (see [[SqlQuery.statement]])
   * @param params Parameter names in initial order (see [[SqlQuery.paramsInitialOrder]])
   * @param tmout Query execution timeout (see [[SqlQuery.timeout]])
   */
  private[anorm] def prepare(st: String, params: List[String] = List.empty, tmout: Option[Int] = None): SqlQuery = new SqlQuery {
    val statement = st
    val paramsInitialOrder = params
    val timeout = tmout
  }

  /** Extractor for pattern matching */
  def unapply(query: SqlQuery): Option[(String, List[String], Option[Int])] =
    Option(query).map(q => (q.statement, q.paramsInitialOrder, q.timeout))
}
