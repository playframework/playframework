/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.streams.impl

import org.reactivestreams._
import org.specs2.mutable.Specification
import play.api.libs.iteratee.{ Concurrent, Enumerator, Input }
import scala.concurrent.{ Await, Future, Promise }
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

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
    "be done enumerating after EOF" in {
      val testEnv = new TestEnv[Int]
      var enumDone = Promise[Boolean]()
      val enum = (Enumerator(1, 2, 3) >>> Enumerator.eof).onDoneEnumerating {
        enumDone.success(true)
      }
      val pubr = new EnumeratorPublisher(enum)
      pubr.subscribe(testEnv.subscriber)
      testEnv.next must_== OnSubscribe
      testEnv.request(4)
      testEnv.next must_== RequestMore(4)
      testEnv.next must_== OnNext(1)
      testEnv.next must_== OnNext(2)
      testEnv.next must_== OnNext(3)
      testEnv.next must_== OnComplete
      testEnv.isEmptyAfterDelay() must beTrue
      Await.result(enumDone.future, Duration(5, SECONDS)) must beTrue
    }

    "complete the subscriber when done enumerating without eof" in {
      val testEnv = new TestEnv[Int]
      val enum = Enumerator(1, 2, 3)
      val pubr = new EnumeratorPublisher(enum)
      pubr.subscribe(testEnv.subscriber)
      testEnv.next must_== OnSubscribe
      testEnv.request(4)
      testEnv.next must_== RequestMore(4)
      testEnv.next must_== OnNext(1)
      testEnv.next must_== OnNext(2)
      testEnv.next must_== OnNext(3)
      testEnv.next must_== OnComplete
      testEnv.isEmptyAfterDelay() must beTrue
    }

    "be done enumerating after being cancelled" in {
      val testEnv = new TestEnv[Int]
      val enumDone = Promise[Boolean]()
      val (broadcastEnum, channel) = Concurrent.broadcast[Int]
      val enum = broadcastEnum.onDoneEnumerating {
        enumDone.success(true)
      }
      val pubr = new EnumeratorPublisher(enum)
      pubr.subscribe(testEnv.subscriber)
      testEnv.next must_== OnSubscribe
      testEnv.request(4)
      testEnv.next must_== RequestMore(4)
      testEnv.isEmptyAfterDelay() must beTrue
      testEnv.cancel()
      testEnv.next must_== Cancel
      // Element push occurs after cancel, so will not generate an event.
      // However it is necessary to have an event so that the publisher's
      // Cont is satisfied. We want to advance the iteratee to pick up the
      // Done iteratee caused by the cancel.
      try {
        channel.push(0)
        Await.result(enumDone.future, Duration(5, SECONDS)) must beTrue
      } catch {
        case t: Throwable =>
          // If it didn't work the first time, try again, since cancel only guarantees that the publisher will
          // eventually finish
          channel.push(0)
          Await.result(enumDone.future, Duration(5, SECONDS)) must beTrue
      }
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
    "handle errors when enumerating" in {
      val testEnv = new TestEnv[Int]
      val lotsOfItems = 0 until 25
      val exception = new Exception("x")
      val enum = Enumerator.flatten(Future.failed(exception))
      val pubr = new EnumeratorPublisher[Nothing](enum)
      pubr.subscribe(testEnv.subscriber)
      testEnv.next must_== OnSubscribe
      testEnv.request(1)
      testEnv.next must_== RequestMore(1)
      testEnv.next must beLike {
        case OnError(e) => e.getMessage must_== exception.getMessage
      }
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
