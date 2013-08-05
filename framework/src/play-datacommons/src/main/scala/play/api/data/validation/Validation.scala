package play.api.data.validation

sealed trait Validation[+E, +A] { self =>
  def map[X](f: A => X): Validation[E, X] = this match {
    case Success(v) => Success(f(v))
    case Failure(e) => Failure(e)
  }

  def flatMap[EE >: E, AA](f: A => Validation[EE, AA]): Validation[EE, AA] = this match {
    case Success(v) => f(v)
    case Failure(e) => Failure(e)
  }

  def fold[X](invalid: Seq[E] => X, valid: A => X): X = this match {
    case Success(v) => valid(v)
    case Failure(e) => invalid(e)
  }

  def filterNot[EE >: E](error: EE)(p: A => Boolean): Validation[EE, A] =
    this.flatMap { a => if (p(a)) Failure[EE, A](Seq(error)) else Success[EE, A](a) }

  def filterNot(p: A => Boolean): Validation[E, A] =
    this.flatMap { a => if (p(a)) Failure[E, A](Nil) else Success[E, A](a) }

  def filter(p: A => Boolean): Validation[E, A] =
    this.flatMap { a => if (p(a)) Success[E, A](a) else Failure[E, A](Nil) }

  def filter[EE >: E](otherwise: EE)(p: A => Boolean): Validation[EE, A] =
    this.flatMap { a => if (p(a)) Success[EE, A](a) else Failure[EE, A](Seq(otherwise)) }

  def collect[EE >: E, B](otherwise: EE)(p: PartialFunction[A, B]): Validation[EE, B] = flatMap {
    case t if p.isDefinedAt(t) => Success(p(t))
    case _ => Failure(Seq(otherwise))
  }

  def foreach(f: A => Unit): Unit = this match {
    case Success(a) => f(a)
    case _ => ()
  }

  def withFilter(p: A => Boolean) = new WithFilter(p)

  final class WithFilter(p: A => Boolean) {
    def map[B](f: A => B): Validation[E, B] = self match {
      case Success(a) =>
        if (p(a)) Success(f(a))
        else Failure(Nil)
      case Failure(errs) => Failure(errs)
    }
    def flatMap[EE >: E, B](f: A => Validation[EE, B]): Validation[EE, B] = self match {
      case Success(a) =>
        if (p(a)) f(a)
        else Failure(Nil)
      case Failure(errs) => Failure(errs)
    }
    def foreach(f: A => Unit): Unit = self match {
      case Success(a) if p(a) => f(a)
      case _ => ()
    }
    def withFilter(q: A => Boolean) = new WithFilter(a => p(a) && q(a))
  }

  def get: A

  def getOrElse[AA >: A](t: => AA): AA = this match {
    case Success(a) => a
    case Failure(_) => t
  }

  def orElse[EE >: E, AA >: A](t: => Validation[EE, AA]): Validation[EE, AA] = this match {
    case s @ Success(_) => s
    case Failure(_) => t
  }

  def asOpt = this match {
    case Success(v) => Some(v)
    case Failure(_) => None
  }

  def asEither = this match {
    case Success(v) => Right(v)
    case Failure(e) => Left(e)
  }

  def recover[EE >: E, AA >: A](errManager: PartialFunction[Failure[EE, AA], AA]): Validation[EE, AA] = this match {
    case Success(v) => Success(v)
    case e@Failure(_) => if (errManager isDefinedAt e) Success(errManager(e)) else this
  }

  def recoverTotal[AA >: A](errManager: Failure[E, A] => AA): AA = this match {
    case Success(v) => v
    case e@Failure(_) => errManager(e)
  }

  // TODO: rename (keepAnd ?)
  def *>[EE >: E, B](o: Validation[EE, B]): Validation[EE, B] = (this, o) match {
    case (Success(_), Success(v)) => Success(v)
    case (Success(_), Failure(e)) => Failure(e)
    case (Failure(e), Success(_)) => Failure(e)
    case (Failure(e1), Failure(e2)) => Failure((e1: Seq[E]) ++ (e2: Seq[EE])) // dafuk??? hy do I need to force types ?
  }

  def |[EE >: E, AA >: A](o: => Validation[EE, AA]): Validation[EE, AA] =  (this, o) match {
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

final case class FailProjection[+E, +A](v: Validation[E, A]) {
  def map[F](f: Seq[E] => Seq[F]): Validation[F, A] = v match {
    case Success(v) => Success(v)
    case Failure(e) => Failure(f(e))
  }
}
final case class SuccessProjection[+E, +A](v: Validation[E, A]) {
  def map[B](f: A => B): Validation[E, B] = v match {
    case Success(v) => Success(f(v))
    case Failure(e) => Failure(e)
  }
}

// Those classes should be final, but we neeed to inherit
// from them for backward compatibility of JsSuccess and JsError
class Success[+E, +A](val value: A) extends Validation[E, A] {
  def get: A = value
  override def toString = s"Success($value)"

  override def equals(o: Any) = {
    if(canEqual(o)) {
      val j = o.asInstanceOf[Success[E, A]]
      this.value == j.value
    }
    else
      false
  }

  def canEqual(o: Any) = o.isInstanceOf[Success[E, A]]
}
class Failure[+E, +A](val errors: Seq[E]) extends Validation[E, A] {
  def get: Nothing = throw new NoSuchElementException("Failure.get")
  override def toString = s"Failure($errors)"

  override def equals(o: Any) = {
    if(canEqual(o)) {
      val j = o.asInstanceOf[Failure[E, A]]
      this.errors == j.errors
    }
    else
      false
  }
  def canEqual(o: Any) = o.isInstanceOf[Failure[E, A]]
}

object Success {
  def apply[E, A](v: A): Success[E, A] = new Success(v)
  def unapply[E, A](s: Success[E, A]): Option[A] = Some(s.value)
}

object Failure {
  import play.api.libs.functional.Monoid

  def apply[E, A](errors: Seq[E]): Failure[E, A] = new Failure(errors)
  def unapply[E, A](f: Failure[E, A]): Option[Seq[E]] = Some(f.errors)

  def merge[E, A](e1: Failure[E, A], e2: Failure[E, A]): Failure[E, A] = {
    Failure(e1.errors ++ e2.errors)
  }
}
