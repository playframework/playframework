/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.akka;

import akka.actor.Actor;
import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Support for creating injected child actors.
 */
public interface InjectedActorSupport {

    /**
     * Create an injected child actor.
     *
     * @param create A function to create the actor.
     * @param name The name of the actor.
     * @param props A function to provide props for the actor. The props passed in will just describe how to create the
     *              actor, this function can be used to provide additional configuration such as router and dispatcher
     *              configuration.
     * @return An ActorRef for the created actor.
     */
    default ActorRef injectedChild(Supplier<Actor> create, String name, Function<Props, Props> props) {
        return context().actorOf(props.apply(Props.create(Actor.class, create::get)), name);
    }

    /**
     * Create an injected child actor.
     *
     * @param create A function to create the actor.
     * @param name The name of the actor.
     * @return An ActorRef for the created actor.
     */
    default ActorRef injectedChild(Supplier<Actor> create, String name) {
        return injectedChild(create, name, Function.identity());
    }

    /**
     * Context method expected to be implemented by {@link akka.actor.AbstractActor}.
     * @return the ActorContext.
     */
    ActorContext context();
}
