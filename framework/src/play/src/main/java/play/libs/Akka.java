package play.libs;

import akka.actor.*;
import scala.concurrent.Future;

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
        return new Promise<A>(akkaFuture);
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
        return play.core.j.JavaPromise.akkaFuture(callable);
    }

    /**
     * Returns a Promise which is redeemed after a period of time.
     */
    public static <T> Promise<T> timeout(java.util.concurrent.Callable<T> callable, Long duration, java.util.concurrent.TimeUnit unit) {
        return new Promise(play.core.j.JavaPromise.timeout(callable,duration,unit));
    }

}
