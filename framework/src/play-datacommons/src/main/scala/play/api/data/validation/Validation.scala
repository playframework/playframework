package play.api.data.validation

object Validations {

  import play.api.libs.functional._
  import scala.language.reflectiveCalls

  implicit def monoidConstraint[T] = new Monoid[Constraint[T]] {
    def append(c1: Constraint[T], c2: Constraint[T]) = v => c1(v) *> (c2(v))
    def identity = Constraints.noConstraint[T]
  }

  implicit def applicativeRule[I] = new Applicative[({type f[O] = Rule[I, O]})#f] {
    override def pure[A](a: A): Rule[I, A] =
      Rule(Path[I](), (_: Path[I]) => (_: I) => Success(a))

    override def map[A, B](m: Rule[I, A], f: A => B): Rule[I, B] =
      Rule(m.p, { p => d =>
        m.m(p)(d)
         .fold(
           errs => Failure(errs),
           a => m.v(a).fail.map{ errs => Seq(p -> errs) })
         .map(f)
      })

    override def apply[A, B](mf: Rule[I, A => B], ma: Rule[I, A]): Rule[I, B] =
      Rule(Path[I](), { p => d =>
        val a = ma.validate(d)
        val f = mf.validate(d)
        val res = (f *> a).flatMap(x => f.map(_(x)))
        res
      })
  }

  implicit def functorRule[I] = new Functor[({type f[O] = Rule[I, O]})#f] {
    def fmap[A, B](m: Rule[I, A], f: A => B): Rule[I, B] = applicativeRule[I].map(m, f)
  }

  // Helps the compiler a bit
  import play.api.libs.functional.syntax._
  implicit def cba[I] = functionalCanBuildApplicative[({type f[O] = Rule[I, O]})#f]
  implicit def fbo[I, O] = toFunctionalBuilderOps[({type f[O] = Rule[I, O]})#f, O] _

  // TODO
  /*
  import play.api.libs.json._
  implicit def pathWrite[I, O](implicit w: Writes[O]) = Writes[(Path[I], O)]{
    case (p, o) => Json.obj("path" -> p.toString, "errors" -> w.writes(o))
  }
  */
}

sealed trait Validation[E, +A] {
  def map[X](f: A => X): Validation[E, X] = this match {
    case Success(v) => Success(f(v))
    case Failure(e) => Failure(e)
  }

  def flatMap[X](f: A => Validation[E, X]): Validation[E, X] = this match {
    case Success(v) => f(v)
    case Failure(e) => Failure(e)
  }

  def fold[X](invalid: Seq[E] => X, valid: A => X): X = this match {
    case Success(v) => valid(v)
    case Failure(e) => invalid(e)
  }

  // TODO: rename
  def *>[B](o: Validation[E, B]): Validation[E, B] = (this, o) match {
    case (Success(_), Success(v)) => Success(v)
    case (Success(_), Failure(e)) => Failure(e)
    case (Failure(e), Success(_)) => Failure(e)
    case (Failure(e1), Failure(e2)) => Failure(e1 ++ e2)
  }

  def |[B >: A](o: => Validation[E, B]): Validation[E, B] =  (this, o) match {
    case (Success(v), _) => Success(v)
    case (Failure(e), Success(v)) => Success(v)
    case (Failure(e), _) => Failure(e)
  }

  def fail = FailProjection(this)
  def success = SuccessProjection(this)
}

object Validation {
  // this method should be defined for any instance of Monad
  def sequence[E, A](vs: Seq[Validation[E, A]]): Validation[E, Seq[A]] = {
    val reversed = vs.foldLeft[Validation[E, Seq[A]]](Success(Nil)){ (acc, va) =>
      va.flatMap({ a: A =>
        acc.flatMap({ as: Seq[A] =>
          Success(a +: as)
        })
      })
    }
    reversed.flatMap({ as: Seq[A] => Success(as.reverse) })
  }
}

case class FailProjection[E, +A](v: Validation[E, A]) {
  def map[F](f: Seq[E] => Seq[F]): Validation[F, A] = v match {
    case Success(v) => Success(v)
    case Failure(e) => Failure(f(e))
  }
}
case class SuccessProjection[E, +A](v: Validation[E, A]) {
  def map[B](f: A => B): Validation[E, B] = v match {
    case Success(v) => Success(f(v))
    case Failure(e) => Failure(e)
  }
}

final case class Success[E, A](a: A) extends Validation[E, A]
final case class Failure[E, A](errors: Seq[E]) extends Validation[E, A]

object Failure {
  import play.api.libs.functional.Monoid

  def merge[E, A](e1: Failure[E, A], e2: Failure[E, A]): Failure[E, A] = {
    Failure(e1.errors ++ e2.errors)
  }
}
