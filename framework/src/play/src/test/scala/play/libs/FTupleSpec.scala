/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{ Arbitrary, Gen }
import org.specs2.mutable.Specification
import org.specs2.ScalaCheck

object FTupleSpec extends Specification with ScalaCheck {

  import ArbitraryTuples._

  type A = String
  type B = Integer
  type C = String
  type D = Integer
  type E = String

  implicit val stringParam: Arbitrary[String] = Arbitrary(Gen.oneOf("x", null))
  implicit val integerParam: Arbitrary[Integer] = Arbitrary(Gen.oneOf(42, null))

  checkEquality[F.Tuple[A, B]]("Tuple")
  checkEquality[F.Tuple3[A, B, C]]("Tuple3")
  checkEquality[F.Tuple4[A, B, C, D]]("Tuple4")
  checkEquality[F.Tuple5[A, B, C, D, E]]("Tuple5")

  def checkEquality[A: Arbitrary](name: String): Unit = {
    s"$name equality" should {

      "be commutative" in prop { (a1: A, a2: A) =>
        (a1 equals a2) == (a2 equals a1)
      }

      "be reflexive" in prop { (a: A) =>
        a equals a
      }

      "check for null" in prop { (a: A) =>
        !(a equals null)
      }

      "check object type" in prop { (a: A, s: String) =>
        !(a equals s)
      }

      "obey hashCode contract" in prop { (a1: A, a2: A) =>
        // (a1 equals a2) ==> (a1.hashCode == a2.hashCode)
        if (a1 equals a2) (a1.hashCode == a2.hashCode) else true
      }
    }
  }

  object ArbitraryTuples {
    implicit def arbTuple[A: Arbitrary, B: Arbitrary]: Arbitrary[F.Tuple[A, B]] = Arbitrary {
      for (a <- arbitrary[A]; b <- arbitrary[B]) yield F.Tuple(a, b)
    }

    implicit def arbTuple3[A: Arbitrary, B: Arbitrary, C: Arbitrary]: Arbitrary[F.Tuple3[A, B, C]] = Arbitrary {
      for (a <- arbitrary[A]; b <- arbitrary[B]; c <- arbitrary[C]) yield F.Tuple3(a, b, c)
    }

    implicit def arbTuple4[A: Arbitrary, B: Arbitrary, C: Arbitrary, D: Arbitrary]: Arbitrary[F.Tuple4[A, B, C, D]] = Arbitrary {
      for (a <- arbitrary[A]; b <- arbitrary[B]; c <- arbitrary[C]; d <- arbitrary[D]) yield F.Tuple4(a, b, c, d)
    }

    implicit def arbTuple5[A: Arbitrary, B: Arbitrary, C: Arbitrary, D: Arbitrary, E: Arbitrary]: Arbitrary[F.Tuple5[A, B, C, D, E]] = Arbitrary {
      for (a <- arbitrary[A]; b <- arbitrary[B]; c <- arbitrary[C]; d <- arbitrary[D]; e <- arbitrary[E]) yield F.Tuple5(a, b, c, d, e)
    }
  }
}
