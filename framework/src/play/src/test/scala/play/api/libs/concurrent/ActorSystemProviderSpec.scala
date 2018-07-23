/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import java.util.concurrent.atomic.AtomicBoolean

import akka.Done
import akka.actor.{ ActorSystem, CoordinatedShutdown }
import akka.actor.CoordinatedShutdown._
import com.typesafe.config.{ Config, ConfigFactory, ConfigValueFactory }
import org.specs2.mutable.Specification
import play.api.inject.{ ApplicationLifecycle, DefaultApplicationLifecycle }
import play.api.{ Configuration, Environment }

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class ActorSystemProviderSpec extends Specification {

  val akkaMaxDelayInSec = 2147483
  val fiveSec = Duration(5, "seconds")
  val oneSec = Duration(100, "milliseconds")

  val akkaTimeoutKey = "akka.coordinated-shutdown.phases.actor-system-terminate.timeout"
  val playTimeoutKey = "play.akka.shutdown-timeout"

  "ActorSystemProvider" should {

    "use Play's 'play.akka.shutdown-timeout' if defined " in {
      withOverriddenTimeout {
        _.withValue(playTimeoutKey, ConfigValueFactory.fromAnyRef("12 s"))
      } { actorSystem =>
        actorSystem.settings.config.getDuration(akkaTimeoutKey).getSeconds must equalTo(12)
      }
    }

    "use an infinite timeout if using Play's 'play.akka.shutdown-timeout = null' " in {
      withOverriddenTimeout {
        _.withFallback(ConfigFactory.parseResources("src/test/resources/application-infinite-timeout.conf"))
      } { actorSystem =>
        actorSystem.settings.config.getDuration(akkaTimeoutKey).getSeconds must equalTo(akkaMaxDelayInSec)
      }
    }

    "use Play's 'Duration.Inf' when no 'play.akka.shutdown-timeout' is defined and user overwrites Akka's default" in {
      withOverriddenTimeout {
        _.withValue(akkaTimeoutKey, ConfigValueFactory.fromAnyRef("21 s"))
      } { actorSystem =>
        actorSystem.settings.config.getDuration(akkaTimeoutKey).getSeconds must equalTo(akkaMaxDelayInSec)
      }
    }

    "use infinite when 'play.akka.shutdown-timeout = null' and user overwrites Akka's default" in {
      withOverriddenTimeout {
        _.withFallback(ConfigFactory.parseResources("src/test/resources/application-infinite-timeout.conf"))
          .withValue(akkaTimeoutKey, ConfigValueFactory.fromAnyRef("17 s"))
      } { actorSystem =>
        actorSystem.settings.config.getDuration(akkaTimeoutKey).getSeconds must equalTo(akkaMaxDelayInSec)
      }
    }

    "run all the phases for coordinated shutdown" in {

      val phaseBeforeServiceUnbindExecuted = new AtomicBoolean(false)
      val phaseServiceUnbindExecuted = new AtomicBoolean(false)
      val phaseServiceRequestsDoneExecuted = new AtomicBoolean(false)
      val phaseServiceStopExecuted = new AtomicBoolean(false)
      val phaseBeforeClusterShutdownExecuted = new AtomicBoolean(false)
      val phaseClusterShardingShutdownRegionExecuted = new AtomicBoolean(false)
      val phaseClusterLeaveExecuted = new AtomicBoolean(false)
      val phaseClusterExitingExecuted = new AtomicBoolean(false)
      val phaseClusterExitingDoneExecuted = new AtomicBoolean(false)
      val phaseClusterShutdownExecuted = new AtomicBoolean(false)
      val phaseBeforeActorSystemTerminateExecuted = new AtomicBoolean(false)
      val phaseActorSystemTerminateExecuted = new AtomicBoolean(false)

      val config = Configuration
        .load(Environment.simple())
        .underlying

      val (actorSystem, _) = ActorSystemProvider.start(
        this.getClass.getClassLoader,
        Configuration(config)
      )

      val lifecycle: ApplicationLifecycle = new DefaultApplicationLifecycle()

      val cs = new CoordinatedShutdownProvider(actorSystem, lifecycle).get

      def run(atomicBoolean: AtomicBoolean) = () => {
        atomicBoolean.set(true)
        Future.successful(Done)
      }

      cs.addTask(PhaseBeforeServiceUnbind, "test-BeforeServiceUnbindExecuted")(run(phaseBeforeServiceUnbindExecuted))
      cs.addTask(PhaseServiceUnbind, "test-ServiceUnbindExecuted")(run(phaseServiceUnbindExecuted))
      cs.addTask(PhaseServiceRequestsDone, "test-ServiceRequestsDoneExecuted")(run(phaseServiceRequestsDoneExecuted))
      cs.addTask(PhaseServiceStop, "test-ServiceStopExecuted")(run(phaseServiceStopExecuted))
      cs.addTask(PhaseBeforeClusterShutdown, "test-BeforeClusterShutdownExecuted")(run(phaseBeforeClusterShutdownExecuted))
      cs.addTask(PhaseClusterShardingShutdownRegion, "test-ClusterShardingShutdownRegionExecuted")(run(phaseClusterShardingShutdownRegionExecuted))
      cs.addTask(PhaseClusterLeave, "test-ClusterLeaveExecuted")(run(phaseClusterLeaveExecuted))
      cs.addTask(PhaseClusterExiting, "test-ClusterExitingExecuted")(run(phaseClusterExitingExecuted))
      cs.addTask(PhaseClusterExitingDone, "test-ClusterExitingDoneExecuted")(run(phaseClusterExitingDoneExecuted))
      cs.addTask(PhaseClusterShutdown, "test-ClusterShutdownExecuted")(run(phaseClusterShutdownExecuted))
      cs.addTask(PhaseBeforeActorSystemTerminate, "test-BeforeActorSystemTerminateExecuted")(run(phaseBeforeActorSystemTerminateExecuted))
      cs.addTask(PhaseActorSystemTerminate, "test-ActorSystemTerminateExecuted")(run(phaseActorSystemTerminateExecuted))

      CoordinatedShutdownProvider.syncShutdown(actorSystem, CoordinatedShutdown.UnknownReason)

      phaseBeforeServiceUnbindExecuted.get() must equalTo(true)
      phaseServiceUnbindExecuted.get() must equalTo(true)
      phaseServiceRequestsDoneExecuted.get() must equalTo(true)
      phaseServiceStopExecuted.get() must equalTo(true)
      phaseBeforeClusterShutdownExecuted.get() must equalTo(true)
      phaseClusterShardingShutdownRegionExecuted.get() must equalTo(true)
      phaseClusterLeaveExecuted.get() must equalTo(true)
      phaseClusterExitingExecuted.get() must equalTo(true)
      phaseClusterExitingDoneExecuted.get() must equalTo(true)
      phaseClusterShutdownExecuted.get() must equalTo(true)
      phaseBeforeActorSystemTerminateExecuted.get() must equalTo(true)
      phaseActorSystemTerminateExecuted.get() must equalTo(true)

    }

  }

  private def withOverriddenTimeout[T](reconfigure: Config => Config)(block: ActorSystem => T): T = {
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
