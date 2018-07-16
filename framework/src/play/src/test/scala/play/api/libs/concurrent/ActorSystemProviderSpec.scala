/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import java.util.concurrent.atomic.AtomicBoolean

import akka.Done
import akka.actor.{ ActorSystem, CoordinatedShutdown }
import com.typesafe.config.{ Config, ConfigFactory, ConfigValueFactory }
import org.specs2.mutable.Specification
import play.api.inject.{ ApplicationLifecycle, DefaultApplicationLifecycle }
import play.api.{ Configuration, Environment }

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future, Promise }
import scala.util.Success

class ActorSystemProviderSpec extends Specification {

  val akkaMaxDelayInSec = 2147483
  val fiveSec = Duration(5, "seconds")
  val oneSec = Duration(100, "milliseconds")
  val mustRunPhase = CoordinatedShutdown.PhaseServiceStop
  val mustNotRunPhase = CoordinatedShutdown.PhaseBeforeServiceUnbind

  val akkaTimeoutKey = "akka.coordinated-shutdown.phases.actor-system-terminate.timeout"
  val playTimeoutKey = "play.akka.shutdown-timeout"

  "ActorSystemProvider" should {

    "use Play's 'play.akka.shutdown-timeout' if defined " in {
      withOverridenTimeout {
        _.withValue(playTimeoutKey, ConfigValueFactory.fromAnyRef("12 s"))
      } { actorSystem =>
        actorSystem.settings.config.getDuration(akkaTimeoutKey).getSeconds must equalTo(12)
      }
    }

    "use an infinite timeout if usingg Play's 'play.akka.shutdown-timeout = null' " in {
      withOverridenTimeout {
        _.withFallback(ConfigFactory.parseResources("src/test/resources/application-infinite-timeout.conf"))
      } { actorSystem =>
        actorSystem.settings.config.getDuration(akkaTimeoutKey).getSeconds must equalTo(akkaMaxDelayInSec)
      }
    }

    "use Play's 'Duration.Inf' when no 'play.akka.shutdown-timeout' is defined and user overwrites Akka's default" in {
      withOverridenTimeout {
        _.withValue(akkaTimeoutKey, ConfigValueFactory.fromAnyRef("21 s"))
      } { actorSystem =>
        actorSystem.settings.config.getDuration(akkaTimeoutKey).getSeconds must equalTo(akkaMaxDelayInSec)
      }
    }

    "use infinite when 'play.akka.shutdown-timeout = null' and user overwrites Akka's default" in {
      withOverridenTimeout {
        _.withFallback(ConfigFactory.parseResources("src/test/resources/application-infinite-timeout.conf"))
          .withValue(akkaTimeoutKey, ConfigValueFactory.fromAnyRef("17 s"))
      } { actorSystem =>
        actorSystem.settings.config.getDuration(akkaTimeoutKey).getSeconds must equalTo(akkaMaxDelayInSec)
      }
    }

    "run the CoordinatedShutdown from the configured phase system when the stopHook is run" in {

      val config = Configuration
        .load(Environment.simple())
        .underlying
        .withValue(
          "play.akka.run-cs-from-phase",
          ConfigValueFactory.fromAnyRef(mustRunPhase))

      val (actorSystem, _) = ActorSystemProvider.start(
        this.getClass.getClassLoader,
        Configuration(config)
      )
      val lifecycle: ApplicationLifecycle = new DefaultApplicationLifecycle()

      val promise = Promise[Done]()
      val terminated = promise.future
      val isRun = new AtomicBoolean(false)

      val cs = new CoordinatedShutdownProvider(actorSystem, lifecycle).get
      cs.addTask(mustRunPhase, "termination-promise") {
        () =>
          promise.complete(Success(Done))
          Future.successful(Done)
      }
      cs.addTask(mustNotRunPhase, "is-ignored-promise") {
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

      implicit val ctx = actorSystem.dispatcher
      // lifecycle.stop is deprecated, use CS instead.
      //      val completeShutdown = lifecycle.stop().flatMap(_ => terminated)
      val completeShutdown = cs.run(CoordinatedShutdown.UnknownReason, CoordinatedShutdownProvider.loadRunFromPhaseConfig(actorSystem))

      Await.result(completeShutdown, fiveSec) must equalTo(Done)
      isRun.get() must equalTo(false)
    }

  }

  private def withOverridenTimeout[T](reconfigure: Config => Config)(block: ActorSystem => T): T = {
    val config: Config = reconfigure(Configuration
      .load(Environment.simple())
      .underlying
      .withoutPath(playTimeoutKey)
    )
    val (actorSystem, stopHook) = ActorSystemProvider.start(
      this.getClass.getClassLoader,
      Configuration(config)
    )
    try {
      block(actorSystem)
    } finally {
      stopHook()
    }
  }

}
