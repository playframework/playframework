/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.streams.impl

import org.reactivestreams._
import org.specs2.mutable.Specification
import scala.concurrent.duration.{ FiniteDuration => ScalaFiniteDuration, SECONDS }
import scala.concurrent.{ Future, Promise }
import scala.util.{ Failure, Success, Try }

class PromiseSubscriberSpec extends Specification {

  case class RequestMore(elementCount: Int)
  case object Cancel
  case class OnComplete(result: Try[Any])

  class TestEnv[T] extends EventRecorder(ScalaFiniteDuration(2, SECONDS)) {

    val prom = Promise[T]()
    val subr = new PromiseSubscriber(prom)
    prom.future.onComplete { result => record(OnComplete(result)) }

    object publisher extends Publisher[T] {
      object subscription extends Subscription {
        val subscriber = Promise[Subscriber[T]]()

        def cancel(): Unit = {
          record(Cancel)
        }

        def request(elements: Int): Unit = {
          record(RequestMore(elements))
        }
      }
      override def subscribe(s: Subscriber[T]) = {
        subscription.subscriber.success(s)
      }
    }

    def forSubscriber(f: Subscriber[T] => Any): Future[Unit] = {
      publisher.subscription.subscriber.future.map(f).map(_ => ())
    }

    def onSubscribe(): Unit = {
      forSubscriber { s =>
        s.onSubscribe(publisher.subscription)
      }
    }

    def onNext(element: T): Unit = {
      forSubscriber { s =>
        s.onNext(element)
      }
    }

    def onError(t: Throwable): Unit = {
      forSubscriber { s =>
        s.onError(t)
      }
    }

    def onComplete(): Unit = {
      forSubscriber { s =>
        s.onComplete()
      }
    }

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