/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.Try

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory
import org.apache.pekko.actor.setup.ActorSystemSetup
import org.apache.pekko.actor.setup.Setup
import org.apache.pekko.actor.typed.Scheduler
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.ActorContext
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.BootstrapSetup
import org.apache.pekko.actor.ClassicActorSystemProvider
import org.apache.pekko.actor.CoordinatedShutdown
import org.apache.pekko.actor.Props
import org.apache.pekko.stream.Materializer
import org.apache.pekko.Done
import org.slf4j.LoggerFactory
import play.api._
import play.api.inject._

/**
 * Helper to access the application defined Pekko Actor system.
 */
object Pekko {

  /**
   * Create a provider for an actor implemented by the given class, with the given name.
   *
   * This will instantiate the actor using Play's injector, allowing it to be dependency injected itself.  The returned
   * provider will provide the ActorRef for the actor, allowing it to be injected into other components.
   *
   * Typically, you will want to use this in combination with a named qualifier, so that multiple ActorRefs can be
   * bound, and the scope should be set to singleton or eager singleton.
   * *
   *
   * @param name  The name of the actor.
   * @param props A function to provide props for the actor. The props passed in will just describe how to create the
   *              actor, this function can be used to provide additional configuration such as router and dispatcher
   *              configuration.
   * @tparam T The class that implements the actor.
   * @return A provider for the actor.
   */
  def providerOf[T <: Actor: ClassTag](name: String, props: Props => Props = identity): Provider[ActorRef] =
    new ActorRefProvider(name, props)

  /**
   * Create a binding for an actor implemented by the given class, with the given name.
   *
   * This will instantiate the actor using Play's injector, allowing it to be dependency injected itself.  The returned
   * binding will provide the ActorRef for the actor, qualified with the given name, allowing it to be injected into
   * other components.
   *
   * Example usage from a Play module:
   * {{{
   * def bindings = Seq(
   *   Pekko.bindingOf[MyActor]("myActor"),
   *   ...
   * )
   * }}}
   *
   * Then to use the above actor in your application, add a qualified injected dependency, like so:
   * {{{
   *   class MyController @Inject() (@Named("myActor") myActor: ActorRef,
   *      val controllerComponents: ControllerComponents) extends BaseController {
   *     ...
   *   }
   * }}}
   *
   * @param name  The name of the actor.
   * @param props A function to provide props for the actor. The props passed in will just describe how to create the
   *              actor, this function can be used to provide additional configuration such as router and dispatcher
   *              configuration.
   * @tparam T The class that implements the actor.
   * @return A binding for the actor.
   */
  def bindingOf[T <: Actor: ClassTag](name: String, props: Props => Props = identity): Binding[ActorRef] =
    bind[ActorRef].qualifiedWith(name).to(providerOf[T](name, props)).eagerly()
}

/**
 * Components for configuring Pekko.
 */
trait PekkoComponents {
  def environment: Environment

  def configuration: Configuration

  @deprecated("Since Play 2.7.0 this is no longer required to create an ActorSystem.", "2.7.0")
  def applicationLifecycle: ApplicationLifecycle

  lazy val actorSystem: ActorSystem = new ActorSystemProvider(environment, configuration).get

  lazy val classicActorSystemProvider: ClassicActorSystemProvider = new ClassicActorSystemProviderProvider(
    actorSystem
  ).get

  lazy val coordinatedShutdown: CoordinatedShutdown =
    new CoordinatedShutdownProvider(actorSystem, applicationLifecycle).get

  implicit lazy val materializer: Materializer = Materializer.matFromSystem(actorSystem)

  implicit lazy val executionContext: ExecutionContext = actorSystem.dispatcher
}

/**
 * Pekko Typed components.
 */
trait PekkoTypedComponents {
  def actorSystem: ActorSystem
  implicit lazy val scheduler: Scheduler = new PekkoSchedulerProvider(actorSystem).get
}

