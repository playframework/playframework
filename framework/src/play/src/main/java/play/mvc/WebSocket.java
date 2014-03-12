/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

import java.util.*;

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
        public final List<Callback<A>> callbacks = new ArrayList<Callback<A>>();

        /**
         * Callbacks to invoke on close.
         */
        public final List<Callback0> closeCallbacks = new ArrayList<Callback0>();

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
     * An extension of WebSocket that obtains its onReady from
     * the specified {@code Callback2<In<A>, Out<A>>}.
     */
    static final class WhenReadyWebSocket<A> extends WebSocket<A> {

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
                play.PlayInternal.logger().error("Exception in WebSocket.onReady", e);
            }
        }
    }
}
