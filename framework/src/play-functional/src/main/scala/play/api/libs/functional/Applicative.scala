package play.api.libs.functional

import scala.language.higherKinds

trait Applicative[M[_]] {

  def pure[A](a: A): M[A]
  def map[A, B](m: M[A], f: A => B): M[B]
  def apply[A, B](mf: M[A => B], ma: M[A]): M[B]

}

class ApplicativeOps[M[_], A](ma: M[A])(implicit a: Applicative[M]) {

  def ~>[B](mb: M[B]): M[B] = a(a(a.pure((_: A) => (b: B) => b), ma), mb)
  def andKeep[B](mb: M[B]): M[B] = ~>(mb)

  def <~[B](mb: M[B]): M[A] = a(a(a.pure((a: A) => (_: B) => a), ma), mb)
  def keepAnd[B](mb: M[B]): M[A] = <~(mb)

  def <~>[B, C](mb: M[B])(implicit witness: <:<[A, B => C]): M[C] = apply(mb)
  def apply[B, C](mb: M[B])(implicit witness: <:<[A, B => C]): M[C] = a(a.map(ma, witness), mb)
}