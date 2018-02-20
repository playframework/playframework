/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import java.lang.reflect.Method

import akka.actor._
import com.google.inject._
import com.google.inject.assistedinject.FactoryModuleBuilder

import scala.reflect._

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
 *   class MyController @Inject() (@Named("myActor") myActor: ActorRef, val controllerComponents: ControllerComponents)
 *       extends BaseController {
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
   *       val child: ActorRef = injectedChild(myChildActorFactory(id), id)
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

