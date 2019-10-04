/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.akka;

import akka.actor.typed.Behavior;
import play.api.libs.concurrent.BehaviorProvider;
import scala.reflect.ClassTag;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.annotation.ApiMayChange;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import play.api.libs.concurrent.TypedActorRefProvider;
import play.libs.Akka;

import static play.api.libs.concurrent.TypedAkka.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
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
 * <p>Then to use the above actor in your application, add a qualified injected dependency, like so:
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
  default <T extends akka.actor.Actor> void bindActor(
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
  default <T extends akka.actor.Actor> void bindActor(Class<T> actorClass, String name) {
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
  default <T extends akka.actor.Actor> void bindActorFactory(
      Class<T> actorClass, Class<?> factoryClass) {
    BinderAccessor.binder(this)
        .install(
            new FactoryModuleBuilder()
                .implement(akka.actor.Actor.class, actorClass)
                .build(factoryClass));
  }

  @ApiMayChange
  default <Message, Actor extends Behavior<Message>> void bindTypedActor(
      Class<Actor> behaviorClass, String name) {
    Class<Message> cls = messageTypeOf(behaviorClass);

    Optional<Method> maybeFactoryMethod =
        Arrays.stream(behaviorClass.getDeclaredMethods())
            .filter(m -> m.getName().equals("create"))
            .findFirst();

    if (!maybeFactoryMethod.isPresent())
      throw new RuntimeException(
          "Missing factory method named \"create(...)\" in typed actor " + behaviorClass.getName());
    else {
      MethodHandle methodHandle = null;
      try {
        methodHandle = MethodHandles.lookup().unreflect(maybeFactoryMethod.get());
      } catch (IllegalAccessException e) {
        throw new RuntimeException(
            "Factory method \"create(...)\" in typed actor "
                + behaviorClass.getName()
                + " must be 'public'.",
            e);
      }
      // At this point we have a MethodHandle (which is an API more convenient than the plain
      // Method) and
      // we know it's a `public xyz create(args)`
      Class<?> returnType = methodHandle.type().returnType();

      if (!returnType.isAssignableFrom(Behavior.class)) {
        throw new RuntimeException(
            "Factory method \"create(...)\" in typed actor "
                + behaviorClass.getName()
                + " must return \"Behavior<T>\" .");
      }

      BehaviorProvider<Message> behaviorProvider =
          new BehaviorProvider<Message>(methodHandle, ClassTag.apply(cls));
      BinderAccessor.binder(this)
          .bind(behaviorOf(cls))
          .toProvider(behaviorProvider)
          .asEagerSingleton();
    }

    TypedActorRefProvider<Message> actorRefProvider =
        new TypedActorRefProvider<>(name, ClassTag.apply(cls));
    BinderAccessor.binder(this)
        .bind(actorRefOf(cls))
        .toProvider(actorRefProvider)
        .asEagerSingleton();
  }
}
