package play.mvc;

import java.util.*;

import play.libs.F.*;

public abstract class WebSocket<A> {

    public abstract void onReady(In<A> in, Out<A> out);
    
    public static interface Out<A> {
        public void write(A frame);
        public void close();
    }

    public static class In<A> {

        public final List<Callback<A>> callbacks = new ArrayList<Callback<A>>();
        public final List<Callback0> closeCallbacks = new ArrayList<Callback0>();

        public void onMessage(Callback<A> callback) {
            callbacks.add(callback);
        }
        
        public void onClose(Callback0 callback) {
            closeCallbacks.add(callback);
        }

    }

}
