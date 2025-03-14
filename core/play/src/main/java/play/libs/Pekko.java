/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;

import jakarta.inject.Provider;
import java.util.function.Function;
import org.apache.pekko.actor.Actor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import play.api.libs.concurrent.ActorRefProvider;
import scala.reflect.ClassTag$;
import scala.runtime.AbstractFunction1;

/** Helper to access the application defined Pekko Actor system. */
public class Pekko {

  /**
   * Create a provider for an actor implemented by the given class, with the given name.
   *
   * <p>This will instantiate the actor using Play's injector, allowing it to be dependency injected
   * itself. The returned provider will provide the ActorRef for the actor, allowing it to be
   * injected into other components.
   *
   * <p>Typically, you will want to use this in combination with a named qualifier, so that multiple
   * ActorRefs can be bound, and the scope should be set to singleton or eager singleton.
   *
   * @param <T> the type of the actor
   * @param actorClass The class that implements the actor.
   * @param name The name of the actor.
   * @param props A function to provide props for the actor. The props passed in will just describe
   *     how to create the actor, this function can be used to provide additional configuration such
   *     as router and dispatcher configuration.
   * @return A provider for the actor.
   */
  public static <T extends Actor> Provider<ActorRef> providerOf(
      Class<T> actorClass, String name, Function<Props, Props> props) {
    return new ActorRefProvider<T>(
        name,
        new AbstractFunction1<Props, Props>() {
          public Props apply(Props p) {
            return props.apply(p);
          }
        },
        ClassTag$.MODULE$.apply(actorClass));
  }

  /**
   * Create a provider for an actor implemented by the given class, with the given name.
   *
   * <p>This will instantiate the actor using Play's injector, allowing it to be dependency injected
   * itself. The returned provider will provide the ActorRef for the actor, allowing it to be
   * injected into other components.
   *
   * <p>Typically, you will want to use this in combination with a named qualifier, so that multiple
   * ActorRefs can be bound, and the scope should be set to singleton or eager singleton.
   *
   * @param <T> the type of the actor
   * @param actorClass The class that implements the actor.
   * @param name The name of the actor.
   * @return A provider for the actor.
   */
  public static <T extends Actor> Provider<ActorRef> providerOf(Class<T> actorClass, String name) {
    return providerOf(actorClass, name, Function.identity());
  }
}
