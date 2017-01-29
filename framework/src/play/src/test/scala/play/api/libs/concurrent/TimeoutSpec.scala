/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.concurrent

import akka.actor.ActorSystem
import org.specs2.mutable.Specification

import scala.concurrent._
import scala.concurrent.duration._

// testOnly play.api.libs.concurrent.TimeoutSpec
class TimeoutSpec extends Specification {

  class MyService(val actorSystem: ActorSystem) extends Timeout {

    def calculateWithTimeout(timeoutDuration: FiniteDuration): Future[Int] = {
      timeout(actorSystem, timeoutDuration)(rawCalculation())
    }

    def rawCalculation(): Future[Int] = {
      import akka.pattern.after
      implicit val ec = actorSystem.dispatcher
      after(300 millis, actorSystem.scheduler)(Future(42))(actorSystem.dispatcher)
    }
  }

  val timeoutDuration = 10 seconds

  "Timeout" should {

    "timeout if duration is too small" in {
      val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      val future = new MyService(actorSystem).calculateWithTimeout(100 millis).recover {
        case _: TimeoutException =>
          -1
      }
      val result = Await.result(future, timeoutDuration) must be_==(-1)
      actorSystem.terminate()
      result
    }

    "succeed eventually with the raw calculation" in {
      val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      val future = new MyService(actorSystem).rawCalculation().recover {
        case _: TimeoutException =>
          -1
      }
      val result = Await.result(future, timeoutDuration) must be_==(42)
      actorSystem.terminate()
      result
    }

    "succeed with a timeout duration" in {
      val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      val future = new MyService(actorSystem).calculateWithTimeout(600 millis).recover {
        case _: TimeoutException =>
          -1
      }
      val result = Await.result(future, timeoutDuration) must be_==(42)
      actorSystem.terminate()
      result
    }

  }

  "Future enriched with FutureTimeout implicit class" should {

    "timeout with a duration" in {
      import Timeout._

      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      val future = new MyService(actorSystem).rawCalculation().withTimeout(100 millis).recover {
        case _: TimeoutException =>
          -1
      }
      val result = Await.result(future, timeoutDuration) must be_==(-1)
      actorSystem.terminate()
      result
    }

    "succeed with a duration" in {
      import Timeout._

      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      val future = new MyService(actorSystem).rawCalculation().withTimeout(500 millis).recover {
        case _: TimeoutException =>
          -1
      }
      val result = Await.result(future, timeoutDuration) must be_==(42)
      actorSystem.terminate()
      result
    }

    "timeout with an implicit akka.util.Timeout" in {
      import Timeout._

      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      implicit val implicitTimeout = akka.util.Timeout(100 millis)
      val future = new MyService(actorSystem).rawCalculation().withTimeout.recover {
        case _: TimeoutException =>
          -1
      }
      val result = Await.result(future, timeoutDuration) must be_==(-1)
      actorSystem.terminate()
      result
    }

    "succeed with an implicit akka.util.Timeout" in {
      import Timeout._

      implicit val actorSystem = ActorSystem()
      implicit val ec = actorSystem.dispatcher
      implicit val implicitTimeout = akka.util.Timeout(500 millis)
      val future = new MyService(actorSystem).rawCalculation().withTimeout.recover {
        case _: TimeoutException =>
          -1
      }
      val result = Await.result(future, timeoutDuration) must be_==(42)
      actorSystem.terminate()
      result
    }
  }

}
