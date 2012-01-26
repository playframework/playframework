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

}
