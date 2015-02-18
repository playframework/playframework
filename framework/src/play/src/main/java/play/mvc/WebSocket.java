/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import akka.actor.ActorRef;
import akka.actor.Props;
import play.libs.F.*;

/**
 * A WebSocket result.
 */
public abstract class WebSocket<A> {

    /**
     * Called when the WebSocket is ready
     *
     * @param in The Socket in.
     * @param out The Socket out.
     */
    public abstract void onReady(In<A> in, Out<A> out);

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

    /**
     * A WebSocket out.
     */
    public static interface Out<A> {

        /**
         * Writes a frame.
         */
        public void write(A frame);

        /**
         * Close this channel.
         */
        public void close();
    }

    /**
     * A WebSocket in.
     */
    public static class In<A> {

        /**
         * Callbacks to invoke at each frame.
         */
        public final List<Callback<A>> callbacks = new CopyOnWriteArrayList<Callback<A>>();

        /**
         * Callbacks to invoke on close.
         */
        public final List<Callback0> closeCallbacks = new CopyOnWriteArrayList<Callback0>();

        /**
         * Registers a message callback.
         */
        public void onMessage(Callback<A> callback) {
            callbacks.add(callback);
        }

        /**
         * Registers a close callback.
         */
        public void onClose(Callback0 callback) {
            closeCallbacks.add(callback);
        }

    }

    /**
     * Creates a WebSocket. The abstract {@code onReady} method is
     * implemented using the specified {@code Callback2<In<A>, Out<A>>}
     *
     * @param callback the callback used to implement onReady
     * @return a new WebSocket
     * @throws NullPointerException if the specified callback is null
     */
    public static <A> WebSocket<A> whenReady(Callback2<In<A>, Out<A>> callback) {
        return new WhenReadyWebSocket<A>(callback);
    }

    /**
     * Rejects a WebSocket.
     *
     * @param result The result that will be returned.
     * @return A rejected WebSocket.
     */
    public static <A> WebSocket<A> reject(final Result result) {
        return new WebSocket<A>() {
            public void onReady(In<A> in, Out<A> out) {
            }
            public Result rejectWith() {
                return result;
            }
        };
    }

    /**
     * Handles a WebSocket with an actor.
     *
     * @param props The function used to create the props for the actor.  The passed in argument is the upstream actor.
     * @return An actor WebSocket.
     */
    public static <A> WebSocket<A> withActor(final Function<ActorRef, Props> props) {
        return new WebSocket<A>() {
            public void onReady(In<A> in, Out<A> out) {
            }
            public boolean isActor() {
                return true;
            }
            public Props actorProps(ActorRef out) {
                try {
                    return props.apply(out);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Error e) {
                    throw e;
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        };
    }

    /**
     * An extension of WebSocket that obtains its onReady from
     * the specified {@code Callback2<In<A>, Out<A>>}.
     */
    static final class WhenReadyWebSocket<A> extends WebSocket<A> {

        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WhenReadyWebSocket.class);

        private final Callback2<In<A>, Out<A>> callback;

        WhenReadyWebSocket(Callback2<In<A>, Out<A>> callback) {
            if (callback == null) throw new NullPointerException("WebSocket onReady callback cannot be null");
            this.callback = callback;
        }

        @Override
        public void onReady(In<A> in, Out<A> out) {
            try {
                callback.invoke(in, out);
            } catch (Throwable e) {
                logger.error("Exception in WebSocket.onReady", e);
            }
        }
    }
}
