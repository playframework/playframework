/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package anorm

import SqlParser.ResultSet

object SqlParser {
  import MayErr._
  import java.util.Date

  type ResultSet = Stream[Row]

  private val NoColumnsInReturnedResult = SqlMappingError("No column in result")

  /**
   * Returns parser for a scalar not-null value.
   *
   * {{{
   * val count = SQL("select count(*) from Country").as(scalar[Long].single)
   * }}}
   */
  def scalar[T](implicit transformer: Column[T]): RowParser[T] =
    new ScalarRowParser[T] {
      def apply(row: Row): SqlResult[T] = {
        (for {
          meta <- row.metaData.ms.headOption.toRight(NoColumnsInReturnedResult)
          value <- row.data.headOption.toRight(NoColumnsInReturnedResult)
          result <- transformer(value, meta)
        } yield result).fold(e => Error(e), a => Success(a))
      }
    }

  /**
   * Flatten columns tuple-like.
   *
   * {{{
   * import anorm.SQL
   * import anorm.SqlParser.{ long, str, int }
   *
   * val tuple: (Long, String, Int) = SQL("SELECT a, b, c FROM Test").
   *   as(long("a") ~ str("b") ~ int("c") map (SqlParser.flatten) single)
   * }}}
   */
  def flatten[T1, T2, R](implicit f: TupleFlattener[(T1 ~ T2) => R]): ((T1 ~ T2) => R) = f.f

  /**
   * Parses specified column as float.
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Float, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.float("a") ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def float(columnName: String): RowParser[Float] =
    get[Float](columnName)(implicitly[Column[Float]]) // TODO: Review implicit

  /**
   * Parses specified column as string.
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Float, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.float("a") ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def str(columnName: String): RowParser[String] =
    get[String](columnName)(implicitly[anorm.Column[String]])

  /**
   * Parses specified column as boolean.
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Boolean, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.bool("a") ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def bool(columnName: String): RowParser[Boolean] =
    get[Boolean](columnName)(implicitly[Column[Boolean]])

  /**
   * Parses specified column as byte.
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Byte, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.byte("a") ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def byte(columnName: String): RowParser[Byte] =
    get[Byte](columnName)(implicitly[Column[Byte]])

  /**
   * Parses specified column as double.
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Double, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.double("a") ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def double(columnName: String): RowParser[Double] =
    get[Double](columnName)(implicitly[Column[Double]])

  /**
   * Parses specified column as short.
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Short, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.short("a") ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def short(columnName: String): RowParser[Short] =
    get[Short](columnName)(implicitly[Column[Short]])

  /**
   * Parses specified column as integer.
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Int, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.int("a") ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def int(columnName: String): RowParser[Int] =
    get[Int](columnName)(implicitly[Column[Int]])

  /**
   * Parses specified column as long.
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Long, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.long("a") ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def long(columnName: String): RowParser[Long] =
    get[Long](columnName)(implicitly[Column[Long]])

  /**
   * Parses specified column as date.
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Date, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.date("a") ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def date(columnName: String): RowParser[Date] =
    get[Date](columnName)(implicitly[Column[Date]])

  def getAliased[T](aliasName: String)(implicit extractor: Column[T]): RowParser[T] = RowParser { row =>
    import MayErr._

    (for {
      meta <- row.metaData.getAliased(aliasName)
        .toRight(ColumnNotFound(aliasName, row.metaData.availableColumns))
      value <- row.getAliased(aliasName)
      result <- extractor(value, MetaDataItem(meta._1, meta._2, meta._3))
    } yield result).fold(e => Error(e), a => Success(a))
  }

  // TODO: Scaladoc
  def get[T](columnName: String)(implicit extractor: Column[T]): RowParser[T] = RowParser { row =>
    import MayErr._

    (for {
      meta <- row.metaData.get(columnName)
        .toRight(ColumnNotFound(columnName, row.metaData.availableColumns))
      value <- row.get1(columnName)
      result <- extractor(value, MetaDataItem(meta._1, meta._2, meta._3))
    } yield result).fold(e => Error(e), a => Success(a))
  }

  def contains[TT: Column, T <: TT](columnName: String, t: T): RowParser[Unit] =
    get[TT](columnName)(implicitly[Column[TT]]).
      collect(s"Row doesn't contain a column: $columnName with value $t") {
        case a if a == t => Unit
      }

}

/** Columns tuple-like */
// Using List or HList?
final case class ~[+A, +B](_1: A, _2: B)

object RowParser {
  def apply[A](f: Row => SqlResult[A]): RowParser[A] = new RowParser[A] {
    def apply(row: Row): SqlResult[A] = f(row)
  }
}

trait RowParser[+A] extends (Row => SqlResult[A]) { parent =>

  def map[B](f: A => B): RowParser[B] = RowParser(parent.andThen(_.map(f)))

