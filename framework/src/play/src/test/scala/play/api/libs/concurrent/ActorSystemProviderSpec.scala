/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.concurrent

import java.util.concurrent.atomic.AtomicBoolean

import akka.Done
import akka.actor.CoordinatedShutdown
import com.typesafe.config.ConfigValueFactory
import org.specs2.mutable.Specification
import play.api.{ Configuration, Environment }

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future, Promise }
import scala.util.Success

class ActorSystemProviderSpec extends Specification {
  val fiveSec = Duration(5, "seconds")
  val oneSec = Duration(100, "milliseconds")

  "ActorSystemProvider" should {

    "run the CoordinatedShutdown from the configured phase system when the stopHook is run" in {

      val mustRunPhase = CoordinatedShutdown.PhaseServiceStop
      val mustNotRunPhase = CoordinatedShutdown.PhaseBeforeServiceUnbind

      val config = Configuration
        .load(Environment.simple())
        .underlying
        .withValue(
          "play.akka.run-cs-from-phase",
          ConfigValueFactory.fromAnyRef(mustRunPhase))

      val (actorSystem, stopHook) = ActorSystemProvider.start(
        this.getClass.getClassLoader,
        Configuration(config)
      )

      val promise = Promise[Done]()
      val terminated = promise.future
      val isRun = new AtomicBoolean(false)

      CoordinatedShutdown(actorSystem).addTask(mustRunPhase, "termination-promise") {
        () =>
          promise.complete(Success(Done))
          Future.successful(Done)
      }
      CoordinatedShutdown(actorSystem).addTask(mustNotRunPhase, "is-ignored-promise") {
        () =>
          isRun.set(true)
          Future.successful(Done)
      }

      try {
        Await.result(terminated, oneSec)
        failure
      } catch {
        case _: Throwable =>
      }

      import scala.concurrent.ExecutionContext.Implicits.global
      val completeShutdown = stopHook().flatMap(_ => terminated)

      Await.result(completeShutdown, fiveSec) must equalTo(Done)
      isRun.get() must equalTo(false)
    }

  }

}
