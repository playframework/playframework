package anorm

import java.sql.Connection

/**
 * A result from execution of an SQL query, row data and context
 * (e.g. statement warnings).
 *
 * @constructor create a result with a result set
 * @param resultSet Result set from executed query
 */
final case class SqlQueryResult(resultSet: java.sql.ResultSet) {
  /** Query statement already executed */
  val statement: java.sql.Statement = resultSet.getStatement

  /**
   * Returns statement warning if there is some for this result.
   *
   * {{{
   * val res = SQL("EXEC stored_proc {p}").on("p" -> paramVal).executeQuery()
   * res.statementWarning match {
   *   case Some(warning) =>
   *     warning.printStackTrace()
   *     None
   *
   *   case None =>
   *     // go on with row parsing ...
   *     res.as(scalar[String].singleOpt)
   * }
   * }}}
   */
  def statementWarning: Option[java.sql.SQLWarning] =
    Option(statement.getWarnings)

  /** Returns stream of row from query result. */
  def apply()(implicit connection: Connection): Stream[Row] =
    Sql.resultSetToStream(resultSet)

  def as[T](parser: ResultSetParser[T])(implicit connection: Connection): T =
    parser(Sql.resultSetToStream(resultSet)) match {
      case Success(a) => a
      case Error(e) => sys.error(e.toString)
    }

  def list[A](rowParser: RowParser[A])(implicit connection: Connection): Seq[A] = as(rowParser.*)

  def single[A](rowParser: RowParser[A])(implicit connection: Connection): A =
    as(ResultSetParser.single(rowParser))

  def singleOpt[A](rowParser: RowParser[A])(implicit connection: Connection): Option[A] = as(ResultSetParser.singleOpt(rowParser))

  def parse[T](parser: ResultSetParser[T])(implicit connection: Connection): T =
    parser(Sql.resultSetToStream(resultSet)) match {
      case Success(a) => a
      case Error(e) => sys.error(e.toString)
    }

}
