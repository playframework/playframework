/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import akka.actor.ActorRef;
import akka.actor.Props;

/**
 * A legacy WebSocket result.
 *
 * @deprecated Use play.mvc.WebSocket instead.
 */
@Deprecated
public abstract class LegacyWebSocket<A> {

    /**
     * Called when the WebSocket is ready
     *
     * @param in The Socket in.
     * @param out The Socket out.
     */
    public abstract void onReady(WebSocket.In<A> in, WebSocket.Out<A> out);

    /**
     * If this method returns a result, the WebSocket will be rejected with that result.
     *
     * This method will be invoked before onReady.
     *
     * @return The result to reject the WebSocket with, or null if the WebSocket shouldn't be rejected.
     */
    public Result rejectWith() {
        return null;
    }

    /**
     * If this method returns true, then the WebSocket should be handled by an actor.  The actor will be obtained by
     * passing an ActorRef representing to the actor method, which should return the props for creating the actor.
     *
     * @return true if the websocket should be handled by an actor.
     */
    public boolean isActor() {
        return false;
    }

    /**
     * The props to create the actor to handle this WebSocket.
     *
     * @param out The actor to send upstream messages to.
     * @return The props of the actor to handle the WebSocket.  If isActor returns true, must not return null.
     */
    public Props actorProps(ActorRef out) {
        return null;
    }
}