/**
 * Provider for the actor system
 */
@Singleton
class ActorSystemProvider @Inject() (environment: Environment, configuration: Configuration)
    extends Provider[ActorSystem] {
  lazy val get: ActorSystem = ActorSystemProvider.start(environment.classLoader, configuration, Nil: _*)
}

/**
 * Provider for a classic actor system provide
 */
@Singleton
class ClassicActorSystemProviderProvider @Inject() (actorSystem: ActorSystem)
    extends Provider[ClassicActorSystemProvider] {
  lazy val get: ClassicActorSystemProvider = actorSystem
}

/**
 * Provider for the default flow materializer
 */
@Singleton
class MaterializerProvider @Inject() (actorSystem: ActorSystem) extends Provider[Materializer] {
  lazy val get: Materializer = Materializer.matFromSystem(actorSystem)
}

/**
 * Provider for the default execution context
 */
@Singleton
class ExecutionContextProvider @Inject() (actorSystem: ActorSystem) extends Provider[ExecutionContextExecutor] {
  def get: ExecutionContextExecutor = actorSystem.dispatcher
}

/**
 * Provider for an [[org.apache.pekko.actor.typed.Scheduler Pekko Typed Scheduler]].
 */
@Singleton
class PekkoSchedulerProvider @Inject() (actorSystem: ActorSystem) extends Provider[Scheduler] {
  import org.apache.pekko.actor.typed.scaladsl.adapter._
  override lazy val get: Scheduler = actorSystem.scheduler.toTyped
}

object ActorSystemProvider {
  type StopHook = () => Future[_]

  private val logger = LoggerFactory.getLogger(classOf[ActorSystemProvider])

  case object ApplicationShutdownReason extends CoordinatedShutdown.Reason

  /**
   * Start an ActorSystem, using the given configuration and ClassLoader.
   *
   * @return The ActorSystem and a function that can be used to stop it.
   */
  @deprecated("Use start(ClassLoader, Configuration, Setup*) instead", "2.8.0")
  protected[ActorSystemProvider] def start(classLoader: ClassLoader, config: Configuration): ActorSystem = {
    start(classLoader, config, Nil: _*)
  }

  /**
   * Start an ActorSystem, using the given configuration, ClassLoader, and additional ActorSystem Setup.
   *
   * @return The ActorSystem and a function that can be used to stop it.
   */
  @deprecated("Use start(ClassLoader, Configuration, Setup*) instead", "2.8.0")
  protected[ActorSystemProvider] def start(
      classLoader: ClassLoader,
      config: Configuration,
      additionalSetup: Setup
  ): ActorSystem = {
    start(classLoader, config, Seq(additionalSetup): _*)
  }

