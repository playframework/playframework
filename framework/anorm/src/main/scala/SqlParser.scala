
import java.util.Date

package anorm {

  import utils.Scala.MayErr
  import utils.Scala.MayErr._

  object SqlParser extends SqlParser {
    def flatten[T1, T2, R](implicit f: TupleFlattener[(T1 ~ T2) => R]): ((T1 ~ T2) => R) = f.f
  }

  trait SqlParser extends scala.util.parsing.combinator1.Parsers {

    case class StreamReader(s: Stream[Row], override val lastNoSuccess: NoSuccess = null) extends ReaderWithLastNoSuccess {
      override type R = Input
      def first = s.headOption.toRight(EndOfStream())
      def rest = this.copy(s = (s.drop(1)))
      def pos = scala.util.parsing.input.NoPosition
      def atEnd = s.isEmpty
      override def withLastNoSuccess(noSuccess: NoSuccess) = this.copy(lastNoSuccess = noSuccess)

    }

    case class EndOfStream()

    type Elem = Either[EndOfStream, Row]

    type Input = StreamReader

    import scala.collection.generic.CanBuildFrom
    import scala.collection.mutable.Builder

    implicit def extendParser[A](a: Parser[A]): ExtendedParser[A] = ExtendedParser(a)

    case class ExtendedParser[A](p: Parser[A]) {
      // a combinator that keeps first parser from consuming input
      def ~<[B](b: Parser[B]): Parser[A ~ B] = guard(p) ~ b
      def ~/[B](b: Parser[B]): Parser[A ~ B] = guard(p) ~ b
      def ?! : Parser[Option[A]] = (p ^^ { case o => Some(o) } | newLine ^^^ None)
    }

    def sequence[A](ps: Traversable[Parser[A]])(implicit bf: CanBuildFrom[Traversable[_], A, Traversable[A]]) = {
      Parser[Traversable[A]] { in =>
        ps.foldLeft(success(bf(ps)))((s, p) =>
          for (ss <- s; pp <- p) yield ss += pp) map (_.result) apply in
      }
    }
    implicit def rowFunctionToParser[T](f: (Row => MayErr[SqlRequestError, T])): Parser[T] = {
      eatRow(Parser[T] { in =>
        in.first.left.map(_ => PFailure("End of Stream", in))
          .flatMap(f(_).left.map({ case e => PFailure(e.toString, in) }))
          .fold(e => e, a => { Success(a, in) })
      })
    }

    implicit def rowParserToFunction[T](p: RowParser[T]): (Row => MayErr[SqlRequestError, T]) = p.f

    case class RowParser[A](f: (Row => MayErr[SqlRequestError, A])) extends Parser[A] {
      lazy val parser = rowFunctionToParser(f)
      def apply(in: Input) = parser(in)
      def ~<[B](b: RowParser[B]): RowParser[A ~ B] = RowParser[A ~ B](r =>
        f(r).flatMap(a =>
          b.f(r).map(c =>
            new ~(a, c))))
      def ~<[B](b: Parser[B]): Parser[A ~ B] = extendParser(this) ~< b
      def ~/[B](b: Parser[B]): Parser[A ~ B] = extendParser(this) ~< b
      def ?! : Parser[Option[A]] = extendParser(this) ?!
    }

    def maybe[A](p: Parser[A]): Parser[Option[A]] = extendParser(p) ?!

    def str(columnName: String): RowParser[String] = get[String](columnName)(implicitly[ColumnTo[String]])

    def bool(columnName: String): RowParser[Boolean] = get[Boolean](columnName)(implicitly[ColumnTo[Boolean]])

    def int(columnName: String): RowParser[Int] = get[Int](columnName)(implicitly[ColumnTo[Int]])

    def long(columnName: String): RowParser[Long] = get[Long](columnName)(implicitly[ColumnTo[Long]])

    def date(columnName: String): RowParser[Date] = get[Date](columnName)(implicitly[ColumnTo[Date]])

    def get[T](columnName: String)(implicit extractor: ColumnTo[T]): RowParser[T] = RowParser(extractor.transform(_, columnName))

    def contains[TT: ColumnTo, T <: TT](columnName: String, t: T): Parser[Unit] = guard(get[TT](columnName)(implicitly[ColumnTo[TT]]) ^? { case a if a == t => Unit })

    def current[T](columnName: String)(implicit extractor: ColumnTo[T]): RowParser[T] = RowParser(extractor.transform(_, columnName))

    def eatRow[T](p: Parser[T]) = p <~ newLine
    /*
        def noError[T](p:Parser[T]) = Parser( in => {
          val before = lastNoSuccess
          val result = p(in)
          val after = lastNoSuccess
          if((after != null) && !(before eq after))
            after match {
              case Error(_,_) => after
              case Failure(_,_)  => result
            }
          else result

        })*/

    def current1[T](columnName: String)(implicit extractor: ColumnTo[T]): Parser[T] = commit(current[T](columnName)(extractor))

    def newLine: Parser[Unit] = Parser[Unit] { in =>
      if (in.atEnd) PFailure("end", in) else Success(Unit, in.rest)
    }

    def scalar[T](implicit m: Manifest[T]) = {
      SqlParser.RowParser(row =>
        row.asList
          .headOption.toRight(NoColumnsInReturnedResult)
          .flatMap(a =>
            if (m >:> TypeWrangler.javaType(a.asInstanceOf[AnyRef].getClass))
              Right(a.asInstanceOf[T])
            else
              Left(TypeDoesNotMatch(m.erasure + " and " + a.asInstanceOf[AnyRef].getClass))))
    }

    def spanM[A](by: (Row => MayErr[SqlRequestError, Any]), a: Parser[A]): Parser[List[A]] = {
      val d = guard(by)
      d >> (first => Parser[List[A]] { in =>
        //instead of cast it'd be much better to override type Reader
        {
          val (groupy, rest) = in.s.span(by(_).right.toOption.exists(r => r == first))
          val g = (a *)(StreamReader(groupy))
          g match {
            case Success(a, _) => Success(a, StreamReader(rest))
            case Failure(msg, _) => PFailure(msg, in)
            case Error(msg, _) => PError(msg, in)
          }
        }
      })
    }

    implicit def symbolToColumn(columnName: Symbol): ColumnSymbol = ColumnSymbol(columnName)

    case class ColumnSymbol(name: Symbol) {

      def of[T](implicit extractor: ColumnTo[T]): Parser[T] = get[T](name.name)(extractor)
      def is[TT: ColumnTo, T <: TT](t: T): Parser[Unit] = contains[TT, T](name.name, t)(implicitly[ColumnTo[TT]])

    }

  }

}
