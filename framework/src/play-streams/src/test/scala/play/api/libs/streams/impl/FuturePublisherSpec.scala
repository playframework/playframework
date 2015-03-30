/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.streams.impl

import org.reactivestreams._
import org.specs2.mutable.Specification
import scala.concurrent.{ Future, Promise }

import scala.concurrent.ExecutionContext.Implicits.global

class FuturePublisherSpec extends Specification {

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

  "FuturePublisher" should {
    "produce immediate success results" in {
      val testEnv = new TestEnv[Int]
      val fut = Future.successful(1)
      val pubr = new FuturePublisher(fut)
      pubr.subscribe(testEnv.subscriber)
      testEnv.next must_== OnSubscribe
      testEnv.request(1)
      testEnv.next must_== RequestMore(1)
      testEnv.next must_== OnNext(1)
    }
    "produce immediate failure results" in {
      val testEnv = new TestEnv[Int]
      val e = new Exception("test failure")
      val fut: Future[Int] = Future.failed(e)
      val pubr = new FuturePublisher(fut)
      pubr.subscribe(testEnv.subscriber)
      testEnv.next must_== OnError(e)
      testEnv.isEmptyAfterDelay() must beTrue
    }
    "produce delayed success results" in {
      val testEnv = new TestEnv[Int]
      val prom = Promise[Int]()
      val pubr = new FuturePublisher(prom.future)
      pubr.subscribe(testEnv.subscriber)
      testEnv.next must_== OnSubscribe
      testEnv.request(1)
      testEnv.next must_== RequestMore(1)
      testEnv.isEmptyAfterDelay() must beTrue
      prom.success(3)
      testEnv.next must_== OnNext(3)
      testEnv.next must_== OnComplete
      testEnv.isEmptyAfterDelay() must beTrue
    }
    "produce delayed failure results" in {
      val testEnv = new TestEnv[Int]
      val prom = Promise[Int]()
      val pubr = new FuturePublisher(prom.future)
      pubr.subscribe(testEnv.subscriber)
      testEnv.next must_== OnSubscribe
      testEnv.request(1)
      testEnv.next must_== RequestMore(1)
      testEnv.isEmptyAfterDelay() must beTrue
      val e = new Exception("test failure")
      prom.failure(e)
      testEnv.next must_== OnError(e)
      testEnv.isEmptyAfterDelay() must beTrue
    }
  }

}
