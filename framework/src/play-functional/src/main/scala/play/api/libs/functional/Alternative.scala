/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.functional

import scala.language.higherKinds

trait Alternative[M[_]] {

  def app: Applicative[M]
  def |[A, B >: A](alt1: M[A], alt2: M[B]): M[B]
  def empty: M[Nothing]
  //def some[A](m: M[A]): M[List[A]]
  //def many[A](m: M[A]): M[List[A]]

}

class AlternativeOps[M[_], A](alt1: M[A])(implicit a: Alternative[M]) {

  def |[B >: A](alt2: M[B]): M[B] = a.|(alt1, alt2)
  def or[B >: A](alt2: M[B]): M[B] = |(alt2)

}

