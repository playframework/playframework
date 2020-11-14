/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import java.util.concurrent.atomic.AtomicBoolean

import akka.Done
import akka.actor.ActorSystem
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown._
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.specs2.mutable.Specification
import play.api.inject.DefaultApplicationLifecycle
import play.api.internal.libs.concurrent.CoordinatedShutdownSupport
import play.api.Configuration
import play.api.Environment
import play.api.PlayException

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._

class ActorSystemProviderSpec extends Specification {
  val akkaMaxDuration = (Int.MaxValue / 1000).seconds
  val akkaTimeoutKey  = "akka.coordinated-shutdown.phases.actor-system-terminate.timeout"
  val playTimeoutKey  = "play.akka.shutdown-timeout"
  val akkaExitJvmKey  = "akka.coordinated-shutdown.exit-jvm"

  "ActorSystemProvider" should {
    s"use '$playTimeoutKey'" in {
      testTimeout(s"$playTimeoutKey = 12s", 12.seconds)
    }

    s"use Akka's max duration if '$playTimeoutKey = null' " in {
      testTimeout(s"$playTimeoutKey = null", akkaMaxDuration)
    }

    s"use Akka's max duration when no '$playTimeoutKey' is defined, ignoring '$akkaTimeoutKey'" in {
      testTimeout(s"$akkaTimeoutKey = 21s", akkaMaxDuration)
    }

    s"use Akka's max duration when '$playTimeoutKey = null', ignoring '$akkaTimeoutKey'" in {
      testTimeout(s"$playTimeoutKey = null\n$akkaTimeoutKey = 17s", akkaMaxDuration)
    }

    s"fail to start if '$akkaExitJvmKey = on'" in {
      withConfiguration { config =>
        ConfigFactory.parseString(s"$akkaExitJvmKey = on").withFallback(config)
      }(identity) must throwA[PlayException]
    }

    s"start as expected if '$akkaExitJvmKey = off'" in {
      withConfiguration { config =>
        ConfigFactory.parseString(s"$akkaExitJvmKey = off").withFallback(config)
      } { actorSystem =>
        actorSystem.dispatcher must not beNull
      }
    }

    s"start as expected with the default configuration for $akkaExitJvmKey" in {
      withConfiguration(identity) { actorSystem =>
        actorSystem.dispatcher must not beNull
      }
    }

    "run all the phases for coordinated shutdown" in {
      // The default phases of Akka CoordinatedShutdown are ordered as a DAG by defining the
      // dependencies between the phases. That means we don't need to test each phase, but
      // just the first and the last one. We are then adding a custom phase so that we
      // can assert that Play is correctly executing CoordinatedShutdown.

      // First phase is PhaseBeforeServiceUnbind
      val phaseBeforeServiceUnbindExecuted = new AtomicBoolean(false)

      // Last phase is PhaseActorSystemTerminate
      val phaseActorSystemTerminateExecuted = new AtomicBoolean(false)

      val config = Configuration
        .load(Environment.simple())
        .underlying
        // Add a custom phase which executes after the last one defined by Akka.
        .withValue(
          "akka.coordinated-shutdown.phases.custom-defined-phase.depends-on",
          ConfigValueFactory.fromIterable(java.util.Arrays.asList("actor-system-terminate"))
        )

      // Custom phase CustomDefinedPhase
      val PhaseCustomDefinedPhase         = "custom-defined-phase"
      val phaseCustomDefinedPhaseExecuted = new AtomicBoolean(false)

      val actorSystem = ActorSystemProvider.start(getClass.getClassLoader, Configuration(config))

      val cs = new CoordinatedShutdownProvider(actorSystem, new DefaultApplicationLifecycle()).get

      def run(atomicBoolean: AtomicBoolean) = () => {
        atomicBoolean.set(true)
        Future.successful(Done)
      }

      cs.addTask(PhaseBeforeServiceUnbind, "test-BeforeServiceUnbindExecuted")(run(phaseBeforeServiceUnbindExecuted))
      cs.addTask(PhaseActorSystemTerminate, "test-ActorSystemTerminateExecuted")(run(phaseActorSystemTerminateExecuted))
      cs.addTask(PhaseCustomDefinedPhase, "test-PhaseCustomDefinedPhaseExecuted")(run(phaseCustomDefinedPhaseExecuted))

      CoordinatedShutdownSupport.syncShutdown(actorSystem, CoordinatedShutdown.UnknownReason)

      phaseBeforeServiceUnbindExecuted.get() must equalTo(true)
      phaseActorSystemTerminateExecuted.get() must equalTo(true)
      phaseCustomDefinedPhaseExecuted.get() must equalTo(true)
    }
  }

  private def withConfiguration[T](reconfigure: Config => Config)(block: ActorSystem => T): T = {
    val config      = reconfigure(Configuration.load(Environment.simple()).underlying)
    val actorSystem = ActorSystemProvider.start(getClass.getClassLoader, Configuration(config))
    try block(actorSystem)
    finally {
      Await.ready(CoordinatedShutdown(actorSystem).run(CoordinatedShutdown.UnknownReason), 5.seconds)
    }
  }

  private def testTimeout(configString: String, expected: Duration) = {
    withConfiguration { config =>
      config.withoutPath(playTimeoutKey).withFallback(ConfigFactory.parseString(configString))
    } { actorSystem =>
      val akkaTimeout = actorSystem.settings.config.getDuration(akkaTimeoutKey)
      Duration.fromNanos(akkaTimeout.toNanos) must equalTo(expected)
    }
  }
}
