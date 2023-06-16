/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.pekko;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import java.util.function.Function;
import org.apache.pekko.actor.Actor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import play.libs.Pekko;

/**
 * Support for binding actors with Guice.
 *
 * <p>Mix this interface in with a Guice AbstractModule to get convenient support for binding
 * actors. For example:
 *
 * <pre>
 * public class MyModule extends AbstractModule implements PekkoGuiceSupport {
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
public interface PekkoGuiceSupport {

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
        .toProvider(Providers.guicify(Pekko.providerOf(actorClass, name, props)))
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
   * href="https://www.playframework.com/documentation/2.6.x/JavaPekko#Dependency-injecting-child-actors">Dependency-injecting-child-actors</a>
   *
   * @param <T> the actor type.
   * @param actorClass The class that implements the actor.
   * @param factoryClass The factory interface for creating the actor.
   */
  default <T extends Actor> void bindActorFactory(Class<T> actorClass, Class<?> factoryClass) {
    BinderAccessor.binder(this)
        .install(new FactoryModuleBuilder().implement(Actor.class, actorClass).build(factoryClass));
  }
}
