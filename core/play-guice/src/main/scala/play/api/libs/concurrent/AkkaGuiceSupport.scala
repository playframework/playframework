/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import java.lang.reflect.Method

import akka.actor._
import akka.actor.typed.Behavior
import akka.annotation.ApiMayChange
import com.google.inject._
import com.google.inject.assistedinject.FactoryModuleBuilder
import play.api.libs.concurrent.TypedAkka._

import scala.reflect._

/**
 * Support for binding actors with Guice.
 *
 * Mix this trait in with a Guice AbstractModule to get convenient support for binding actors.  For example:
 * {{{
 *   class MyModule extends AbstractModule with AkkaGuiceSupport {
 *     def configure = {
 *       bindActor[MyActor]("myActor")
 *       bindTypedActor(HelloActor(), "hello-actor")
 *     }
 *   }
 * }}}
 *
 * Then to use the above actor in your application, add a qualified injected dependency, like so:
 * {{{
 *   class MyController @Inject() (
 *       @Named("myActor") myActor: ActorRef,
 *       helloActor: ActorRef[HelloActor.SayHello],
 *       val controllerComponents: ControllerComponents,
 *   ) extends BaseController {
 *     ...
 *   }
 * }}}
 *
 * @define unnamed Note that, while the name is used when spawning the actor in the `ActorSystem`,
 *   it is <em>NOT</em> used as a name qualifier for the binding.  This is so that you don't need to
 *   use [[javax.inject.Named Named]] to qualify all injections of typed actors. Use the underlying
 *   API to create multiple, name-annotated bindings.
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
    accessBinder
      .bind(classOf[ActorRef])
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
    accessBinder.install(
      new FactoryModuleBuilder()
        .implement(classOf[Actor], implicitly[ClassTag[ActorClass]].runtimeClass.asInstanceOf[Class[_ <: Actor]])
        .build(implicitly[ClassTag[FactoryClass]].runtimeClass)
    )
  }

  /**
   * Bind a typed actor.
   *
   * Binds `Behavior[T]` and `ActorRef[T]` for the given message type `T` to the given [[Behavior]]
   * value and actor name, so that it can be injected into other components.  Use this variant of
   * `bindTypedActor` when using the "functional programming" style of defining your actor's
   * behavior and it doesn't depend on anything in dependency scope.
   *
   * $unnamed
   *
   * @param behavior The `Behavior` of the typed actor.
   * @param name The name of the typed actor.
   * @tparam T The type of the messages the typed actor can handle.
   */
  @ApiMayChange
  final def bindTypedActor[T: ClassTag](behavior: Behavior[T], name: String): Unit = {
    accessBinder.bind(behaviorOf[T]).toInstance(behavior)
    bindTypedActorRef[T](name)
  }

  /**
   * Bind a typed actor.
   *
   * Binds `Behavior[T]` and `ActorRef[T]` for the given message type `T` to the given
   * [[ActorModule]] and actor name, so that it can be injected into other components.  Use this
   * variant of `bindTypedActor` when using the "functional programming" style of defining your
   * actor's behavior and it needs to be injected with dependencies in dependency scope (such as
   * [[play.api.Configuration Configuration]]).
   *
   * The binding of the [[Behavior]] happens by installing the given `ActorModule` into this Guice
   * `Module`.  Make sure to add the [[Provides]] annotation on the `Behavior`-returning method
   * to bind (by convention this is the `apply` method).
   *
   * $unnamed
   *
   * @param actorModule The `ActorModule` that provides the behavior of the typed actor.
   * @param name The name of the typed actor.
   * @tparam T The type of the messages the typed actor can handle.
   */
  @ApiMayChange
  final def bindTypedActor[T: ClassTag](actorModule: ActorModule.Aux[T], name: String): Unit = {
    accessBinder.install(actorModule)
    bindTypedActorRef[T](name)
  }

  private[concurrent] final def bindTypedActorRef[T: ClassTag](name: String): Unit = {
    accessBinder.bind(actorRefOf[T]).toProvider(new TypedActorRefProvider[T](name)).asEagerSingleton()
  }
}
