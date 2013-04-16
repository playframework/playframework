package play.api.libs.functional

import scala.language.higherKinds

trait Variant[M[_]]

trait Functor[M[_]] extends Variant[M] {

  def fmap[A, B](m: M[A], f: A => B): M[B]

}

trait InvariantFunctor[M[_]] extends Variant[M] {

  def inmap[A, B](m: M[A], f1: A => B, f2: B => A): M[B]

}

trait ContravariantFunctor[M[_]] extends Variant[M] {

  def contramap[A, B](m: M[A], f1: B => A): M[B]

}

class FunctorOps[M[_], A](ma: M[A])(implicit fu: Functor[M]) {

  def fmap[B](f: A => B): M[B] = fu.fmap(ma, f)

}

class ContravariantFunctorOps[M[_], A](ma: M[A])(implicit fu: ContravariantFunctor[M]) {

  def contramap[B](f: B => A): M[B] = fu.contramap(ma, f)

}

class InvariantFunctorOps[M[_], A](ma: M[A])(implicit fu: InvariantFunctor[M]) {

  def inmap[B](f: A => B, g: B => A): M[B] = fu.inmap(ma, f, g)

}