  def collect[B](otherwise: String)(f: PartialFunction[A, B]): RowParser[B] =
    RowParser(row => parent(row).flatMap(a =>
      if (f.isDefinedAt(a)) Success(f(a))
      else Error(SqlMappingError(otherwise))))

  def flatMap[B](k: A => RowParser[B]): RowParser[B] =
    RowParser(row => parent(row).flatMap(a => k(a)(row)))

  def ~[B](p: RowParser[B]): RowParser[A ~ B] =
    RowParser(row => parent(row).flatMap(a => p(row).map(new ~(a, _))))

  def ~>[B](p: RowParser[B]): RowParser[B] =
    RowParser(row => parent(row).flatMap(a => p(row)))

  def <~[B](p: RowParser[B]): RowParser[A] = parent.~(p).map(_._1)

  def |[B >: A](p: RowParser[B]): RowParser[B] = RowParser { row =>
    parent(row) match {
      case Error(_) => p(row)
      case a => a
    }
  }

  /**
   * Returns a row parser for optional column,
   * that will turn missing or null column as None.
   */
  def ? : RowParser[Option[A]] = RowParser { row =>
    parent(row) match {
      case Success(a) => Success(Some(a))
      case Error(UnexpectedNullableFound(_)) | Error(ColumnNotFound(_, _)) =>
        Success(None)
      case e @ Error(f) => e
    }
  }

  def >>[B](f: A => RowParser[B]): RowParser[B] = flatMap(f)

  def * : ResultSetParser[List[A]] = ResultSetParser.list(parent)

  def + : ResultSetParser[List[A]] = ResultSetParser.nonEmptyList(parent)

  /**
   * Returns a result set parser expecting exactly one row to parse.
   *
   * {{{
   * val b: Boolean = SQL("SELECT flag FROM Test WHERE id = :id").
   *   on("id" -> 1).as(scalar[Boolean].single)
   * }}}
   */
  def single = ResultSetParser.single(parent)

  /**
   * Returns a result set parser for none or one parsed row.
   *
   * {{{
   * val name: Option[String] =
   *   SQL("SELECT name FROM Country WHERE lang = :lang")
   *   .on("lang" -> "notFound").as(scalar[String].singleOpt)
   * }}}
   */
  def singleOpt: ResultSetParser[Option[A]] = ResultSetParser.singleOpt(parent)

}

sealed trait ScalarRowParser[+A] extends RowParser[A] {
  override def singleOpt: ResultSetParser[Option[A]] = ResultSetParser {
    case head #:: Stream.Empty if (head.data.headOption == Some(null)) =>
      // one column present in head row, but column value is null
      Success(None)
    case head #:: Stream.Empty => map(Some(_))(head)
    case Stream.Empty => Success(None)
    case _ => Error(SqlMappingError("too many rows when expecting a single one"))
  }
}

trait ResultSetParser[+A] extends (ResultSet => SqlResult[A]) { parent =>
  def map[B](f: A => B): ResultSetParser[B] =
    ResultSetParser(rs => parent(rs).map(f))

}

object ResultSetParser {
  def apply[A](f: ResultSet => SqlResult[A]): ResultSetParser[A] =
    new ResultSetParser[A] { rows =>
      def apply(rows: ResultSet): SqlResult[A] = f(rows)
    }

  def list[A](p: RowParser[A]): ResultSetParser[List[A]] = {
    // Performance note: sequence produces a List in reverse order, since appending to a
    // List is an O(n) operation, and this is done n times, yielding O(n2) just to convert the
    // result set to a List.  Prepending is O(1), so we use prepend, and then reverse the result
    // in the map function below.
    @annotation.tailrec
    def sequence(results: SqlResult[List[A]], rows: Stream[Row]): SqlResult[List[A]] = {
      (results, rows) match {
        case (Success(rs), row #:: tail) => sequence(p(row).map(_ +: rs), tail)
        case (r, _) => r
      }
    }

    ResultSetParser { rows => sequence(Success(List()), rows).map(_.reverse) }
  }

  def nonEmptyList[A](p: RowParser[A]): ResultSetParser[List[A]] =
    ResultSetParser(rows =>
      if (rows.isEmpty) Error(SqlMappingError("Empty Result Set"))
      else list(p)(rows))

  def single[A](p: RowParser[A]): ResultSetParser[A] = ResultSetParser {
    case head #:: Stream.Empty => p(head)
    case Stream.Empty =>
      Error(SqlMappingError("No rows when expecting a single one"))
    case _ =>
      Error(SqlMappingError("too many rows when expecting a single one"))

  }

  def singleOpt[A](p: RowParser[A]): ResultSetParser[Option[A]] =
    ResultSetParser {
      case head #:: Stream.Empty => p.map(Some(_))(head)
      case Stream.Empty => Success(None)
      case _ => Error(SqlMappingError("too many rows when expecting a single one"))
    }

}
