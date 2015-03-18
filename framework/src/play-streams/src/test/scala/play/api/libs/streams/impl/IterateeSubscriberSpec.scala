/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.streams.impl

import org.specs2.mutable.Specification
import play.api.libs.iteratee._
import scala.concurrent.duration.{ FiniteDuration => ScalaFiniteDuration, SECONDS }
import scala.concurrent.{ Await, Promise }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success, Try }

class IterateeSubscriberSpec extends Specification {

  import PublisherEvents._
  case class ContInput(input: Input[_])
  case class Result(result: Any)

  class TestEnv[T] extends EventRecorder() with PublisherEvents[T] {

    @volatile
    var nextIterateePromise = Promise[Iteratee[T, T]]()
    def nextIteratee = Iteratee.flatten(nextIterateePromise.future)

    // Initialize
    {
      val iter = nextIteratee
      val subr = new IterateeSubscriber(iter)
      subr.result.unflatten.onComplete { tryStep: Try[Step[T, T]] =>
        record(Result(tryStep))

      }
      publisher.subscribe(subr)
    }

    def contStep(): Unit = {
      val oldPromise = nextIterateePromise
      nextIterateePromise = Promise[Iteratee[T, T]]()
      oldPromise.success(Cont { input =>
        record(ContInput(input))
        nextIteratee
      })
    }

    def doneStep(result: T, remaining: Input[T]): Unit = {
      nextIterateePromise.success(Done(result, remaining))
    }

    def errorStep(msg: String, input: Input[T]): Unit = {
      nextIterateePromise.success(Error(msg, input))
    }

  }

  "IterateeSubscriber" should {
    "consume 1 item" in {
      val enum = Enumerator(1, 2, 3) >>> Enumerator.eof
      val pubr = new EnumeratorPublisher(enum)
      val iter = Iteratee.getChunks[Int]
      val subr = new IterateeSubscriber(iter)
      pubr.subscribe(subr)
      Await.result(subr.result.unflatten, ScalaFiniteDuration(2, SECONDS)) must_== Done(List(1, 2, 3), Input.EOF)
    }

    "consume one element (on-subscribe/cont-step/on-next/cont-step/on-complete/done-step)" in {
      val testEnv = new TestEnv[Int]
      testEnv.onSubscribe()
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.contStep()
      testEnv.next must_== RequestMore(1)
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.onNext(1)
      testEnv.next must_== ContInput(Input.El(1))
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.contStep()
      testEnv.next must_== RequestMore(1)
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.onComplete()
      testEnv.next must_== ContInput(Input.EOF)
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.doneStep(123, Input.Empty)
      testEnv.next must_== Result(Success(Step.Done(123, Input.Empty)))
      testEnv.isEmptyAfterDelay() must beTrue
    }

    "consume one element (cont-step/on-subscribe/on-next/cont-step/on-complete/done-step)" in {
      val testEnv = new TestEnv[Int]

      testEnv.contStep()
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.onSubscribe()
      testEnv.next must_== RequestMore(1)
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.onNext(1)
      testEnv.next must_== ContInput(Input.El(1))
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.contStep()
      testEnv.next must_== RequestMore(1)
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.onComplete()
      testEnv.next must_== ContInput(Input.EOF)
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.doneStep(123, Input.Empty)
      testEnv.next must_== Result(Success(Step.Done(123, Input.Empty)))
      testEnv.isEmptyAfterDelay() must beTrue
    }

    "send EOF to cont when publisher completes immediately, moving to cont (cont-step/on-complete/cont-step)" in {
      val testEnv = new TestEnv[Int]

      testEnv.contStep()
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.onComplete()
      testEnv.next must_== ContInput(Input.EOF)
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.contStep()
      testEnv.next must beLike { case Result(Success(Step.Cont(_))) => ok }
      testEnv.isEmptyAfterDelay() must beTrue
    }

    "send EOF to cont when publisher completes immediately, moving to error (on-complete/cont-step/error-step)" in {
      val testEnv = new TestEnv[Int]

      testEnv.onComplete()
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.contStep()
      testEnv.next must_== ContInput(Input.EOF)
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.errorStep("!!!", Input.EOF)
      testEnv.next must_== Result(Success(Step.Error("!!!", Input.EOF)))
      testEnv.isEmptyAfterDelay() must beTrue
    }

    "fail when publisher errors immediately (on-error)" in {
      val testEnv = new TestEnv[Int]

      val t = new Exception("%@^%#!")
      testEnv.onError(t)
      testEnv.next must_== Result(Failure(t))
      testEnv.isEmptyAfterDelay() must beTrue
    }

    "fail when publisher errors immediately (cont-step/on-error)" in {
      val testEnv = new TestEnv[Int]

      testEnv.contStep()
      testEnv.isEmptyAfterDelay() must beTrue

      val t = new Exception("%@^%#!")
      testEnv.onError(t)
      testEnv.next must_== Result(Failure(t))
      testEnv.isEmptyAfterDelay() must beTrue
    }

    "finish when iteratee is done immediately, cancel subscription (done-step/on-subscribe)" in {
      val testEnv = new TestEnv[Int]

      testEnv.doneStep(333, Input.El(99))
      testEnv.next must_== Result(Success(Done(333, Input.El(99))))
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.onSubscribe()
      testEnv.next must_== Cancel
      testEnv.isEmptyAfterDelay() must beTrue
    }

    "finish when iteratee is done immediately, ignore complete (done-step/on-complete)" in {
      val testEnv = new TestEnv[Int]

      testEnv.doneStep(333, Input.El(99))
      testEnv.next must_== Result(Success(Done(333, Input.El(99))))
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.onComplete()
      testEnv.isEmptyAfterDelay() must beTrue
    }

    "finish when iteratee is done immediately, ignore error (done-step/on-error)" in {
      val testEnv = new TestEnv[Int]

      testEnv.doneStep(333, Input.El(99))
      testEnv.next must_== Result(Success(Done(333, Input.El(99))))
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.onError(new Exception("x"))
      testEnv.isEmptyAfterDelay() must beTrue
    }

    "finish when iteratee errors immediately, cancel subscription (done-step/on-subscribe)" in {
      val testEnv = new TestEnv[Int]

      testEnv.errorStep("iteratee error", Input.El(99))
      testEnv.next must_== Result(Success(Error("iteratee error", Input.El(99))))
      testEnv.isEmptyAfterDelay() must beTrue

      testEnv.onSubscribe()
      testEnv.next must_== Cancel
      testEnv.isEmptyAfterDelay() must beTrue
    }

  }

}
