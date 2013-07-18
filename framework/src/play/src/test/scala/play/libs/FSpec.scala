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
      fp.get() must equalTo(1) // deprecated
      fp.get(5000) must equalTo(1)
      fp.get(new java.lang.Long(5000)) must equalTo(1) // deprecated
      fp.get(5, SECONDS) must equalTo(1)
      fp.get(new java.lang.Long(5), SECONDS) must equalTo(1) // deprecated
    }

    "throw its exception" in {
      val e = new RuntimeException("x")
      val fp = F.Promise.throwing[Int](e)
      fp.get() must throwA(e) // deprecated
      fp.get(5000) must throwA(e)
      fp.get(new java.lang.Long(5000)) must throwA(e) // deprecated
      fp.get(5, SECONDS) must throwA(e)
      fp.get(new java.lang.Long(5), SECONDS) must throwA(e) // deprecated
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

    "yield a timeout value" in {
      F.Promise.timeout(1, 2).get(1, SECONDS) must equalTo(1)
      F.Promise.timeout(1, 2, MILLISECONDS).get(1, SECONDS) must equalTo(1)
    }

    "throw a timeout exception" in {
      //F.Promise.timeout().get(15, SECONDS) must throwA[TimeoutException] // Too slow to run for normal testing
      F.Promise.timeout(2).get(1, SECONDS) must throwA[TimeoutException]
      F.Promise.timeout(2, MILLISECONDS).get(1, SECONDS) must throwA[TimeoutException]
    }

    "combine a sequence of promises from a vararg" in {
      mustExecute(8) { ec =>
        import F.Promise.pure
        F.Promise.sequence[Int](ec, pure(1), pure(2), pure(3)).get(5, SECONDS) must equalTo(Arrays.asList(1, 2, 3))
      }
    }

    "combine a sequence of promises from a iterable" in {
      mustExecute(8) { ec =>
        import F.Promise.pure
        F.Promise.sequence[Int](Arrays.asList(pure(1), pure(2), pure(3)): java.lang.Iterable[F.Promise[_ <: Int]], ec).get(5, SECONDS) must equalTo(Arrays.asList(1, 2, 3))
      }
    }

  }

}