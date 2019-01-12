/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import akka.Done
import akka.actor.setup.{ ActorSystemSetup, Setup }
import akka.actor.{ CoordinatedShutdown, _ }
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.{ Config, ConfigValueFactory }
import javax.inject.{ Inject, Provider, Singleton }
import org.slf4j.LoggerFactory
import play.api._
import play.api.inject._

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.Try

/**
 * Helper to access the application defined Akka Actor system.
 */
object Akka {

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
   *   Akka.bindingOf[MyActor]("myActor"),
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
 * Components for configuring Akka.
 */
trait AkkaComponents {

  def environment: Environment

  def configuration: Configuration

  @deprecated("Since Play 2.7.0 this is no longer required to create an ActorSystem.", "2.7.0")
  def applicationLifecycle: ApplicationLifecycle

  lazy val actorSystem: ActorSystem = new ActorSystemProvider(environment, configuration).get
}

/**
 * Provider for the actor system
 */
@Singleton
class ActorSystemProvider @Inject() (environment: Environment, configuration: Configuration) extends Provider[ActorSystem] {

  lazy val get: ActorSystem = ActorSystemProvider.start(environment.classLoader, configuration)

}

/**
 * Provider for the default flow materializer
 */
@Singleton
class MaterializerProvider @Inject() (actorSystem: ActorSystem) extends Provider[Materializer] {
  lazy val get: Materializer = ActorMaterializer()(actorSystem)
}

/**
 * Provider for the default execution context
 */
@Singleton
class ExecutionContextProvider @Inject() (actorSystem: ActorSystem) extends Provider[ExecutionContextExecutor] {
  def get = actorSystem.dispatcher
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
  def start(classLoader: ClassLoader, config: Configuration): ActorSystem = {
    start(classLoader, config, additionalSetup = None)
  }

  /**
   * Start an ActorSystem, using the given configuration, ClassLoader, and additional ActorSystem Setup.
   *
   * @return The ActorSystem and a function that can be used to stop it.
   */
  def start(classLoader: ClassLoader, config: Configuration, additionalSetup: Setup): ActorSystem = {
    start(classLoader, config, Some(additionalSetup))
  }

  private def start(classLoader: ClassLoader, config: Configuration, additionalSetup: Option[Setup]): ActorSystem = {
    val exitJvmPath = "akka.coordinated-shutdown.exit-jvm"
    if (config.get[Boolean](exitJvmPath)) {
      // When this setting is enabled, there'll be a deadlock at shutdown. Therefore, we
      // prevent the creation of the Actor System.
      val errorMessage =
        s"""Can't start Play: detected "$exitJvmPath = on". Using "$exitJvmPath = on" in
           | Play may cause a deadlock when shutting down. Please set "$exitJvmPath = off""""
          .stripMargin.replace("\n", "")
      logger.error(errorMessage)
      throw config.reportError(exitJvmPath, errorMessage)
    }

    val akkaConfig: Config = {
      val akkaConfigRoot = config.get[String]("play.akka.config")

      // normalize timeout values for Akka's use
      // TODO: deprecate this setting (see https://github.com/playframework/playframework/issues/8442)
      val playTimeoutKey = "play.akka.shutdown-timeout"
      val playTimeoutDuration = Try(config.get[Duration](playTimeoutKey)).getOrElse(Duration.Inf)

      // Typesafe config used internally by Akka doesn't support "infinite".
      // Also, the value expected is an integer so can't use Long.MaxValue.
      // Finally, Akka requires the delay to be less than a certain threshold.
      val akkaMaxDelay = Int.MaxValue / 1000
      val akkaMaxDuration = Duration(akkaMaxDelay, "seconds")
      val normalisedDuration =
        if (playTimeoutDuration > akkaMaxDuration) akkaMaxDuration else playTimeoutDuration

      val akkaTimeoutKey = "akka.coordinated-shutdown.phases.actor-system-terminate.timeout"
      config.get[Config](akkaConfigRoot)
        // Need to fallback to root config so we can lookup dispatchers defined outside the main namespace
        .withFallback(config.underlying)
        // Need to manually merge and override akkaTimeoutKey because `null` is meaningful in playTimeoutKey
        .withValue(
          akkaTimeoutKey,
          ConfigValueFactory.fromAnyRef(java.time.Duration.ofMillis(normalisedDuration.toMillis))
        )
    }

    val name = config.get[String]("play.akka.actor-system")

    val bootstrapSetup = BootstrapSetup(Some(classLoader), Some(akkaConfig), None)
    val actorSystemSetup = additionalSetup match {
      case Some(setup) => ActorSystemSetup(bootstrapSetup, setup)
      case None => ActorSystemSetup(bootstrapSetup)
    }

    val system = ActorSystem(name, actorSystemSetup)
    logger.debug(s"Starting application default Akka system: $name")

    system
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
  def injectedChild(create: => Actor, name: String, props: Props => Props = identity)(implicit context: ActorContext): ActorRef = {
    context.actorOf(props(Props(create)), name)
  }
}

/**
 * Provider for creating actor refs
 */
class ActorRefProvider[T <: Actor: ClassTag](name: String, props: Props => Props) extends Provider[ActorRef] {

  @Inject private var actorSystem: ActorSystem = _
  @Inject private var injector: Injector = _
  lazy val get = {
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
class CoordinatedShutdownProvider @Inject() (actorSystem: ActorSystem, applicationLifecycle: ApplicationLifecycle) extends Provider[CoordinatedShutdown] {

  import CoordinatedShutdownProvider.logger

  lazy val get: CoordinatedShutdown = {

    logWarningWhenRunPhaseConfigIsPresent()

    val cs = CoordinatedShutdown(actorSystem)
    implicit val exCtx: ExecutionContext = actorSystem.dispatcher

    // Once the ActorSystem is built we can register the ApplicationLifecycle stopHooks as a CoordinatedShutdown phase.
    CoordinatedShutdown(actorSystem).addTask(
      CoordinatedShutdown.PhaseServiceStop,
      "application-lifecycle-stophook") { () =>
        applicationLifecycle.stop().map(_ => Done)
      }

    cs
  }

  private def logWarningWhenRunPhaseConfigIsPresent(): Unit = {
    val config = actorSystem.settings.config
    if (config.hasPath("play.akka.run-cs-from-phase")) {
      logger.warn("Configuration 'play.akka.run-cs-from-phase' was deprecated and has no effect. Play now runs all the CoordinatedShutdown phases.")
    }
  }

}

