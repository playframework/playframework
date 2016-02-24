/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.streams.impl

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.iteratee.{ Step, Iteratee, Enumerator }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._

object SubscriberIterateeSpec extends Specification {

  import SubscriberEvents._

  def await[T](f: Future[T]) = Await.result(f, 5.seconds)

  trait TestEnv extends EventRecorder with SubscriberEvents with Scope {
    val iteratee = new SubscriberIteratee(subscriber)
  }

  "a subscriber iteratee" should {
    "not subscribe until fold is invoked" in new TestEnv {
      isEmptyAfterDelay() must beTrue
    }

    "not consume anything from the enumerator while there is no demand" in new TestEnv {
      iteratee.fold { folder =>
        // Record if the folder was invoked
        record(folder)
        Future.successful(())
      }
      next() must beAnInstanceOf[OnSubscribe]
      isEmptyAfterDelay() must beTrue
    }

    "not enter cont state until demand is requested" in new TestEnv {
      val step = iteratee.unflatten
      next() must beLike {
        case OnSubscribe(sub) =>
          isEmptyAfterDelay() must beTrue
          step.isCompleted must beFalse
          sub.request(1)
          await(step) must beAnInstanceOf[Step.Cont[_, _]]
      }
    }

    "publish events one at a time in response to demand" in new TestEnv {
      val result = Enumerator(10, 20, 30) |>>> iteratee
      next() must beLike {
        case OnSubscribe(sub) =>
          isEmptyAfterDelay() must beTrue
          sub.request(1)
          next() must_== OnNext(10)
          isEmptyAfterDelay() must beTrue
          sub.request(1)
          next() must_== OnNext(20)
          isEmptyAfterDelay() must beTrue
          sub.request(1)
          next() must_== OnNext(30)
          isEmptyAfterDelay() must beTrue
          sub.request(1)
          next() must_== OnComplete
      }
      await(result) must_== ()
    }

    "publish events in batches in response to demand" in new TestEnv {
      val result = Enumerator(10, 20, 30) |>>> iteratee
      next() must beLike {
        case OnSubscribe(sub) =>
          isEmptyAfterDelay() must beTrue
          sub.request(2)
          next() must_== OnNext(10)
          next() must_== OnNext(20)
          isEmptyAfterDelay() must beTrue
          sub.request(2)
          next() must_== OnNext(30)
          next() must_== OnComplete
      }
      await(result) must_== ()
    }

    "publish events all at once in response to demand" in new TestEnv {
      val result = Enumerator(10, 20, 30) |>>> iteratee
      next() must beLike {
        case OnSubscribe(sub) =>
          isEmptyAfterDelay() must beTrue
          sub.request(10)
          next() must_== OnNext(10)
          next() must_== OnNext(20)
          next() must_== OnNext(30)
          next() must_== OnComplete
      }
      await(result) must_== ()
    }

    "become done when the stream is cancelled" in new TestEnv {
      val result = Enumerator(10, 20, 30) |>>> iteratee.flatMap(_ => Iteratee.getChunks[Int])
      next() must beLike {
        case OnSubscribe(sub) =>
          sub.request(1)
          next() must_== OnNext(10)
          sub.cancel()
          isEmptyAfterDelay() must beTrue
      }
      await(result) must_== Seq(20, 30)
    }

  }

}
