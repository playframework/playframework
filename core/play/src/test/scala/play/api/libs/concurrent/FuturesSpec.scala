/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import akka.actor.ActorSystem
import org.specs2.mutable.Specification

import scala.concurrent._
import scala.concurrent.duration._
import Futures._

// testOnly play.api.libs.concurrent.FuturesSpec
class FuturesSpec extends Specification {

  class MyService()(implicit futures: Futures) {

    def calculateWithTimeout(timeoutDuration: FiniteDuration): Future[Long] = {
      rawCalculation().withTimeout(timeoutDuration)
    }

    def rawCalculation(): Future[Long] = {
      val start = System.currentTimeMillis()
      futures.delayed(300 millis)(Future.successful(System.currentTimeMillis() - start))
    }
  }

  val timeoutDuration = 10 seconds

  "Futures" should {

    "time out if duration is too small" in {
      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      val future = new MyService().calculateWithTimeout(100 millis).recover {
        case _: TimeoutException =>
          -1L
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
          -1L
      }
      val result = Await.result(future, timeoutDuration) must be_>=(300L)
      actorSystem.terminate()
      result
    }

    "succeed with a timeout duration" in {
      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      val future = new MyService().calculateWithTimeout(600 millis).recover {
        case _: TimeoutException =>
          -1L
      }
      val result = Await.result(future, timeoutDuration) must be_>=(300L)
      actorSystem.terminate()
      result
    }

    "succeed with delay" in {
      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      val futures: Futures = Futures.actorSystemToFutures
      val future: Future[String] = futures.delay(1 second).map(_ => "hello world")

      val result = Await.result(future, timeoutDuration) must be_==("hello world")
      actorSystem.terminate()
      result
    }

  }

  "Future enriched with FutureOps implicit class" should {

    "timeout with a duration" in {
      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      val future = new MyService().rawCalculation().withTimeout(100 millis).recover {
        case _: TimeoutException =>
          -1L
      }
      val result = Await.result(future, timeoutDuration) must be_==(-1L)
      actorSystem.terminate()
      result
    }

    "succeed with a duration" in {
      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      val future = new MyService().rawCalculation().withTimeout(500 millis).recover {
        case _: TimeoutException =>
          -1L
      }
      val result = Await.result(future, timeoutDuration) must be_>=(300L)
      actorSystem.terminate()
      result
    }

    "timeout with an implicit akka.util.Timeout" in {
      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      implicit val implicitTimeout = akka.util.Timeout(100 millis)
      val future = new MyService().rawCalculation().withTimeout.recover {
        case _: TimeoutException =>
          -1L
      }
      val result = Await.result(future, timeoutDuration) must be_==(-1L)
      actorSystem.terminate()
      result
    }

    "succeed with an implicit akka.util.Timeout" in {
      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      implicit val implicitTimeout = akka.util.Timeout(500 millis)
      val future = new MyService().rawCalculation().withTimeout.recover {
        case _: TimeoutException =>
          -1L
      }
      val result = Await.result(future, timeoutDuration) must be_>=(300L)
      actorSystem.terminate()
      result
    }
  }

}
