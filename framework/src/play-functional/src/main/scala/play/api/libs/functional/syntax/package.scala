/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.functional.syntax

import scala.language.higherKinds
import scala.language.implicitConversions

import play.api.libs.functional._

/**
 * Don't forget to {{{import play.api.libs.functional.syntax._}}} to enable functional combinators
 * when using Json API.
 */
object `package` {

  implicit def toAlternativeOps[M[_], A](a: M[A])(implicit app: Alternative[M]): AlternativeOps[M, A] = new AlternativeOps(a)

  implicit def toApplicativeOps[M[_], A](a: M[A])(implicit app: Applicative[M]): ApplicativeOps[M, A] = new ApplicativeOps(a)

  implicit def toFunctionalBuilderOps[M[_], A](a: M[A])(implicit fcb: FunctionalCanBuild[M]) = new FunctionalBuilderOps[M, A](a)(fcb)

  implicit def functionalCanBuildApplicative[M[_]](implicit app: Applicative[M]): FunctionalCanBuild[M] = new FunctionalCanBuild[M] {

    def apply[A, B](a: M[A], b: M[B]): M[A ~ B] = app.apply(app.map[A, B => A ~ B](a, a => ((b: B) => new ~(a, b))), b)

  }

  implicit def functorOption: Functor[Option] = new Functor[Option] {

    def fmap[A, B](a: Option[A], f: A => B): Option[B] = a.map(f)

  }

  implicit def applicativeOption: Applicative[Option] = new Applicative[Option] {

    def pure[A](a: A): Option[A] = Some(a)

    def map[A, B](m: Option[A], f: A => B): Option[B] = m.map(f)

    def apply[A, B](mf: Option[A => B], ma: Option[A]): Option[B] = mf.flatMap(f => ma.map(f))

  }

  implicit def functionMonoid[A] = new Monoid[A => A] {
    override def append(f1: A => A, f2: A => A) = f2 compose f1
    override def identity = Predef.identity
  }

  implicit def toMonoidOps[A](a: A)(implicit m: Monoid[A]): MonoidOps[A] = new MonoidOps(a)

  implicit def toFunctorOps[M[_], A](ma: M[A])(implicit fu: Functor[M]): FunctorOps[M, A] = new FunctorOps(ma)
  implicit def toContraFunctorOps[M[_], A](ma: M[A])(implicit fu: ContravariantFunctor[M]): ContravariantFunctorOps[M, A] = new ContravariantFunctorOps(ma)
  implicit def toInvariantFunctorOps[M[_], A](ma: M[A])(implicit fu: InvariantFunctor[M]): InvariantFunctorOps[M, A] = new InvariantFunctorOps(ma)

  def unapply[B, A](f: B => Option[A]) = { b: B => f(b).get }

  def unlift[A, B](f: A => Option[B]): A => B = Function.unlift(f)

}