  /**
   * Start an ActorSystem, using the given configuration, ClassLoader, and optional additional ActorSystem Setups.
   *
   * @return The ActorSystem and a function that can be used to stop it.
   */
  def start(classLoader: ClassLoader, config: Configuration, additionalSetups: Setup*): ActorSystem = {
    val exitJvmPath = "pekko.coordinated-shutdown.exit-jvm"
    if (config.get[Boolean](exitJvmPath)) {
      // When this setting is enabled, there'll be a deadlock at shutdown. Therefore, we
      // prevent the creation of the Actor System.
      val errorMessage =
        s"""Can't start Play: detected "$exitJvmPath = on". """ +
          s"""Using "$exitJvmPath = on" in Play may cause a deadlock when shutting down. """ +
          s"""Please set "$exitJvmPath = off""""
      logger.error(errorMessage)
      throw config.reportError(exitJvmPath, errorMessage)
    }

    val pekkoConfig: Config = {
      // normalize timeout values for Pekko's use
      // TODO: deprecate this setting (see https://github.com/playframework/playframework/issues/8442)
      val playTimeoutKey      = "play.pekko.shutdown-timeout"
      val playTimeoutDuration = Try(config.get[Duration](playTimeoutKey)).getOrElse(Duration.Inf)

      // Typesafe config used internally by Pekko doesn't support "infinite".
      // Also, the value expected is an integer so can't use Long.MaxValue.
      // Finally, Pekko requires the delay to be less than a certain threshold.
      val pekkoMaxDelay        = Int.MaxValue / 1000
      val pekkoMaxDuration     = Duration(pekkoMaxDelay, "seconds")
      val normalisedDuration   = playTimeoutDuration.min(pekkoMaxDuration)
      val pekkoTimeoutDuration = java.time.Duration.ofMillis(normalisedDuration.toMillis)

      val pekkoTimeoutKey = "pekko.coordinated-shutdown.phases.actor-system-terminate.timeout"
      // Need to manually merge and override pekkoTimeoutKey because `null` is meaningful in playTimeoutKey
      config.underlying.withValue(pekkoTimeoutKey, ConfigValueFactory.fromAnyRef(pekkoTimeoutDuration))
    }

    val name = config.get[String]("play.pekko.actor-system")

    val bootstrapSetup   = BootstrapSetup(Some(classLoader), Some(pekkoConfig), None)
    val actorSystemSetup = ActorSystemSetup(bootstrapSetup +: additionalSetups: _*)

    logger.debug(s"Starting application default Pekko system: $name")
    ActorSystem(name, actorSystemSetup)
  }
}

/**
 * Support for creating injected child actors.
 */
trait InjectedActorSupport {

  /**
   * Create an injected child actor.
   *
   * @param create  A function to create the actor.
   * @param name    The name of the actor.
   * @param props   A function to provide props for the actor. The props passed in will just describe how to create the
   *                actor, this function can be used to provide additional configuration such as router and dispatcher
   *                configuration.
   * @param context The context to create the actor from.
   * @return An ActorRef for the created actor.
   */
  def injectedChild(create: => Actor, name: String, props: Props => Props = identity)(
      implicit context: ActorContext
  ): ActorRef = {
    context.actorOf(props(Props(create)), name)
  }
}

/**
 * Provider for creating actor refs
 */
class ActorRefProvider[T <: Actor: ClassTag](name: String, props: Props => Props) extends Provider[ActorRef] {
  @Inject private var actorSystem: ActorSystem = _
  @Inject private var injector: Injector       = _

  lazy val get: ActorRef = {
    val creation = Props(injector.instanceOf[T])
    actorSystem.actorOf(props(creation), name)
  }
}

private object CoordinatedShutdownProvider {
  private val logger = LoggerFactory.getLogger(classOf[CoordinatedShutdownProvider])
}

/**
 * Provider for the coordinated shutdown
 */
@Singleton
class CoordinatedShutdownProvider @Inject() (actorSystem: ActorSystem, applicationLifecycle: ApplicationLifecycle)
    extends Provider[CoordinatedShutdown] {
  import CoordinatedShutdownProvider.logger

  lazy val get: CoordinatedShutdown = {
    logWarningWhenRunPhaseConfigIsPresent()

    implicit val ec = actorSystem.dispatcher

    val cs = CoordinatedShutdown(actorSystem)
    // Once the ActorSystem is built we can register the ApplicationLifecycle stopHooks as a CoordinatedShutdown phase.
    CoordinatedShutdown(actorSystem)
      .addTask(CoordinatedShutdown.PhaseServiceStop, "application-lifecycle-stophook")(() => {
        applicationLifecycle.stop().map(_ => Done)
      })

    cs
  }

  private def logWarningWhenRunPhaseConfigIsPresent(): Unit = {
    val config = actorSystem.settings.config
    if (config.hasPath("play.pekko.run-cs-from-phase")) {
      logger.warn(
        "Configuration 'play.pekko.run-cs-from-phase' was deprecated and has no effect. Play now runs all the CoordinatedShutdown phases."
      )
    }
  }
}
