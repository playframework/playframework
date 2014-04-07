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
        } yield result).fold(Error(_), Success(_))
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
  def float(columnName: String)(implicit c: Column[Float]): RowParser[Float] =
    get[Float](columnName)(c)

  /**
   * Parses specified column as float.
   * @param columnPosition from 1 to n
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Float, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.float(1) ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def float(columnPosition: Int)(implicit c: Column[Float]): RowParser[Float] =
    get[Float](columnPosition)(c)

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
  def str(columnName: String)(implicit c: Column[String]): RowParser[String] =
    get[String](columnName)(c)

  /**
   * Parses specified column as string.
   * @param columnPosition from 1 to n
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Float, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.float("a") ~ SqlParser.str(1) map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def str(columnPosition: Int)(implicit c: Column[String]): RowParser[String] =
    get[String](columnPosition)(c)

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
  def bool(columnName: String)(implicit c: Column[Boolean]): RowParser[Boolean] = get[Boolean](columnName)(c)

  /**
   * Parses specified column as boolean.
   * @param columnPosition from 1 to n
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Boolean, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.bool(1) ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def bool(columnPosition: Int)(implicit c: Column[Boolean]): RowParser[Boolean] = get[Boolean](columnPosition)(c)

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
  def byte(columnName: String)(implicit c: Column[Byte]): RowParser[Byte] =
    get[Byte](columnName)(c)

  /**
   * Parses specified column as byte.
   * @param columnPosition from 1 to n
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Byte, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.byte(1) ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def byte(columnPosition: Int)(implicit c: Column[Byte]): RowParser[Byte] =
    get[Byte](columnPosition)(c)

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
  def double(columnName: String)(implicit c: Column[Double]): RowParser[Double] = get[Double](columnName)(c)

  /**
   * Parses specified column as double.
   * @param columnPosition from 1 to n
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Double, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.double(1) ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def double(columnPosition: Int)(implicit c: Column[Double]): RowParser[Double] = get[Double](columnPosition)(c)

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
  def short(columnName: String)(implicit c: Column[Short]): RowParser[Short] =
    get[Short](columnName)(c)

  /**
   * Parses specified column as short.
   * @param columnPosition from 1 to n
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Short, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.short(1) ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def short(columnPosition: Int)(implicit c: Column[Short]): RowParser[Short] =
    get[Short](columnPosition)(c)

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
  def int(columnName: String)(implicit c: Column[Int]): RowParser[Int] =
    get[Int](columnName)(c)

  /**
   * Parses specified column as integer.
   * @param columnPosition from 1 to n
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Int, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.int(1) ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def int(columnPosition: Int)(implicit c: Column[Int]): RowParser[Int] =
    get[Int](columnPosition)(c)

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
  def long(columnName: String)(implicit c: Column[Long]): RowParser[Long] =
    get[Long](columnName)(c)

  /**
   * Parses specified column as long.
   * @param columnPosition from 1 to n
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Long, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.long(1) ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def long(columnPosition: Int)(implicit c: Column[Long]): RowParser[Long] =
    get[Long](columnPosition)(c)

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
  def date(columnName: String)(implicit c: Column[Date]): RowParser[Date] =
    get[Date](columnName)(c)

  /**
   * Parses specified column as date.
   * @param columnPosition from 1 to n
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val t: (Date, String) = SQL("SELECT a, b FROM test")
   *   .as(SqlParser.date(1) ~ SqlParser.str("b") map (
   *     SqlParser.flatten) single)
   * }}}
   */
  def date(columnPosition: Int)(implicit c: Column[Date]): RowParser[Date] =
    get[Date](columnPosition)(c)

  def getAliased[T](aliasName: String)(implicit extractor: Column[T]): RowParser[T] = RowParser { row =>
    (for {
      col <- row.getAliased(aliasName)
      res <- extractor.tupled(col)
    } yield res).fold(Error(_), Success(_))
  }

  // TODO: Scaladoc
  def get[T](columnName: String)(implicit extractor: Column[T]): RowParser[T] =
    RowParser { row =>
      (for {
        col <- row.get1(columnName)
        res <- extractor.tupled(col)
      } yield res).fold(Error(_), Success(_))
    }

  /**
   * Returns row parser for column at given position.
   * @param position Column position, from 1 to n
   *
   * {{{
   * val res: (Float, String) = // parsing columns #1 & #3
   *   SQL("SELECT * FROM Test").as(get[String](1) ~ get[Float](3) map {
   *     case str ~ f => (f -> str)
   *   } *)
   * }}}
   */
  def get[T](position: Int)(implicit extractor: Column[T]): RowParser[T] =
    RowParser { row =>
      (for {
        col <- row.getIndexed(position - 1)
        result <- extractor.tupled(col)
      } yield result).fold(e => Error(e), a => Success(a))
    }

  /**
   * Returns row parser which throws exception if specified `column` is either
   * missing or not matching expected `value`.
   * If row contains described column, do nothing (Unit).
   *
   * {{{
   * import anorm.SQL
   * import anorm.SqlParser.{ contains, str }
   *
   * val parser = contains("a", true) ~ str("b") map {
   *   case () ~ str => str
   * }
   *
   * SQL("SELECT * FROM table").as(parser.+)
   * // Throws exception if there no |a| column or if |a| is not true,
   * // otherwise parses as non-empty list of |b| strings.
   * }}}
   */
  @throws[RuntimeException](
    "SqlMappingError(Row doesn't contain a column: f with value 2.34)")
  @deprecated(message = "Use [[matches]]", since = "2.3.0")
  def contains[TT: Column, T <: TT](column: String, value: T): RowParser[Unit] =
    get[TT](column)(implicitly[Column[TT]]).
      collect(s"Row doesn't contain a column: $column with value $value") {
        case a if a == value => Unit
      }

  /**
   * Returns row parser which true if specified `column` is found
   * and matching expected `value`.
   *
   * {{{
   * import anorm.SQL
   * import anorm.SqlParser.matches
   *
   * val m: Boolean = SQL("SELECT * FROM table").as(matches("a", 1.2f).single)
   * // true if column |a| is found and matching 1.2f, otherwise false
   * }}}
   *
   * @return true if matches, or false if not
   */
  def matches[TT: Column, T <: TT](column: String, value: T)(implicit c: Column[TT]): RowParser[Boolean] = get[TT](column)(c).?.map(_.fold(false)(_ == value))

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

  /**
   * Returns parser which collects information
   * from already parsed row data using `f`.
   *
   * @param otherwise Message returned as error if nothing can be collected using `f`.
   * @param f Collecting function
   */
  def collect[B](otherwise: String)(f: PartialFunction[A, B]): RowParser[B] =
    RowParser(parent(_).flatMap(f.lift(_).
      fold[SqlResult[B]](Error(SqlMappingError(otherwise)))(Success(_))))

  def flatMap[B](k: A => RowParser[B]): RowParser[B] =
    RowParser(row => parent(row).flatMap(k(_)(row)))

  def ~[B](p: RowParser[B]): RowParser[A ~ B] =
    RowParser(row => parent(row).flatMap(a => p(row).map(new ~(a, _))))

  def ~>[B](p: RowParser[B]): RowParser[B] =
    RowParser(row => parent(row).flatMap(_ => p(row)))

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
  def ? : RowParser[Option[A]] = RowParser {
    parent(_) match {
      case Success(a) => Success(Some(a))
      case Error(UnexpectedNullableFound(_)) | Error(ColumnNotFound(_, _)) =>
        Success(None)
      case e @ Error(f) => e
    }
  }

  def >>[B](f: A => RowParser[B]): RowParser[B] = flatMap(f)

  /**
   * Returns possibly empty list parsed from result.
   *
   * {{{
   * val price = 125
   * SQL"SELECT name FROM item WHERE price < $price".as(scalar[String].*)
   * }}}
   */
  def * : ResultSetParser[List[A]] = ResultSetParser.list(parent)

  /**
   * Returns non empty list parse from result,
   * or raise error if there is no result.
   *
   * {{{
   * import anorm.SQL
   * import anorm.SqlParser.str
   *
   * val parser = str("title") ~ str("descr")
   * SQL("SELECT title, descr FROM pages").as(parser.+) // at least 1 page
   * }}}
   */
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

