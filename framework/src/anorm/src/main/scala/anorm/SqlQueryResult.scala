package anorm

import java.sql.{ Connection, SQLWarning }

import resource.ManagedResource

/**
 * A result from execution of an SQL query, row data and context
 * (e.g. statement warnings).
 *
 * @constructor create a result with a result set
 * @param resultSet Result set from executed query
 */
final case class SqlQueryResult(
    /** Underlying result set */
    resultSet: ManagedResource[java.sql.ResultSet]) extends WithResult {

  protected def resultSet(c: java.sql.Connection) = resultSet

  /** Query statement already executed */
  val statement: ManagedResource[java.sql.Statement] =
    resultSet.map(_.getStatement)

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
  def statementWarning: Option[SQLWarning] =
    statement.acquireFor(_.getWarnings).fold[Option[SQLWarning]](
      _.headOption.map(new SQLWarning(_)), Option(_))

  /** Applies current parser with optionnal list of rows (0..n). */
  @deprecated(
    message = """Use `SQL("...").executeQuery().as(parser.*)`""",
    since = "2.3.5")
  def list[A](rowParser: RowParser[A])(implicit connection: Connection): Seq[A] = as(rowParser.*)

  /** Applies current parser to exactly on row. */
  @deprecated(
    message = """Use `SQL("...").executeQuery().as(parser.single)`""",
    since = "2.3.5")
  def single[A](rowParser: RowParser[A])(implicit connection: Connection): A =
    as(ResultSetParser.single(rowParser))

  /** Applies current parser to one optional row. */
  @deprecated(
    message = """Use `SQL("...").executeQuery().as(parser.singleOpt)`""",
    since = "2.3.5")
  def singleOpt[A](rowParser: RowParser[A])(implicit connection: Connection): Option[A] = as(ResultSetParser.singleOpt(rowParser))

  @deprecated(message = "Use [[as]]", since = "2.3.2")
  def parse[T](parser: ResultSetParser[T])(implicit connection: Connection): T =
    as(parser)

}
