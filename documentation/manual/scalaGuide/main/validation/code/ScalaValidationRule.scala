/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.validation

import org.specs2.mutable.Specification

object ScalaValidationRuleSpec extends Specification {

  //#rule-first-ex
  import play.api.data.mapping._
  def isFloat: Rule[String, Float] = ???
  //#rule-first-ex

  "Scala Validation Rule" should {

    "test default Rules" in {
      import play.api.data.mapping.Rules

      //#rule-defaults
      Rules.float.validate("1") === Success(1.0f)
      Rules.float.validate("-13.7") === Success(-13.7f)

      Rules.float.validate("abc") === Failure(Seq((Path,Seq(ValidationError("error.number", "Float")))))
      //#rule-defaults
    }

    "custom rule definition" in {
      //#rule-headInt
      import play.api.data.mapping._

      val headInt: Rule[List[Int], Int] = Rule.fromMapping {
        case Nil => Failure(Seq(ValidationError("error.emptyList")))
        case head :: _ => Success(head)
      }
      //#rule-headInt

      //#rule-headInt-test
      headInt.validate(List(1, 2, 3, 4, 5)) === Success(1)
      headInt.validate(Nil) === Failure(Seq((Path, Seq(ValidationError("error.emptyList")))))
      //#rule-headInt-test
    }

    "custom generic rule definition" in {
      //#rule-head
      import play.api.data.mapping._

      def head[T]: Rule[List[T], T] = Rule.fromMapping {
        case Nil => Failure(Seq(ValidationError("error.emptyList")))
        case head :: _ => Success(head)
      }
      //#rule-head

      //#rule-head-test
      head.validate(List('a', 'b', 'c', 'd')) === Success('a')
      head.validate(List[Char]()) === Failure(Seq((Path, Seq(ValidationError("error.emptyList")))))
      //#rule-head-test
    }

    "compose Rules" in {
      import play.api.data.mapping._
      import play.api.data.mapping.Rules._

      def head[T]: Rule[List[T], T] = Rule.fromMapping {
        case Nil => Failure(Seq(ValidationError("error.emptyList")))
        case head :: _ => Success(head)
      }

      //#rule-firstFloat
      val firstFloat: Rule[List[String], Float] = head.compose(float)
      //#rule-firstFloat

      //#rule-firstFloat-test1
      firstFloat.validate(List("1", "2")) === Success(1.0f)
      firstFloat.validate(List("1.2", "foo")) === Success(1.2f)
      //#rule-firstFloat-test1

      //#rule-firstFloat-test2
      firstFloat.validate(List()) === Failure(Seq((Path,Seq(ValidationError("error.emptyList")))))
      //#rule-firstFloat-test2

      //#rule-firstFloat-test3
      firstFloat.validate(List("foo", "2")) === Failure(Seq((Path,Seq(ValidationError("error.number", "Float")))))
      //#rule-firstFloat-test3
    }

    "repath" in {
      def head[T]: Rule[List[T], T] = Rule.fromMapping {
        case Nil => Failure(Seq(ValidationError("error.emptyList")))
        case head :: _ => Success(head)
      }

      //#rule-firstFloat-repath
      val firstFloat2: Rule[List[String],Float] = head.compose(Path \ 0)(Rules.float)
      firstFloat2.validate(List("foo", "2")) === Failure(Seq((Path \ 0, Seq(ValidationError("error.number", "Float")))))
      //                                       NOTICE THE INDEX HERE ^
      //#rule-firstFloat-repath
    }

    "support // composition" in {
      import play.api.data.mapping._

      //#par-composition-def
      val positive: Rule[Int,Int] = Rules.min(0)
      val even: Rule[Int,Int] = Rules.validateWith[Int]("error.even"){ _ % 2 == 0 }
      //#par-composition-def

      //#par-composition-comp
      val positiveAndEven: Rule[Int,Int] = positive |+| even
      //#par-composition-comp

      //#par-composition-test
      positiveAndEven.validate(12) === Success(12)
      positiveAndEven.validate(-12) === Failure(Seq((Path, List(ValidationError("error.min", 0)))))
      positiveAndEven.validate(13) === Failure(Seq((Path, List(ValidationError("error.even")))))
      positiveAndEven.validate(-13) ===  Failure(Seq((Path,Seq(ValidationError("error.min",0), ValidationError("error.even")))))
      //#par-composition-test
    }

  }
}
