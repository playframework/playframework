/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.streams.impl

import org.specs2.mutable.Specification
import scala.concurrent.duration.{ FiniteDuration => ScalaFiniteDuration, SECONDS }
import scala.concurrent.Promise
import scala.util.{ Failure, Success, Try }

import scala.concurrent.ExecutionContext.Implicits.global

class PromiseSubscriberSpec extends Specification {

  import PublisherEvents._
  case class OnComplete(result: Try[Any])

  class TestEnv[T] extends EventRecorder(ScalaFiniteDuration(2, SECONDS)) with PublisherEvents[T] {

    val prom = Promise[T]()
    val subr = new PromiseSubscriber(prom)
    prom.future.onComplete { result => record(OnComplete(result)) }

  }

  "PromiseSubscriber" should {
    "consume 1 item" in {
      val testEnv = new TestEnv[Int]
      import testEnv._
      isEmptyAfterDelay() must beTrue

      publisher.subscribe(subr)
      onSubscribe()
      next must_== RequestMore(1)
      isEmptyAfterDelay() must beTrue

      onNext(3)
      next must_== OnComplete(Success(3))
      isEmptyAfterDelay() must beTrue
    }
    "consume an error" in {
      val testEnv = new TestEnv[Int]
      import testEnv._
      isEmptyAfterDelay() must beTrue

      publisher.subscribe(subr)
      onSubscribe()
      next must_== RequestMore(1)
      isEmptyAfterDelay() must beTrue

      val e = new Exception("!!!")
      onError(e)
      next must_== OnComplete(Failure(e))
      isEmptyAfterDelay() must beTrue
    }
    "fail when completed too early" in {
      val testEnv = new TestEnv[Int]
      import testEnv._
      isEmptyAfterDelay() must beTrue

      publisher.subscribe(subr)
      onSubscribe()
      next must_== RequestMore(1)
      isEmptyAfterDelay() must beTrue

      onComplete()
      next must beLike { case OnComplete(Failure(_: IllegalStateException)) => ok }
      isEmptyAfterDelay() must beTrue
    }
  }

}
