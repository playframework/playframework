/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.concurrent

import java.lang.reflect.Method

import akka.stream.{ ActorMaterializer, Materializer }
import com.google.inject.{ Binder, AbstractModule }
import com.google.inject.assistedinject.FactoryModuleBuilder
import com.typesafe.config.Config
import java.util.concurrent.TimeoutException
import javax.inject.{ Provider, Inject, Singleton }
import play.api._
import play.api.inject.{ Binding, Injector, ApplicationLifecycle, bind }
import play.core.ClosableLazy
import akka.actor._
import scala.concurrent.{ Await, ExecutionContextExecutor, Future }
import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
 * Helper to access the application defined Akka Actor system.
 */
object Akka {

  /**
   * Retrieve the application Akka Actor system, using an implicit application.
   *
   * @deprecated Please use a dependency injected ActorSystem, since 2.5.0
   *
   * Example:
   * {{{
   * val newActor = Akka.system.actorOf[Props[MyActor]]
   * }}}
   */
  @deprecated("Please use a dependency injected ActorSystem", "2.5.0")
  def system(implicit app: Application): ActorSystem = app.actorSystem

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
   *   class MyController @Inject() (@Named("myActor") myActor: ActorRef) extends Controller {
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
 * Support for binding actors with Guice.
 *
 * Mix this trait in with a Guice AbstractModule to get convenient support for binding actors.  For example:
 * {{{
 *   class MyModule extends AbstractModule with AkkaGuiceSupport {
 *     def configure = {
 *       bindActor[MyActor]("myActor")
 *     }
 *   }
 * }}}
 *
 * Then to use the above actor in your application, add a qualified injected dependency, like so:
 * {{{
 *   class MyController @Inject() (@Named("myActor") myActor: ActorRef) extends Controller {
 *     ...
 *   }
 * }}}
 */
trait AkkaGuiceSupport {
  self: AbstractModule =>

  import com.google.inject.name.Names
  import com.google.inject.util.Providers

  private def accessBinder: Binder = {
    val method: Method = classOf[AbstractModule].getDeclaredMethod("binder")
    if (!method.isAccessible) {
      method.setAccessible(true)
    }
    method.invoke(this).asInstanceOf[Binder]
  }

  /**
   * Bind an actor.
   *
   * This will cause the actor to be instantiated by Guice, allowing it to be dependency injected itself.  It will
   * bind the returned ActorRef for the actor will be bound, qualified with the passed in name, so that it can be
   * injected into other components.
   *
   * @param name The name of the actor.
   * @param props A function to provide props for the actor. The props passed in will just describe how to create the
   *              actor, this function can be used to provide additional configuration such as router and dispatcher
   *              configuration.
   * @tparam T The class that implements the actor.
   */
  def bindActor[T <: Actor: ClassTag](name: String, props: Props => Props = identity): Unit = {
    accessBinder.bind(classOf[ActorRef])
      .annotatedWith(Names.named(name))
      .toProvider(Providers.guicify(Akka.providerOf[T](name, props)))
      .asEagerSingleton()
  }

  /**
   * Bind an actor factory.
   *
   * This is useful for when you want to have child actors injected, and want to pass parameters into them, as well as
   * have Guice provide some of the parameters.  It is intended to be used with Guice's AssistedInject feature.
   *
   * Let's say you have an actor that looks like this:
   *
   * {{{
   * class MyChildActor @Inject() (db: Database, @Assisted id: String) extends Actor {
   *   ...
   * }
   * }}}
   *
   * So `db` should be injected, while `id` should be passed.  Now, define a trait that takes the id, and returns
   * the actor:
   *
   * {{{
   * trait MyChildActorFactory {
   *   def apply(id: String): Actor
   * }
   * }}}
   *
   * Now you can use this method to bind the child actor in your module:
   *
   * {{{
   *   class MyModule extends AbstractModule with AkkaGuiceSupport {
   *     def configure = {
   *       bindActorFactory[MyChildActor, MyChildActorFactory]
   *     }
   *   }
   * }}}
   *
   * Now, when you want an actor to instantiate this as a child actor, inject `MyChildActorFactory`:
   *
   * {{{
   * class MyActor @Inject() (myChildActorFactory: MyChildActorFactory) extends Actor with InjectedActorSupport {
   *
   *   def receive {
   *     case CreateChildActor(id) =>
   *       val child: ActorRef = injectedChild(myChildActoryFactory(id), id)
   *       sender() ! child
   *   }
   * }
   * }}}
   *
   * @tparam ActorClass The class that implements the actor that the factory creates
   * @tparam FactoryClass The class of the actor factory
   */
  def bindActorFactory[ActorClass <: Actor: ClassTag, FactoryClass: ClassTag]: Unit = {
    accessBinder.install(new FactoryModuleBuilder()
      .implement(classOf[Actor], implicitly[ClassTag[ActorClass]].runtimeClass.asInstanceOf[Class[_ <: Actor]])
      .build(implicitly[ClassTag[FactoryClass]].runtimeClass))
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
  def start(classLoader: ClassLoader, configuration: Configuration): (ActorSystem, StopHook) = {
    val config = PlayConfig(configuration)

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
