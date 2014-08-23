package anorm

/** Parsed SQL result. */
sealed trait SqlResult[+A] { self =>

  def flatMap[B](k: A => SqlResult[B]): SqlResult[B] = self match {
    case Success(a) => k(a)
    case e @ Error(_) => e
  }

  def map[B](f: A => B): SqlResult[B] = self match {
    case Success(a) => Success(f(a))
    case e @ Error(_) => e
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
  @deprecated("Use [[fold]], [[foldWhile]] or [[withIterator]] instead, which manages resources and memory", "2.4")
  def apply()(implicit connection: Connection): Stream[Row] =
    Sql.withIterator(resultSet(connection))(_.toList.toStream).
      acquireAndGet(identity)

  /**
   * Aggregates over all rows using the specified operator.
   *
   * @param z the start value
   * @param op Aggregate operator
   * @return Either list of failures at left, or aggregated value
   * @see #foldWhile
   * @see #withIterator
   */
  def fold[T](z: => T)(op: (T, Row) => T)(implicit connection: Connection): Either[List[Throwable], T] =
    Sql.withIterator(resultSet(connection))(
      _.foldLeft[T](z)(op)).acquireFor(identity)

  /**
   * Aggregates over part of or the while row stream,
   * using the specified operator.
   *
   * @param z the start value
   * @param op Aggregate operator. Returns aggregated value
   * along with true if aggregation must process next value,
   * or false to stop with current value.
   * @return Either list of failures at left, or aggregated value
   * @see #withIterator
   */
  def foldWhile[T](z: => T)(op: (T, Row) => (T, Boolean))(implicit connection: Connection): Either[List[Throwable], T] = {
    @annotation.tailrec
    def go(it: Iterator[Row], cur: T): T = if (!it.hasNext) cur else {
      val (v, cont) = op(cur, it.next)
      if (!cont) v else go(it, v)
    }

    Sql.withIterator(resultSet(connection))(go(_, z)).acquireFor(identity)
  }

  /**
   * Processes all or some rows through iterator for current results.
   *
   * @param op Operation applied with row iterator
   *
   * {{{
   * val l: Either[List[Throwable], List[Row]] = SQL"SELECT * FROM Test".
   *   withIterator(_.toList)
   * }}}
   */
  def withIterator[T](op: Iterator[Row] => T)(implicit connection: Connection): Either[List[Throwable], T] = Sql.withIterator(resultSet(connection))(op).acquireFor(identity)
}
