/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http.parsing

import scala.concurrent.Future

import play.api.libs.iteratee.{ Done, Enumerator, ExecutionSpecification, Input }
import play.api.mvc.{ BodyParser, BodyParsers, Results, Result }
import play.api.test.{ FakeRequest, PlaySpecification }

import org.specs2.ScalaCheck
import org.scalacheck.{ Arbitrary, Gen }

object BodyParserSpec extends PlaySpecification with ExecutionSpecification with ScalaCheck {

  def run[A](bodyParser: BodyParser[A]) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    await {
      Future {
        bodyParser(FakeRequest())
      }.flatMap {
        Enumerator.empty |>>> _
      }
    }
  }

  def constant[A](a: A): BodyParser[A] =
    BodyParser("constant") { request =>
      Done(Right(a), Input.Empty)
    }

  def simpleResult(s: Result): BodyParser[Any] =
    BodyParser("simple result") { request =>
      Done(Left(s), Input.Empty)
    }

  implicit val arbResult: Arbitrary[Result] =
    Arbitrary {
      Gen.oneOf(
        Results.Ok,
        Results.BadRequest, Results.NotFound,
        Results.InternalServerError
      )
    }

  /* map and mapM should satisfy the functor laws, namely,
   * preservation of identity and function composition.
   * flatMap and flatMapM should satisfy the monad laws, namely,
   * left and right identity, and associativity.
   * The *M versions are simply lifted to Future.
   * When the given function is right-biased, validate and
   * validateM are equivalent to map and mapM, respectively, so
   * should satisfy the functor laws. When left-biased, the result
   * will override the result of the body parser being validated.
   * All of the functions in question should pass a simple result
   * through.
   */

  "BodyParser.map" should {

    "satisfy functor law 1" in prop { (x: Int) =>
      mustExecute(1) { implicit ec => // one execution from `map`
        run {
          constant(x).map(identity)
        } must beRight(x)
      }
    }

    "satisfy functor law 2" in prop { (x: Int) =>
      val inc = (i: Int) => i + 1
      val dbl = (i: Int) => i * 2
      mustExecute(3) { implicit ec => // three executions from `map`
        run {
          constant(x).map(inc)
            .map(dbl)
        } must_== run {
          constant(x).map(inc andThen dbl)
        }
      }
    }

    "pass through simple result" in prop { (s: Result) =>
      mustExecute(1) { implicit ec => // one execution from `map`
        run {
          simpleResult(s).map(identity)
        } must beLeft(s)
      }
    }
  }

  "BodyParser.mapM" should {

    "satisfy lifted functor law 1" in prop { (x: Int) =>
      mustExecute(1) { implicit ec => // one execution from `mapM`
        run {
          constant(x).mapM(Future.successful)
        } must beRight(x)
      }
    }

    "satisfy lifted functor law 2" in prop { (x: Int) =>
      val inc = (i: Int) => Future.successful(i + 1)
      val dbl = (i: Int) => Future.successful(i * 2)
      mustExecute(3, 1) { (mapMEC, flatMapEC) =>
        val flatMapPEC = flatMapEC.prepare()
        /* three executions from `BodyParser.mapM`
         * and one from `Future.flatMapM`
         */
        run {
          constant(x).mapM(inc)(mapMEC)
            .mapM(dbl)(mapMEC)
        } must_== run {
          constant(x).mapM { y =>
            inc(y).flatMap(dbl)(flatMapPEC)
          }(mapMEC)
        }
      }
    }

    "pass through simple result" in prop { (s: Result) =>
      mustExecute(1) { implicit ec => // one execution from `mapM`
        run {
          simpleResult(s).mapM(Future.successful)
        } must beLeft(s)
      }
    }
  }

  "BodyParser.flatMap" should {

    "satisfy monad law 1" in prop { (x: Int) =>
      val inc = (i: Int) => constant(i + 1)
      mustExecute(1) { implicit ec => // one execution from `flatMap`
        run {
          constant(x).flatMap(inc)
        } must_== run {
          inc(x)
        }
      }
    }

    "satisfy monad law 2" in prop { (x: Int) =>
      mustExecute(1) { implicit ec => // one execution from `flatMap`
        run {
          constant(x).flatMap(constant)
        } must_== run {
          constant(x)
        }
      }
    }

    "satisfy monad law 3" in prop { (x: Int) =>
      val inc = (i: Int) => constant(i + 1)
      val dbl = (i: Int) => constant(i * 2)
      mustExecute(3, 1) { (outerEC, innerEC) => // four executions from `flatMap`
        val innerPEC = innerEC.prepare()
        run {
          constant(x).flatMap(inc)(outerEC)
            .flatMap(dbl)(outerEC)
        } must_== run {
          constant(x).flatMap { y =>
            inc(y).flatMap(dbl)(innerPEC)
          }(outerEC)
        }
      }
    }

    "pass through simple result" in prop { (s: Result) =>
      mustExecute(1) { implicit ec => // one execution from `flatMap`
        run {
          simpleResult(s).flatMap(constant)
        } must beLeft(s)
      }
    }
  }

  "BodyParser.flatMapM" should {

    "satisfy lifted monad law 1" in prop { (x: Int) =>
      val inc = (i: Int) => constant(i + 1)
      mustExecute(2) { implicit ec =>
        /* one invocation of `flatMapM`
         * giving two internal executions
         */
        run {
          constant(x).flatMapM(inc andThen Future.successful)
        } must_== run {
          inc(x)
        }
      }
    }

    "satisfy lifted monad law 2" in prop { (x: Int) =>
      mustExecute(2) { implicit ec => // two executions from `flatMapM`
        run {
          constant(x).flatMapM { y => Future.successful(constant(y)) }
        } must_== run {
          constant(x)
        }
      }
    }

    "satisfy lifted monad law 3" in prop { (x: Int) =>
      val inc = (i: Int) => Future.successful(constant(i + 1))
      val dbl = (i: Int) => Future.successful(constant(i * 2))
      mustExecute(6, 2, 1) { (flatMapMEC, innerFlatMapMEC, innerMapEC) =>
        val innerFlatMapMPEC = innerFlatMapMEC.prepare()
        val innerMapPEC = innerMapEC.prepare()
        /* four invocations of `flatMapM`
         * giving eight internal executions
         * and one from `Future.map`
         */
        run {
          constant(x).flatMapM(inc)(flatMapMEC)
            .flatMapM(dbl)(flatMapMEC)
        } must_== run {
          constant(x).flatMapM { y =>
            inc(y).map { z =>
              z.flatMapM(dbl)(innerFlatMapMPEC)
            }(innerMapPEC)
          }(flatMapMEC)
        }
      }
    }

    "pass through simple result" in prop { (s: Result) =>
      mustExecute(1) { implicit ec => // one execution from `flatMapM`
        run {
          simpleResult(s).flatMapM { x => Future.successful(constant(x)) }
        } must beLeft(s)
      }
    }
  }

  "BodyParser.validate" should {

    "satisfy right-biased functor law 1" in prop { (x: Int) =>
      val id = (i: Int) => Right(i)
      mustExecute(1) { implicit ec => // one execution from `validate`
        run {
          constant(x).validate(id)
        } must beRight(x)
      }
    }

    "satisfy right-biased functor law 2" in prop { (x: Int) =>
      val inc = (i: Int) => Right(i + 1)
      val dbl = (i: Int) => Right(i * 2)
      mustExecute(3) { implicit ec => // three executions from `validate`
        run {
          constant(x).validate(inc)
            .validate(dbl)
        } must_== run {
          constant(x).validate { y =>
            inc(y).right.flatMap(dbl)
          }
        }
      }
    }

    "pass through simple result (case 1)" in prop { (s: Result) =>
      mustExecute(1) { implicit ec => // one execution from `validate`
        run {
          simpleResult(s).validate(Right.apply)
        } must beLeft(s)
      }
    }

    "pass through simple result (case 2)" in prop { (s1: Result, s2: Result) =>
      mustExecute(1) { implicit ec => // one execution from `validate`
        run {
          simpleResult(s1).validate { _ => Left(s2) }
        } must beLeft(s1)
      }
    }

    "fail with simple result" in prop { (s: Result) =>
      mustExecute(1) { implicit ec => // one execution from `validate`
        run {
          constant(0).validate { _ => Left(s) }
        } must beLeft(s)
      }
    }
  }

  "BodyParser.validateM" should {

    "satisfy right-biased, lifted functor law 1" in prop { (x: Int) =>
      val id = (i: Int) => Future.successful(Right(i))
      mustExecute(1) { implicit ec => // one execution from `validateM`
        run {
          constant(x).validateM(id)
        } must beRight(x)
      }
    }

    "satisfy right-biased, lifted functor law 2" in prop { (x: Int) =>
      val inc = (i: Int) => Future.successful(Right(i + 1))
      val dbl = (i: Int) => Future.successful(Right(i * 2))
      mustExecute(3) { implicit ec => // three executions from `validateM`
        run {
          constant(x).validateM(inc).validateM(dbl)
        } must_== run {
          constant(x).validateM { y =>
            Future.successful(Right((y + 1) * 2))
          }
        }
      }
    }

    "pass through simple result (case 1)" in prop { (s: Result) =>
      mustExecute(1) { implicit ec => // one execution from `validateM`
        run {
          simpleResult(s).validateM { x => Future.successful(Right(x)) }
        } must beLeft(s)
      }
    }

    "pass through simple result (case 2)" in prop { (s1: Result, s2: Result) =>
      mustExecute(1) { implicit ec => // one execution from `validateM`
        run {
          simpleResult(s1).validateM { _ => Future.successful(Left(s2)) }
        } must beLeft(s1)
      }
    }

    "fail with simple result" in prop { (s: Result) =>
      mustExecute(1) { implicit ec => // one execution from `validateM`
        run {
          constant(0).validateM { _ => Future.successful(Left(s)) }
        } must beLeft(s)
      }
    }
  }

}
