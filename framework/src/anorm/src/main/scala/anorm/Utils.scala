package anorm

case class MayErr[+E, +A](e: Either[E, A]) {

  def flatMap[B, EE >: E](f: A => MayErr[EE, B]): MayErr[EE, B] = {
    MayErr(e.right.flatMap(a => f(a).e))
  }

  def map[B](f: A => B): MayErr[E, B] = {
    MayErr(e.right.map(f))
  }

  def filter[EE >: E](p: A => Boolean, error: EE): MayErr[EE, A] = MayErr(e.right.filter(p).getOrElse(Left(error)))

  def toOptionLoggingError(): Option[A] = {
    e.left.map(m => { println(m.toString); m }).right.toOption
  }

  def get = e.fold(e => throw new RuntimeException(e.toString), a => a)
}

object MayErr {
  import scala.language.implicitConversions
  implicit def eitherToError[E, EE >: E, A, AA >: A](e: Either[E, A]): MayErr[EE, AA] = MayErr[E, A](e)
  implicit def errorToEither[E, EE >: E, A, AA >: A](e: MayErr[E, A]): Either[EE, AA] = e.e
}
