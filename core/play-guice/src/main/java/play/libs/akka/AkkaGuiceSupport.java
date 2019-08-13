/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.akka;

import scala.reflect.ClassTag;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.typed.Behavior;
import akka.annotation.ApiMayChange;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import play.api.libs.concurrent.TypedActorRefProvider;
import play.libs.Akka;

import static play.api.libs.concurrent.TypedAkka.*;

import java.util.function.Function;

/**
 * Support for binding actors with Guice.
 *
 * <p>Mix this interface in with a Guice AbstractModule to get convenient support for binding
 * actors. For example:
 *
 * <pre>
 * public class MyModule extends AbstractModule implements AkkaGuiceSupport {
 *   protected void configure() {
 *     bindActor(MyActor.class, "myActor");
 *     bindTypedActor(HelloActor.class, "hello-actor");
 *   }
 * }
 * </pre>
 *
 * Then to use the above actor in your application, add a qualified injected dependency, like so:
 *
 * <pre>
 * public class MyController extends Controller {
 *   {@literal @}Inject @Named("myActor") ActorRef myActor;
 *   {@literal @}Inject ActorRef&lt;HelloActor.SayHello&gt; helloActor;
 *   ...
 * }
 * </pre>
 */
public interface AkkaGuiceSupport {

  /**
   * Bind an actor.
   *
   * <p>This will cause the actor to be instantiated by Guice, allowing it to be dependency injected
   * itself. It will bind the returned ActorRef for the actor will be bound, qualified with the
   * passed in name, so that it can be injected into other components.
   *
   * @param <T> the actor type.
   * @param actorClass The class that implements the actor.
   * @param name The name of the actor.
   * @param props A function to provide props for the actor. The props passed in will just describe
   *     how to create the actor, this function can be used to provide additional configuration such
   *     as router and dispatcher configuration.
   */
  default <T extends Actor> void bindActor(
      Class<T> actorClass, String name, Function<Props, Props> props) {
    BinderAccessor.binder(this)
        .bind(ActorRef.class)
        .annotatedWith(Names.named(name))
        .toProvider(Providers.guicify(Akka.providerOf(actorClass, name, props)))
        .asEagerSingleton();
  }

  /**
   * Bind an actor.
   *
   * <p>This will cause the actor to be instantiated by Guice, allowing it to be dependency injected
   * itself. It will bind the returned ActorRef for the actor will be bound, qualified with the
   * passed in name, so that it can be injected into other components.
   *
   * @param <T> the actor type.
   * @param actorClass The class that implements the actor.
   * @param name The name of the actor.
   */
  default <T extends Actor> void bindActor(Class<T> actorClass, String name) {
    bindActor(actorClass, name, Function.identity());
  }

  /**
   * Bind an actor factory.
   *
   * <p>This is useful for when you want to have child actors injected, and want to pass parameters
   * into them, as well as have Guice provide some of the parameters. It is intended to be used with
   * Guice's AssistedInject feature.
   *
   * <p>See <a
   * href="https://www.playframework.com/documentation/2.6.x/JavaAkka#Dependency-injecting-child-actors">Dependency-injecting-child-actors</a>
   *
   * @param <T> the actor type.
   * @param actorClass The class that implements the actor.
   * @param factoryClass The factory interface for creating the actor.
   */
  default <T extends Actor> void bindActorFactory(Class<T> actorClass, Class<?> factoryClass) {
    BinderAccessor.binder(this)
        .install(new FactoryModuleBuilder().implement(Actor.class, actorClass).build(factoryClass));
  }

  /**
   * Bind a typed actor.
   *
   * <p>For the given message type {@code T} binds {@code Behavior[T]} to the given {@link Behavior}
   * subclass and {@code ActorRef[T]} to an instance of {@link TypedActorRefProvider} with the given
   * actor name, so that it can be injected into other components.
   *
   * <p>Note that, while the name is used when spawning the actor in the {@code ActorSystem}, it is
   * <em>NOT</em> used as a name qualifier for the binding. This is so that you don't need to use
   * {@link javax.inject.Named Named} to qualify all injections of typed actors. Use the underlying
   * API to create multiple, name-annotated bindings.
   *
   * @param behaviorClass The {@code Behavior} subclass for the typed actor.
   * @param name The name of the typed actor.
   * @param <T> The type of the messages the typed actor can handle.
   */
  @ApiMayChange
  default <T> void bindTypedActor(Class<? extends Behavior<T>> behaviorClass, String name) {
    Class<T> cls = messageTypeOf(behaviorClass);
    TypedActorRefProvider<T> provider = new TypedActorRefProvider<>(name, ClassTag.apply(cls));
    BinderAccessor.binder(this).bind(behaviorOf(cls)).to(behaviorClass).asEagerSingleton();
    BinderAccessor.binder(this).bind(actorRefOf(cls)).toProvider(provider).asEagerSingleton();
  }
}