/** Parser for scalar row (row of one single column). */
sealed trait ScalarRowParser[+A] extends RowParser[A] {
  override def singleOpt: ResultSetParser[Option[A]] = ResultSetParser {
    case head #:: Stream.Empty if (head.data.headOption == Some(null)) =>
      // one column present in head row, but column value is null
      Success(None)
    case head #:: Stream.Empty => map(Some(_))(head)
    case Stream.Empty => Success(None)
    case _ => Error(SqlMappingError(
      "too many rows when expecting a single one"))
  }
}

trait ResultSetParser[+A] extends (ResultSet => SqlResult[A]) { parent =>
  def map[B](f: A => B): ResultSetParser[B] =
    ResultSetParser(rs => parent(rs).map(f))

}

private[anorm] object ResultSetParser {
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
    case Stream.Empty => Error(SqlMappingError(
      "No rows when expecting a single one"))

    case _ => Error(SqlMappingError(
      "too many rows when expecting a single one"))

  }

  def singleOpt[A](p: RowParser[A]): ResultSetParser[Option[A]] =
    ResultSetParser {
      case head #:: Stream.Empty => p.map(Some(_))(head)
      case Stream.Empty => Success(None)
      case _ => Error(SqlMappingError(
        "too many rows when expecting a single one"))
    }

}
