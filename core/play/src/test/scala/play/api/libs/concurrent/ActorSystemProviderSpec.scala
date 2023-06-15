/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.Future
import scala.language.postfixOps

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.CoordinatedShutdown
import org.apache.pekko.actor.CoordinatedShutdown._
import org.apache.pekko.Done
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.specs2.mutable.Specification
import play.api.inject.DefaultApplicationLifecycle
import play.api.internal.libs.concurrent.CoordinatedShutdownSupport
import play.api.Configuration
import play.api.Environment
import play.api.PlayException

class ActorSystemProviderSpec extends Specification {
  val pekkoMaxDuration = (Int.MaxValue / 1000).seconds
  val pekkoTimeoutKey  = "pekko.coordinated-shutdown.phases.actor-system-terminate.timeout"
  val playTimeoutKey  = "play.pekko.shutdown-timeout"
  val pekkoExitJvmKey  = "pekko.coordinated-shutdown.exit-jvm"

  "ActorSystemProvider" should {
    s"use '$playTimeoutKey'" in {
      testTimeout(s"$playTimeoutKey = 12s", 12.seconds)
    }

    s"use Pekko's max duration if '$playTimeoutKey = null' " in {
      testTimeout(s"$playTimeoutKey = null", pekkoMaxDuration)
    }

    s"use Pekko's max duration when no '$playTimeoutKey' is defined, ignoring '$pekkoTimeoutKey'" in {
      testTimeout(s"$pekkoTimeoutKey = 21s", pekkoMaxDuration)
    }

    s"use Pekko's max duration when '$playTimeoutKey = null', ignoring '$pekkoTimeoutKey'" in {
      testTimeout(s"$playTimeoutKey = null\n$pekkoTimeoutKey = 17s", pekkoMaxDuration)
    }

    s"fail to start if '$pekkoExitJvmKey = on'" in {
      withConfiguration { config => ConfigFactory.parseString(s"$pekkoExitJvmKey = on").withFallback(config) }(
        identity
      ) must throwA[
        PlayException
      ]
    }

    s"start as expected if '$pekkoExitJvmKey = off'" in {
      withConfiguration { config => ConfigFactory.parseString(s"$pekkoExitJvmKey = off").withFallback(config) } {
        actorSystem => actorSystem.dispatcher must not beNull
      }
    }

    s"start as expected with the default configuration for $pekkoExitJvmKey" in {
      withConfiguration(identity) { actorSystem => actorSystem.dispatcher must not beNull }
    }

    "run all the phases for coordinated shutdown" in {
      // The default phases of Pekko CoordinatedShutdown are ordered as a DAG by defining the
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
        // Add a custom phase which executes after the last one defined by Pekko.
        .withValue(
          "pekko.coordinated-shutdown.phases.custom-defined-phase.depends-on",
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
      val pekkoTimeout = actorSystem.settings.config.getDuration(pekkoTimeoutKey)
      Duration.fromNanos(pekkoTimeout.toNanos) must equalTo(expected)
    }
  }
}
