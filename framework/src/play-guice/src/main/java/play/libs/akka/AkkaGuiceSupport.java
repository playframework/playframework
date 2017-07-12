/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.akka;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import play.libs.Akka;

import java.util.function.Function;

/**
 * Support for binding actors with Guice.
 *
 * Mix this interface in with a Guice AbstractModule to get convenient support for binding actors.  For example:
 * <pre>
 * public class MyModule extends AbstractModule implements AkkaGuiceSupport {
 *   protected void configure() {
 *     bindActor(MyActor.class, "myActor");
 *   }
 * }
 * </pre>
 *
 * Then to use the above actor in your application, add a qualified injected dependency, like so:
 * <pre>
 * public class MyController extends Controller {
 *   {@literal @}Inject @Named("myActor") ActorRef myActor;
 *   ...
 * }
 * </pre>
 */
public interface AkkaGuiceSupport {

    /**
     * Bind an actor.
     *
     * This will cause the actor to be instantiated by Guice, allowing it to be dependency injected itself.  It will
     * bind the returned ActorRef for the actor will be bound, qualified with the passed in name, so that it can be
     * injected into other components.
     *
     * @param <T> the actor type.
     * @param actorClass The class that implements the actor.
     * @param name The name of the actor.
     * @param props A function to provide props for the actor. The props passed in will just describe how to create the
     *              actor, this function can be used to provide additional configuration such as router and dispatcher
     *              configuration.
     */
    default <T extends Actor> void bindActor(Class<T> actorClass, String name, Function<Props, Props> props) {
        BinderAccessor.binder(this).bind(ActorRef.class)
                .annotatedWith(Names.named(name))
                .toProvider(Providers.guicify(Akka.providerOf(actorClass, name, props)))
                .asEagerSingleton();
    }

    /**
     * Bind an actor.
     *
     * This will cause the actor to be instantiated by Guice, allowing it to be dependency injected itself.  It will
     * bind the returned ActorRef for the actor will be bound, qualified with the passed in name, so that it can be
     * injected into other components.
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
     * This is useful for when you want to have child actors injected, and want to pass parameters into them, as well as
     * have Guice provide some of the parameters.  It is intended to be used with Guice's AssistedInject feature.
     *
     * Let's say you have an actor that looks like this:
     *
     * <pre>
     * public class MyChildActor extends AbstractActor {
     *     final Database db;
     *     final String id;
     *
     *     {@literal @}Inject
     *     public MyChildActor(Database db, {@literal @}Assisted String id) {
     *         this.db = db;
     *         this.id = id;
     *     }
     *     ...
     * }
     * </pre>
     *
     * So {@code db} should be injected, while {@code id} should be passed.  Now, define an interface that takes the id,
     * and returns the actor:
     *
     * <pre>
     * public interface MyChildActorFactory {
     *   MyChildActor apply(String id);
     * }
     * </pre>
     *
     * Now you can use this method to bind the child actor in your module:
     *
     * <pre>
     * public class MyModule extends AbstractModule implements AkkaGuiceSupport {
     *   protected void configure() {
     *     bindActorFactory(MyChildActor.class, MyChildActorFactory.class);
     *   }
     * }
     * </pre>
     *
     * Now, when you want an actor to instantiate this as a child actor, inject `MyChildActorFactory`:
     *
     * <pre>
     * public class MyActor extends AbstractActor implements InjectedActorSupport {
     *   final MyChildActorFactory myChildActorFactory;
     *
     *   {@literal @}Inject
     *   public MyActor(MyChildActor myChildActorFactory) {
     *       this.myChildActorFactory = myChildActorFactory;
     *   }
     *
     *   {@literal @}Override
     *   public Receive createReceive() {
     *   return receiveBuilder()
     *     .match(CreateChildActor.class, msg -&gt; {
     *       ActorRef child = injectedChild(myChildActorFactory.apply(msg.getId()));
     *       sender().send(child, self);
     *     }
     *     .build()
     *   }
     * }
     * </pre>
     *
     * @param <T> the actor type.
     * @param actorClass The class that implements the actor.
     * @param factoryClass The factory interface for creating the actor.
     */
    default <T extends Actor> void bindActorFactory(Class<T> actorClass, Class<?> factoryClass) {
        BinderAccessor.binder(this).install(
                new FactoryModuleBuilder()
                        .implement(Actor.class, actorClass)
                        .build(factoryClass)
        );
    }
}
