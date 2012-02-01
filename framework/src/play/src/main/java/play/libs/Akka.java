package play.libs;

import akka.actor.*;
import akka.dispatch.Future;

import play.api.*;
import play.libs.F.*;

/**
 * Helper to access the application defined Akka Actor system.
 */
public class Akka {
    
    /**
     * Transform this Akka future to a Play Promise.
     */
    public static <A> Promise<A> asPromise(Future<A> akkaFuture) {
        return new Promise<A>(
            new play.api.libs.concurrent.AkkaPromise<A>(akkaFuture)
        );
    }
    
    /**
     * Retrieve the application Akka Actor system.
     */
    public static ActorSystem system() {
        return play.api.libs.concurrent.Akka.system(Play.current());
    }
    
    /**
     * Executes a block of code asynchronously in the application Akka Actor system.
     */
    public static <T> Promise<T> future(java.util.concurrent.Callable<T> callable) {
        return asPromise(akka.dispatch.Futures.future(callable, system().dispatcher()));
    }

    /**
     * Returns a Promise which is redeemed after a period of time.
     */
    public static <T> Promise<T> timeout(java.util.concurrent.Callable<T> callable, Long duration, java.util.concurrent.TimeUnit unit) {
        return new Promise(play.utils.Conversions.timeout(callable,duration,unit));
    }
    
}