/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs

import java.util.Arrays
import java.util.concurrent.{ LinkedBlockingQueue, TimeoutException }
import java.util.concurrent.TimeUnit.{ MILLISECONDS, SECONDS }
import org.specs2.mutable._
import play.api.libs.iteratee.ExecutionSpecification
import scala.collection.JavaConverters
import scala.concurrent.{ Future, Promise }

object FSpec extends Specification
    with ExecutionSpecification {

  "An F.Promise" should {

    "wrap a Scala Future" in {
      val f = Promise.successful(1).future
      val fp = F.Promise.wrap(f)
      fp.wrapped() must equalTo(f)
    }

    "yield its value" in {
      val fp = F.Promise.pure(1)
      fp.get(5000) must equalTo(1)
      fp.get(5, SECONDS) must equalTo(1)
    }

    "throw its exception" in {
      val e = new RuntimeException("x")
      val fp = F.Promise.throwing[Int](e)
      fp.get(5000) must throwA(e)
      fp.get(5, SECONDS) must throwA(e)
      fp.get(5, SECONDS) must throwA(e)
    }

    "be able to be created from a function (with default ExecutionContext)" in {
      F.Promise.promise(new F.Function0[Int] {
        def apply() = 1
      }).get(5, SECONDS) must equalTo(1)
    }

    "be able to be created from a function (with explicit ExecutionContext)" in {
      mustExecute(1) { ec =>
        F.Promise.promise(new F.Function0[Int] {
          def apply() = 1
        }, ec).get(5, SECONDS) must equalTo(1)
      }
    }

    "be able to be created after a delay (with default ExecutionContext)" in {
      F.Promise.delayed(new F.Function0[Int] {
        def apply() = 1
      }, 1, MILLISECONDS).get(5, SECONDS) must equalTo(1)
    }

    "be able to be created after a delay (with explicit ExecutionContext)" in {
      mustExecute(1) { ec =>
        F.Promise.delayed(new F.Function0[Int] {
          def apply() = 1
        }, 1, MILLISECONDS, ec).get(5, SECONDS) must equalTo(1)
      }
    }

    "redeem with its value (with default ExecutionContext)" in {
      val p = Promise[Int]()
      val fp = F.Promise.wrap(p.future)
      val invocations = new LinkedBlockingQueue[Int]()
      fp.onRedeem(new F.Callback[Int] {
        def invoke(x: Int) { invocations.offer(x) }
      })
      p.success(99)
      invocations.poll(5, SECONDS) must equalTo(99)
    }

    "redeem with its value (with explicit ExecutionContext)" in {
      val p = Promise[Int]()
      val fp = F.Promise.wrap(p.future)
      mustExecute(1) { ec =>
        val invocations = new LinkedBlockingQueue[Int]()
        fp.onRedeem(new F.Callback[Int] {
          def invoke(x: Int) { invocations.offer(x) }
        }, ec)
        p.success(99)
        invocations.poll(5, SECONDS) must equalTo(99)
      }
    }

    "map its value (with default ExecutionContext)" in {
      val p = Promise[Int]()
      val fp = F.Promise.wrap(p.future)
      val mapped = fp.map(new F.Function[Int, Int] {
        def apply(x: Int) = 2 * x
      })
      p.success(111)
      mapped.get(5, SECONDS) must equalTo(222)
    }

    "map its value (with explicit ExecutionContext)" in {
      val p = Promise[Int]()
      val fp = F.Promise.wrap(p.future)
      mustExecute(1) { ec =>
        val mapped = fp.map(new F.Function[Int, Int] {
          def apply(x: Int) = 2 * x
        }, ec)
        p.success(111)
        mapped.get(5, SECONDS) must equalTo(222)
      }
    }

    "recover from a thrown exception (with default ExecutionContext)" in {
      val p = Promise[Int]()
      val fp = F.Promise.wrap(p.future)
      val recovered = fp.recover(new F.Function[Throwable, Int] {
        def apply(x: Throwable): Int = 99
      })
      p.failure(new RuntimeException("x"))
      recovered.get(5, SECONDS) must equalTo(99)
    }

    "recover from a thrown exception (with explicit ExecutionContext)" in {
      val p = Promise[Int]()
      val fp = F.Promise.wrap(p.future)
      mustExecute(1) { ec =>
        val recovered = fp.recover(new F.Function[Throwable, Int] {
          def apply(x: Throwable): Int = 99
        }, ec)
        p.failure(new RuntimeException("x"))
        recovered.get(5, SECONDS) must equalTo(99)
      }
    }

    "recoverWith from a thrown exception (with default ExecutionContext)" in {
      val p = Promise[Int]()
      val fp = F.Promise.wrap(p.future)
      val recovered = fp.recoverWith(new F.Function[Throwable, F.Promise[Int]] {
        def apply(x: Throwable) = F.Promise.pure(99)
      })
      p.failure(new RuntimeException("x"))
      recovered.get(5, SECONDS) must equalTo(99)
    }

    "recoverWith from a thrown exception (with explicit ExecutionContext)" in {
      val p = Promise[Int]()
      val fp = F.Promise.wrap(p.future)
      mustExecute(1) { ec =>
        val recovered = fp.recoverWith(new F.Function[Throwable, F.Promise[Int]] {
          def apply(x: Throwable) = F.Promise.pure(99)
        }, ec)
        p.failure(new RuntimeException("x"))
        recovered.get(5, SECONDS) must equalTo(99)
      }
    }

    "fallbackTo another promise" in {
      val p1 = F.Promise.throwing[Int](new RuntimeException("x"))
      val p2 = p1.fallbackTo(F.Promise.pure(42))
      p2.get(5, SECONDS) must equalTo(42)
    }

    "don't fallbackTo on success" in {
      val p1 = F.Promise.pure(1)
      val p2 = p1.fallbackTo(F.Promise.pure(2))
      p2.get(5, SECONDS) must equalTo(1)
    }

    "keep first failure when fallbackTo also fails" in {
      val p1 = F.Promise.throwing[Int](new RuntimeException("1"))
      val p2 = p1.fallbackTo(F.Promise.throwing[Int](new RuntimeException("2")))
      p2.get(5, SECONDS) must throwA[RuntimeException]("1")
    }

    "flatMap its value (with default ExecutionContext)" in {
      val p = Promise[Int]()
      val fp = F.Promise.wrap(p.future)
      val flatMapped = fp.flatMap(new F.Function[Int, F.Promise[Int]] {
        def apply(x: Int) = F.Promise.wrap(Future.successful(2 * x))
      })
      p.success(111)
      flatMapped.get(5, SECONDS) must equalTo(222)
    }

    "flatMap its value (with explicit ExecutionContext)" in {
      val p = Promise[Int]()
      val fp = F.Promise.wrap(p.future)
      mustExecute(1) { ec =>
        val flatMapped = fp.flatMap(new F.Function[Int, F.Promise[Int]] {
          def apply(x: Int) = F.Promise.wrap(Future.successful(2 * x))
        }, ec)
        p.success(111)
        flatMapped.get(5, SECONDS) must equalTo(222)
      }
    }

    "filter its value (with default ExecutionContext)" in {
      val p = Promise[Int]()
      val fp = F.Promise.wrap(p.future)
      val filtered = fp.filter(new F.Predicate[Int] {
        def test(x: Int) = x > 0
      })
      p.success(1)
      filtered.get(5, SECONDS) must equalTo(1)
    }

    "filter its value (with explicit ExecutionContext)" in {
      val p = Promise[Int]()
      val fp = F.Promise.wrap(p.future)
      mustExecute(1) { ec =>
        val filtered = fp.filter(new F.Predicate[Int] {
          def test(x: Int) = x > 0
        }, ec)
        p.success(1)
        filtered.get(5, SECONDS) must equalTo(1)
      }
    }

    "filter to failure (with default ExecutionContext)" in {
      val p = Promise[Int]()
      val fp = F.Promise.wrap(p.future)
      val filtered = fp.filter(new F.Predicate[Int] {
        def test(x: Int) = x > 0
      })
      p.success(-1)
      filtered.get(5, SECONDS) must throwA[NoSuchElementException]
    }

    "filter to failure (with explicit ExecutionContext)" in {
      val p = Promise[Int]()
      val fp = F.Promise.wrap(p.future)
      mustExecute(1) { ec =>
        val filtered = fp.filter(new F.Predicate[Int] {
          def test(x: Int) = x > 0
        }, ec)
        p.success(-1)
        filtered.get(5, SECONDS) must throwA[NoSuchElementException]
      }
    }

    "transform its successful value (with default ExecutionContext)" in {
      val p = F.Promise.pure(1)
      val mapped = p.transform(
        new F.Function[Int, Int] {
          def apply(x: Int) = 2 * x
        },
        new F.Function[Throwable, Throwable] {
          def apply(t: Throwable) = t
        }
      )
      mapped.get(5, SECONDS) must equalTo(2)
    }

    "transform its successful value (with explicit ExecutionContext)" in {
      val p = F.Promise.pure(1)
      mustExecute(1) { ec =>
        val mapped = p.transform(
          new F.Function[Int, Int] {
            def apply(x: Int) = 2 * x
          },
          new F.Function[Throwable, Throwable] {
            def apply(t: Throwable) = t
          },
          ec
        )
        mapped.get(5, SECONDS) must equalTo(2)
      }
    }

    "transform its failed throwable (with default ExecutionContext)" in {
      val p = F.Promise.throwing(new RuntimeException("1"))
      val mapped = p.transform(
        new F.Function[Int, Int] {
          def apply(x: Int) = x
        },
        new F.Function[Throwable, Throwable] {
          def apply(t: Throwable) = new RuntimeException("2")
        }
      )
      mapped.get(5, SECONDS) must throwA[RuntimeException]("2")
    }

    "transform its failed throwable (with explicit ExecutionContext)" in {
      val p = F.Promise.throwing(new RuntimeException("1"))
      mustExecute(1) { ec =>
        val mapped = p.transform(
          new F.Function[Int, Int] {
            def apply(x: Int) = x
          },
          new F.Function[Throwable, Throwable] {
            def apply(t: Throwable) = new RuntimeException("2")
          },
          ec
        )
        mapped.get(5, SECONDS) must throwA[RuntimeException]("2")
      }
    }

    "yield a timeout value" in {
      F.Promise.timeout(1, 2).get(1, SECONDS) must equalTo(1)
      F.Promise.timeout(1, 2, MILLISECONDS).get(1, SECONDS) must equalTo(1)
    }

    "throw a promise timeout exception" in {
      //F.Promise.timeout().get(15, SECONDS) must throwA[TimeoutException] // Too slow to run for normal testing
      F.Promise.timeout(2).get(1, SECONDS) must throwA[F.PromiseTimeoutException]
      F.Promise.timeout(2, MILLISECONDS).get(1, SECONDS) must throwA[F.PromiseTimeoutException]
    }

    "combine a sequence of promises from a vararg" in {
      mustExecute(8) { ec =>
        import F.Promise.pure
        F.Promise.sequence[Int](ec, pure(1), pure(2), pure(3)).get(5, SECONDS) must equalTo(Arrays.asList(1, 2, 3))
      }
    }

    "combine a sequence of promises from an iterable" in {
      mustExecute(8) { ec =>
        import F.Promise.pure
        F.Promise.sequence[Int](Arrays.asList(pure(1), pure(2), pure(3)), ec).get(5, SECONDS) must equalTo(Arrays.asList(1, 2, 3))
      }
    }

    "zip with another promise" in {
      val pa = F.Promise.pure(1)
      val pb = F.Promise.pure("hello")
      val tup = pa.zip(pb).get(1, SECONDS)
      tup._1 must equalTo(1)
      tup._2 must equalTo("hello")
    }

    def orDriver(): (Promise[Int], Promise[String], F.Promise[F.Either[Int, String]]) = {
      val pl = Promise[Int]()
      val pr = Promise[String]()
      val por = F.Promise.wrap(pl.future).or(F.Promise.wrap(pr.future))
      (pl, pr, por)
    }

    "combine with another promise with 'or'" in {
      val (pl, pr, por) = orDriver()
      por.wrapped.isCompleted must beFalse
      pl.success(1)
      val result = por.get(1, SECONDS)
      result.left.get must equalTo(1)
      result.right.isDefined must beFalse
    }

    "combine with another promise with 'or'" in {
      val (pl, pr, por) = orDriver()
      por.wrapped.isCompleted must beFalse
      pr.success("x")
      val result = por.get(1, SECONDS)
      result.left.isDefined must beFalse
      result.right.get must equalTo("x")
    }

  }

}
