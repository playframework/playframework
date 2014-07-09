/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.streams.impl

import org.reactivestreams._
import org.specs2.mutable.Specification
import play.api.libs.iteratee.{ Enumerator, Input }
import scala.concurrent.{ Future, Promise }

class EnumeratorPublisherSpec extends Specification {

  case object OnSubscribe
  case class OnError(t: Throwable)
  case class OnNext(element: Any)
  case object OnComplete
  case class RequestMore(elementCount: Int)
  case object Cancel
  case object GetSubscription

  class TestEnv[T] extends EventRecorder() {

    object subscriber extends Subscriber[T] {
      val subscription = Promise[Subscription]()
      override def onSubscribe(s: Subscription) = {
        record(OnSubscribe)
        subscription.success(s)
      }
      override def onError(t: Throwable) = record(OnError(t))
      override def onNext(element: T) = record(OnNext(element))
      override def onComplete() = record(OnComplete)
    }

    def forSubscription(f: Subscription => Any): Future[Unit] = {
      subscriber.subscription.future.map(f).map(_ => ())
    }
    def request(elementCount: Int): Future[Unit] = {
      forSubscription { s =>
        record(RequestMore(elementCount))
        s.request(elementCount)
      }
    }
    def cancel(): Future[Unit] = {
      forSubscription { s =>
        record(Cancel)
        s.cancel()
      }
    }

  }

  "EnumeratorPublisher" should {
    "enumerate one item" in {
      val testEnv = new TestEnv[Int]
      val enum = Enumerator(1) >>> Enumerator.eof
      val pubr = new EnumeratorPublisher(enum)
      pubr.subscribe(testEnv.subscriber)
      testEnv.next must_== OnSubscribe
      testEnv.request(1)
      testEnv.next must_== RequestMore(1)
      testEnv.next must_== OnNext(1)
      testEnv.request(1)
      testEnv.next must_== RequestMore(1)
      testEnv.next must_== OnComplete
      testEnv.isEmptyAfterDelay() must beTrue
    }
    "enumerate three items, with batched requests" in {
      val testEnv = new TestEnv[Int]
      val enum = Enumerator(1, 2, 3) >>> Enumerator.eof
      val pubr = new EnumeratorPublisher(enum)
      pubr.subscribe(testEnv.subscriber)
      testEnv.next must_== OnSubscribe
      testEnv.request(2)
      testEnv.next must_== RequestMore(2)
      testEnv.next must_== OnNext(1)
      testEnv.next must_== OnNext(2)
      testEnv.request(2)
      testEnv.next must_== RequestMore(2)
      testEnv.next must_== OnNext(3)
      testEnv.next must_== OnComplete
      testEnv.isEmptyAfterDelay() must beTrue
    }
    "enumerate eof only" in {
      val testEnv = new TestEnv[Int]
      val enum: Enumerator[Int] = Enumerator.eof
      val pubr = new EnumeratorPublisher(enum)
      pubr.subscribe(testEnv.subscriber)
      testEnv.next must_== OnSubscribe
      testEnv.request(1)
      testEnv.next must_== RequestMore(1)
      testEnv.next must_== OnComplete
      testEnv.isEmptyAfterDelay() must beTrue
    }
    "by default, enumerate nothing for empty" in {
      val testEnv = new TestEnv[Int]
      val enum: Enumerator[Int] = Enumerator.enumInput(Input.Empty) >>> Enumerator.eof
      val pubr = new EnumeratorPublisher(enum)
      pubr.subscribe(testEnv.subscriber)
      testEnv.next must_== OnSubscribe
      testEnv.request(1)
      testEnv.next must_== RequestMore(1)
      testEnv.next must_== OnComplete
      testEnv.isEmptyAfterDelay() must beTrue
    }
    "be able to enumerate something for empty" in {
      val testEnv = new TestEnv[Int]
      val enum: Enumerator[Int] = Enumerator.enumInput(Input.Empty) >>> Enumerator.eof
      val pubr = new EnumeratorPublisher(enum, emptyElement = Some(-1))
      pubr.subscribe(testEnv.subscriber)
      testEnv.next must_== OnSubscribe
      testEnv.request(1)
      testEnv.next must_== RequestMore(1)
      testEnv.next must_== OnNext(-1)
      testEnv.request(1)
      testEnv.next must_== RequestMore(1)
      testEnv.next must_== OnComplete
      testEnv.isEmptyAfterDelay() must beTrue
    }
    "enumerate 25 items" in {
      val testEnv = new TestEnv[Int]
      val lotsOfItems = 0 until 25
      val enum = Enumerator(lotsOfItems: _*) >>> Enumerator.eof
      val pubr = new EnumeratorPublisher(enum)
      pubr.subscribe(testEnv.subscriber)
      testEnv.next must_== OnSubscribe
      for (i <- lotsOfItems) {
        testEnv.request(1)
        testEnv.next must_== RequestMore(1)
        testEnv.next must_== OnNext(i)
      }
      testEnv.request(1)
      testEnv.next must_== RequestMore(1)
      testEnv.next must_== OnComplete
      testEnv.isEmptyAfterDelay() must beTrue
    }
  }

}