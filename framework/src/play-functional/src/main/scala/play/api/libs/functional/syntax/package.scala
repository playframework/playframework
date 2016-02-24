/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
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

  implicit def toMonoidOps[A](a: A)(implicit m: Monoid[A]): MonoidOps[A] = new MonoidOps(a)

  implicit def toFunctorOps[M[_], A](ma: M[A])(implicit fu: Functor[M]): FunctorOps[M, A] = new FunctorOps(ma)
  implicit def toContraFunctorOps[M[_], A](ma: M[A])(implicit fu: ContravariantFunctor[M]): ContravariantFunctorOps[M, A] = new ContravariantFunctorOps(ma)
  implicit def toInvariantFunctorOps[M[_], A](ma: M[A])(implicit fu: InvariantFunctor[M]): InvariantFunctorOps[M, A] = new InvariantFunctorOps(ma)

  def unapply[B, A](f: B => Option[A]): B => A = { b: B => f(b).get }

  def unlift[A, B](f: A => Option[B]): A => B = Function.unlift(f)

}
