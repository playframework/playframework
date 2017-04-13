/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.concurrent

import akka.actor.ActorSystem
import org.specs2.mutable.Specification

import scala.concurrent._
import scala.concurrent.duration._
import Futures._

// testOnly play.api.libs.concurrent.FuturesSpec
class FuturesSpec extends Specification {

  class MyService()(implicit actorSystem: ActorSystem) {

    def calculateWithTimeout(timeoutDuration: FiniteDuration): Future[Int] = {
      rawCalculation().withTimeout(timeoutDuration)
    }

    def rawCalculation(): Future[Int] = {
      import akka.pattern.after
      implicit val ec = actorSystem.dispatcher
      after(300 millis, actorSystem.scheduler)(Future(42))(actorSystem.dispatcher)
    }
  }

  val timeoutDuration = 10 seconds

  "Futures" should {

    "futures if duration is too small" in {
      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      val future = new MyService().calculateWithTimeout(100 millis).recover {
        case _: TimeoutException =>
          -1
      }
      val result = Await.result(future, timeoutDuration) must be_==(-1)
      actorSystem.terminate()
      result
    }

    "succeed eventually with the raw calculation" in {
      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      val future = new MyService().rawCalculation().recover {
        case _: TimeoutException =>
          -1
      }
      val result = Await.result(future, timeoutDuration) must be_==(42)
      actorSystem.terminate()
      result
    }

    "succeed with a futures duration" in {
      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      val future = new MyService().calculateWithTimeout(600 millis).recover {
        case _: TimeoutException =>
          -1
      }
      val result = Await.result(future, timeoutDuration) must be_==(42)
      actorSystem.terminate()
      result
    }

  }

  "Future enriched with FutureToFutures implicit class" should {

    "futures with a duration" in {
      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      val future = new MyService().rawCalculation().withTimeout(100 millis).recover {
        case _: TimeoutException =>
          -1
      }
      val result = Await.result(future, timeoutDuration) must be_==(-1)
      actorSystem.terminate()
      result
    }

    "succeed with a duration" in {
      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      val future = new MyService().rawCalculation().withTimeout(500 millis).recover {
        case _: TimeoutException =>
          -1
      }
      val result = Await.result(future, timeoutDuration) must be_==(42)
      actorSystem.terminate()
      result
    }

    "futures with an implicit akka.util.Futures" in {
      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      implicit val implicitTimeout = akka.util.Timeout(100 millis)
      val future = new MyService().rawCalculation().withTimeout.recover {
        case _: TimeoutException =>
          -1
      }
      val result = Await.result(future, timeoutDuration) must be_==(-1)
      actorSystem.terminate()
      result
    }

    "succeed with an implicit akka.util.Futures" in {
      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      implicit val implicitTimeout = akka.util.Timeout(500 millis)
      val future = new MyService().rawCalculation().withTimeout.recover {
        case _: TimeoutException =>
          -1
      }
      val result = Await.result(future, timeoutDuration) must be_==(42)
      actorSystem.terminate()
      result
    }
  }

}
