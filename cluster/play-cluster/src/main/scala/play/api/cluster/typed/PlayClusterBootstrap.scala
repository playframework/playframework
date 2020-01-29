/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cluster.typed

import play.api.inject._
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.annotation.InternalApi
import akka.cluster.typed.Cluster
import akka.cluster.typed.Join
import play.api.Configuration
import play.api.Environment
import play.api.Mode
import akka.actor.CoordinatedShutdown
import akka.actor.ExtendedActorSystem
import akka.actor.CoordinatedShutdown._
import play.api.inject.Module
import scala.concurrent.Future
import akka.Done

/**
 * This provider will initialize the Akka Cluster and this node join itself forming a single node cluster,
 * but only in DevMode.
 *
 * Users may disable the cluster formation in dev mode through settings 'play.cluster.dev-mode.join-self'.
 * This would allow the creation of multi-node clusters in dev mode.
 */
@Singleton
@InternalApi
private[play] class PlayClusterBootstrap @Inject() (
    val actorSystem: ActorSystem,
    val configuration: Configuration,
    val environment: Environment
) {

  val typedActorSystem    = actorSystem.toTyped
  val nonProdEnv: Boolean = environment.mode != Mode.Prod
  val cluster             = Cluster(typedActorSystem)

  val exitJvm  = configuration.get[Boolean]("play.cluster.exit-jvm-when-downed")
  val joinSelf = configuration.get[Boolean]("play.cluster.dev-mode.join-self")

  // self-join should only happen in non Prod modes (ie: Dev and Test)
  // Users can disable it by setting 'join-self' to false
  // this can be useful if users want to start a multi-node cluster on their dev machine or on a multi-jvm test
  if (nonProdEnv && joinSelf) {
    cluster.manager ! Join.create(cluster.selfMember.address)
  }

  CoordinatedShutdown(actorSystem).addTask(PhaseClusterShutdown, "exit-jvm-when-downed") { () =>
    val shutdownReason: Option[Reason] = CoordinatedShutdown(actorSystem).shutdownReason()

    val reasons: Seq[Reason] = Seq(
      // NOTE: if this list is updated, update reference.conf file accordignly
      ClusterDowningReason,
      ClusterJoinUnsuccessfulReason,
      IncompatibleConfigurationDetectedReason
    )

    val reasonIsDowning: Boolean = shutdownReason.exists(reasons.contains)

    // if 'exitJvm' is enabled and the trigger (aka Reason) for CoordinatedShutdown is ClusterDowning,
    // JoinUnsuccessful, etc... we must exit the JVM. This can lead to the cluster closing before
    // the `Application` but we're out of the cluster (downed) already so the impact is acceptable.
    if (exitJvm && reasonIsDowning) {

      // If this code is running, it means CoordinatedShutdown was triggered. CoordinatedShutdown of
      // a given actor system can only be invoked once: further invocations block until the initial
      // one completes. The code below works as following:
      //   - create a new Thread and invoke System.exit(-1)
      //   - terminate the "exit-jvm-when-downed" task
      //   - in parallel, the invocation of System.exit(-1) has triggered the execution of the JVM shutdown Hooks
      //   - Play's JVM shutdown hooks proceed to stop Play which means stopping the Server and the
      //     Application. That also involves Play's ApplicationLifecycle. In a particular step of that stop
      //     process Play must stop its Actor System so it invokes CoordinatedShutdown. Since the
      //     CoordinatedShutdown is already running, that operation blocks.
      // !! At this point, there's a CoordinatedShutdown running (triggered by a Downing event) and a
      //    reference to a CoordinatedShutdown blocked until the run completes. The thread blocked is the
      //    JVM shutdown thread.
      //  - When the main CoordinatedShutdown completes, the JVM keeps running because Play (and Lagom)
      //    tune Akka's `exit-jvm=off`.
      //  - When the main CoordinatedShutdown completes, the thread blocked on the main CoordinatedShutdown
      //    unblocks and completes the execution of `System.exit`.
      val t = new Thread(new Runnable {
        override def run(): Unit = {
          // exit code when shutting down because of a cluster Downing event must be non-zero
          System.exit(-1)
        }
      })
      t.setDaemon(true)
      t.start()
    }
    Future.successful(Done)
  }
}
