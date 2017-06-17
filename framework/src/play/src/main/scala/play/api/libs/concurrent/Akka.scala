/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.concurrent

import java.util.concurrent.TimeoutException
import javax.inject.{ Inject, Provider, Singleton }

import akka.actor._
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.Config
import play.api._
import play.api.inject.{ ApplicationLifecycle, Binding, Injector, bind }
import play.core.ClosableLazy

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContextExecutor, Future }
import scala.reflect.ClassTag

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
   * @param name The name of the actor.
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
   * @param name The name of the actor.
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
  def applicationLifecycle: ApplicationLifecycle

  lazy val actorSystem: ActorSystem = new ActorSystemProvider(environment, configuration, applicationLifecycle).get
}

/**
 * Provider for the actor system
 */
@Singleton
class ActorSystemProvider @Inject() (environment: Environment, configuration: Configuration, applicationLifecycle: ApplicationLifecycle) extends Provider[ActorSystem] {

  private val logger = Logger(classOf[ActorSystemProvider])

  lazy val get: ActorSystem = {
    val (system, stopHook) = ActorSystemProvider.start(environment.classLoader, configuration)
    applicationLifecycle.addStopHook(stopHook)
    system
  }

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

  private val logger = Logger(classOf[ActorSystemProvider])

  /**
   * Start an ActorSystem, using the given configuration and ClassLoader.
   * @return The ActorSystem and a function that can be used to stop it.
   */
  def start(classLoader: ClassLoader, config: Configuration): (ActorSystem, StopHook) = {
    val akkaConfig: Config = {
      val akkaConfigRoot = config.get[String]("play.akka.config")
      // Need to fallback to root config so we can lookup dispatchers defined outside the main namespace
      config.get[Config](akkaConfigRoot).withFallback(config.underlying)
    }

    val name = config.get[String]("play.akka.actor-system")
    val system = ActorSystem(name, akkaConfig, classLoader)
    logger.debug(s"Starting application default Akka system: $name")

    val stopHook = { () =>
      logger.debug(s"Shutdown application default Akka system: $name")
      system.terminate()

      config.get[Duration]("play.akka.shutdown-timeout") match {
        case timeout: FiniteDuration =>
          try {
            Await.result(system.whenTerminated, timeout)
          } catch {
            case te: TimeoutException =>
              // oh well.  We tried to be nice.
              logger.info(s"Could not shutdown the Akka system in $timeout milliseconds.  Giving up.")
          }
        case _ =>
          // wait until it is shutdown
          Await.result(system.whenTerminated, Duration.Inf)
      }

      Future.successful(())
    }

    (system, stopHook)
  }

  /**
   * A lazy wrapper around `start`. Useful when the `ActorSystem` may
   * not be needed.
   */
  def lazyStart(classLoader: => ClassLoader, configuration: => Configuration): ClosableLazy[ActorSystem, Future[_]] = {
    new ClosableLazy[ActorSystem, Future[_]] {
      protected def create() = start(classLoader, configuration)
      protected def closeNotNeeded = Future.successful(())
    }
  }

}

/**
 * Support for creating injected child actors.
 */
trait InjectedActorSupport {

  /**
   * Create an injected child actor.
   *
   * @param create A function to create the actor.
   * @param name The name of the actor.
   * @param props A function to provide props for the actor. The props passed in will just describe how to create the
   *              actor, this function can be used to provide additional configuration such as router and dispatcher
   *              configuration.
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
