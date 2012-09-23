package anorm

import SqlParser.ResultSet

object SqlParser {
  import MayErr._
  import java.util.Date

  type ResultSet = Stream[Row]

  def scalar[T](implicit transformer: Column[T]): RowParser[T] = RowParser[T] { row =>

    (for {
      meta <- row.metaData.ms.headOption.toRight(NoColumnsInReturnedResult)
      value <- row.data.headOption.toRight(NoColumnsInReturnedResult)
      result <- transformer(value, meta)
    } yield result).fold(e => Error(e), a => Success(a))
  }

  def flatten[T1, T2, R](implicit f: anorm.TupleFlattener[(T1 ~ T2) => R]): ((T1 ~ T2) => R) = f.f

  def str(columnName: String): RowParser[String] = get[String](columnName)(implicitly[anorm.Column[String]])

  def bool(columnName: String): RowParser[Boolean] = get[Boolean](columnName)(implicitly[Column[Boolean]])

  def int(columnName: String): RowParser[Int] = get[Int](columnName)(implicitly[Column[Int]])

  def long(columnName: String): RowParser[Long] = get[Long](columnName)(implicitly[Column[Long]])

  def date(columnName: String): RowParser[Date] = get[Date](columnName)(implicitly[Column[Date]])

  def getAliased[T](aliasName: String)(implicit extractor: anorm.Column[T]): RowParser[T] = RowParser { row =>
    import MayErr._

    (for {
      meta <- row.metaData.getAliased(aliasName)
        .toRight(ColumnNotFound(aliasName, row.metaData.availableColumns))
      value <- row.getAliased(aliasName)
      result <- extractor(value, MetaDataItem(meta._1, meta._2, meta._3))
    } yield result).fold(e => Error(e), a => Success(a))
  }

  def get[T](columnName: String)(implicit extractor: anorm.Column[T]): RowParser[T] = RowParser { row =>
    import MayErr._

    (for {
      meta <- row.metaData.get(columnName)
        .toRight(ColumnNotFound(columnName, row.metaData.availableColumns))
      value <- row.get1(columnName)
      result <- extractor(value, MetaDataItem(meta._1, meta._2, meta._3))
    } yield result).fold(e => Error(e), a => Success(a))
  }

  def contains[TT: Column, T <: TT](columnName: String, t: T): RowParser[Unit] =
    get[TT](columnName)(implicitly[Column[TT]])
      .collect("Row doesn't contain a column: " + columnName + " with value " + t) { case a if a == t => Unit }

}

case class ~[+A, +B](_1: A, _2: B)

trait SqlResult[+A] {

  self =>

  def flatMap[B](k: A => SqlResult[B]): SqlResult[B] = self match {

    case Success(a) => k(a)
    case e @ Error(_) => e

  }

  def map[B](f: A => B): SqlResult[B] = self match {

    case Success(a) => Success(f(a))
    case e @ Error(_) => e

  }

}

case class Success[A](a: A) extends SqlResult[A]

case class Error(msg: SqlRequestError) extends SqlResult[Nothing]

object RowParser {

  def apply[A](f: Row => SqlResult[A]): RowParser[A] = new RowParser[A] {

    def apply(row: Row): SqlResult[A] = f(row)

  }

}

trait RowParser[+A] extends (Row => SqlResult[A]) {

  parent =>

  def map[B](f: A => B): RowParser[B] = RowParser(parent.andThen(_.map(f)))

  def collect[B](otherwise: String)(f: PartialFunction[A, B]): RowParser[B] = RowParser(row => parent(row).flatMap(a => if (f.isDefinedAt(a)) Success(f(a)) else Error(SqlMappingError(otherwise))))

  def flatMap[B](k: A => RowParser[B]): RowParser[B] = RowParser(row => parent(row).flatMap(a => k(a)(row)))

  def ~[B](p: RowParser[B]): RowParser[A ~ B] = RowParser(row => parent(row).flatMap(a => p(row).map(new ~(a, _))))

  def ~>[B](p: RowParser[B]): RowParser[B] = RowParser(row => parent(row).flatMap(a => p(row)))

  def <~[B](p: RowParser[B]): RowParser[A] = parent.~(p).map(_._1)

  def |[B >: A](p: RowParser[B]): RowParser[B] = RowParser { row =>
    parent(row) match {

      case Error(_) => p(row)

      case a => a

    }
  }

  def ? : RowParser[Option[A]] = RowParser { row =>
    parent(row) match {
      case Success(a) => Success(Some(a))
      case Error(_) => Success(None)
    }
  }

  def >>[B](f: A => RowParser[B]): RowParser[B] = flatMap(f)

  def * : ResultSetParser[List[A]] = ResultSetParser.list(parent)

  def + : ResultSetParser[List[A]] = ResultSetParser.nonEmptyList(parent)

  def single = ResultSetParser.single(parent)

  def singleOpt = ResultSetParser.singleOpt(parent)

}

trait ResultSetParser[+A] extends (ResultSet => SqlResult[A]) {
  parent =>

  def map[B](f: A => B): ResultSetParser[B] = ResultSetParser(rs => parent(rs).map(f))

}

object ResultSetParser {

  def apply[A](f: ResultSet => SqlResult[A]): ResultSetParser[A] = new ResultSetParser[A] { rows =>

    def apply(rows: ResultSet): SqlResult[A] = f(rows)

  }

  def list[A](p: RowParser[A]): ResultSetParser[List[A]] = {
    // Performance note: sequence produces a List in reverse order, since appending to a
    // List is an O(n) operation, and this is done n times, yielding O(n2) just to convert the
    // result set to a List.  Prepending is O(1), so we use prepend, and then reverse the result
    // in the map function below.
    @scala.annotation.tailrec
    def sequence(results: SqlResult[List[A]], rows: Stream[Row]): SqlResult[List[A]] = {

      (results, rows) match {

        case (Success(rs), row #:: tail) => sequence(p(row).map(_ +: rs), tail)

        case (r, _) => r

      }

    }

    ResultSetParser { rows => sequence(Success(List()), rows).map(_.reverse) }
  }

  def nonEmptyList[A](p: RowParser[A]): ResultSetParser[List[A]] = ResultSetParser(rows => if (rows.isEmpty) Error(SqlMappingError("Empty Result Set")) else list(p)(rows))

  def single[A](p: RowParser[A]): ResultSetParser[A] = ResultSetParser {
    case head #:: Stream.Empty => p(head)
    case Stream.Empty => Error(SqlMappingError("No rows when expecting a single one"))
    case _ => Error(SqlMappingError("too many rows when expecting a single one"))

  }

  def singleOpt[A](p: RowParser[A]): ResultSetParser[Option[A]] = ResultSetParser {
    case head #:: Stream.Empty => p.map(Some(_))(head)
    case Stream.Empty => Success(None)
    case _ => Error(SqlMappingError("too many rows when expecting a single one"))
  }

}
