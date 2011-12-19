package play.mvc;

import java.util.*;

import play.libs.F.*;

public abstract class WebSocket<A> {
    
    public abstract void onReady(In<A> in, Out<A> out);
    
    public static class Out<A> {
        
        final play.api.libs.iteratee.CallbackEnumerator<A> enumerator;
        
        public Out(play.api.libs.iteratee.CallbackEnumerator<A> enumerator) {
            this.enumerator = enumerator;
        }
        
        public void write(A frame) {
            enumerator.push(frame);
        }
        
        public void close() {
            enumerator.close();
        }
        
    }
    
    public static class In<A> {
        
        public final List<Callback<A>> callbacks = new ArrayList<Callback<A>>();
        
        public void onMessage(Callback<A> callback) {
            callbacks.add(callback);
        }
        
    }
    
}