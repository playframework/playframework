package anorm

/** Parsed SQL result. */
sealed trait SqlResult[+A] { self =>
  // TODO: Review along with MayErr (unify?)

  def flatMap[B](k: A => SqlResult[B]): SqlResult[B] = self match {
    case Success(a) => k(a)
    case e @ Error(_) => e
  }

  def map[B](f: A => B): SqlResult[B] = self match {
    case Success(a) => Success(f(a))
    case e @ Error(_) => e
  }

  /**
   * Either applies function `e` if result is erroneous,
   * or function `f` with successful result if any.
   */
  def fold[B](e: SqlRequestError => B, f: A => B): B = self match {
    case Success(a) => f(a)
    case Error(err) => e(err)
  }
}

/** Successfully parsed result. */
case class Success[A](a: A) extends SqlResult[A]

/** Erroneous result (failure while parsing). */
case class Error(msg: SqlRequestError) extends SqlResult[Nothing]

private[anorm] trait WithResult {
  import java.sql.Connection

  /** Returns underlying result set */
  protected def resultSet(connection: Connection): resource.ManagedResource[java.sql.ResultSet]

  /**
   * Executes this SQL statement as query, returns result as Row stream.
   */
  @deprecated(
    "Use [[fold]], [[foldWhile]] or [[withResult]] instead, which manages resources and memory", "2.4")
  def apply()(implicit connection: Connection): Stream[Row] = {
    @annotation.tailrec
    def go(c: Option[Cursor], s: Stream[Row]): Stream[Row] = c match {
      case Some(cursor) => go(cursor.next, s :+ cursor.row)
      case _ => s
    }

    Sql.withResult(resultSet(connection))(go(_, Stream.empty[Row])).acquireAndGet(identity)
  }

  /**
   * Aggregates over all rows using the specified operator.
   *
   * @param z the start value
   * @param op Aggregate operator
   * @return Either list of failures at left, or aggregated value
   * @see #foldWhile
   * @see #withResult
   */
  def fold[T](z: => T)(op: (T, Row) => T)(implicit connection: Connection): Either[List[Throwable], T] = {
    @annotation.tailrec
    def go(c: Option[Cursor], cur: T): T = c match {
      case Some(cursor) => go(cursor.next, op(cur, cursor.row))
      case _ => cur
    }

    withResult(go(_, z))
  }

  /**
   * Aggregates over part of or the while row stream,
   * using the specified operator.
   *
   * @param z the start value
   * @param op Aggregate operator. Returns aggregated value along with true if aggregation must process next value, or false to stop with current value.
   * @return Either list of failures at left, or aggregated value
   * @see #withResult
   */
  def foldWhile[T](z: => T)(op: (T, Row) => (T, Boolean))(implicit connection: Connection): Either[List[Throwable], T] = {
    @annotation.tailrec
    def go(c: Option[Cursor], cur: T): T = c match {
      case Some(cursor) =>
        val (v, cont) = op(cur, cursor.row)
        if (!cont) v else go(cursor.next, v)
      case _ => cur
    }

    withResult(go(_, z))
  }

  /**
   * Processes all or some rows from current result.
   *
   * @param op Operation applied with row cursor
   *
   * {{{
   * @annotation.tailrec
   * def go(c: Option[Cursor], l: List[Row]): List[Row] = c match {
   *   case Some(cursor) => go(cursor.next, l :+ cursor.row)
   *   case _ => l
   * }
   *
   * val l: Either[List[Throwable], List[Row]] =
   *   SQL"SELECT * FROM Test".withResult(go)
   * }}}
   */
  def withResult[T](op: Option[Cursor] => T)(implicit connection: Connection): Either[List[Throwable], T] = Sql.withResult(resultSet(connection))(op).acquireFor(identity)

  /**
   * Converts this query result as `T`, using parser.
   */
  def as[T](parser: ResultSetParser[T])(implicit connection: Connection): T =
    Sql.asTry(parser, resultSet(connection)).get // TODO: Safe alternative

}
