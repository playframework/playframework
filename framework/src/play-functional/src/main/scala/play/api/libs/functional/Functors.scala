/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.functional

import scala.language.higherKinds

sealed trait Variant[M[_]]

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

// Work around the fact that Scala does not support higher-kinded type patterns (type variables can only be simple identifiers)
// We use case classes wrappers so we can pattern match using their extractor
sealed trait VariantExtractor[M[_]]

case class FunctorExtractor[M[_]](functor: Functor[M]) extends VariantExtractor[M]

case class InvariantFunctorExtractor[M[_]](InvariantFunctor: InvariantFunctor[M]) extends VariantExtractor[M]

case class ContravariantFunctorExtractor[M[_]](ContraVariantFunctor: ContravariantFunctor[M]) extends VariantExtractor[M]

object VariantExtractor {

  implicit def functor[M[_]: Functor]: FunctorExtractor[M] =
    FunctorExtractor(implicitly[Functor[M]])

  implicit def contravariantFunctor[M[_]: ContravariantFunctor]: ContravariantFunctorExtractor[M] =
    ContravariantFunctorExtractor(implicitly[ContravariantFunctor[M]])

  implicit def invariantFunctor[M[_]: InvariantFunctor]: InvariantFunctorExtractor[M] =
    InvariantFunctorExtractor(implicitly[InvariantFunctor[M]])

}
