package play.api.data.mapping

import org.specs2.mutable._
import scala.util.control.Exception._
import play.api.libs.functional._
import play.api.libs.functional.syntax._

object ValidationSpec extends Specification {

  "Validation" should {

    val success = Success[String, Int](5)
    val failure = Failure[String, Int]("err" :: Nil)

    "be a Functor" in {
      // identity
      success.map(identity) must equalTo(success)
      failure.map(identity) must equalTo(failure)
      // composition
      val p = (_: Int) + 2
      val q = (_: Int) * 3
      success.map(p compose q) must equalTo(success.map(q).map(p))
      failure.map(p compose q) must equalTo(failure.map(q).map(p))

      success.map(_ + 2) must equalTo(Success[String, Int](7))
      failure.map(_ + 2) must equalTo(failure)
    }

    "be foldable" in {
      success.fold(
        err => "err",
        identity
      ) must equalTo(5)

      failure.fold(
        err => "err",
        identity
      ) must equalTo("err")
    }

    "have an Applicative" in {
      val app = implicitly[Applicative[({type f[A] = Validation[String, A]})#f]]

      val u = Success[String, Int => Int](_ + 2)
      val v = Success[String, Int => Int](_ * 3)
      val w = Success[String, Int](5)

      app.apply(app.pure((_: Int) + 2), app.pure(5)) must equalTo(app.pure(7))

      // identity
      app.apply(app.pure[Int => Int](identity _), success) must equalTo(success)
      app.apply(app.pure[Int => Int](identity _), failure) must equalTo(failure)

      // composition
      val p = app.pure((f: Int => Int) => f compose (_: Int => Int))
      app.apply(app.apply(app.apply(p, u), v), w) must equalTo(
        app.apply(u, app.apply(v, w)))

      // homomorphism
      val f = (_: Int) + 2
      val x = 5
      app.apply(app.pure(f), app.pure(x)) must equalTo(app.pure(f(x)))

      // interchange
      app.apply(u, app.pure(x)) must equalTo(
        app.apply(app.pure((f: Int => Int) => f(x)), u))
    }

    "implement filter" in {
      success.filter((_: Int) == 5) must equalTo(success)
      Success(7).filter("err")((_: Int) == 5) must equalTo(failure)
      failure.filter((_: Int) == 5) must equalTo(failure)
    }

    "support for-comprehension" in {
      (for(x <- success) yield x + 2) must equalTo(Success(7))
      (for(x <- failure) yield x + 2) must equalTo(failure)
      (for(x <- success if x == 5) yield x + 2) must equalTo(Success(7))
      (for(x <- success if x == 7) yield x + 2) must equalTo(Failure(Nil))
      (for(x <- failure if x == 5) yield x + 2) must equalTo(failure)
    }

    "have recovery methods" in {
      success.recover {
        case _ => 42
      } must equalTo(success)

      failure.recover {
        case Failure("err" :: Nil) => 42
      } must equalTo(Success(42))

      failure.recover {
        case Failure(Nil) => 42
      } must equalTo(failure)

      success.recoverTotal {
        case _ => 42
      } must equalTo(5)

      failure.recoverTotal { _ => 42 } must equalTo(42)

      success.getOrElse(42) must equalTo(5)
      failure.getOrElse(42) must equalTo(42)

      success.orElse(Success(42)) must equalTo(success)
      failure.getOrElse(Success(42)) must equalTo(Success(42))
    }

    "be easily convertible to scala standars API types" in {
      success.asOpt must equalTo(Some(5))
      failure.asOpt must equalTo(None)

      success.asEither must equalTo(Right(5))
      failure.asEither must equalTo(Left("err" :: Nil))
    }

    "sequence" in {
      val f1: Validation[String, String] = Failure(Seq("err1"))
      val f2: Validation[String, String] = Failure(Seq("err2"))
      val s1: Validation[String, String] = Success("1")
      val s2: Validation[String, String] = Success("2")

      Validation.sequence(Seq(s1, s2)) must equalTo(Success(Seq("1", "2")))
      Validation.sequence(Seq(f1, f2)) must equalTo(Failure(Seq("err1", "err2")))
    }

  }
}
