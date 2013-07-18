package play.libs;

import akka.actor.ActorSystem;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import play.api.*;
import play.core.j.FPromiseHelper;
import play.libs.F.*;

/**
 * Helper to access the application defined Akka Actor system.
 */
public class Akka {

    /**
     * Transforms a Scala Future into a Play Promise.
     *
     * @deprecated Since 2.2. Use {@link Promise#wrap(Future)} instead.
     */
    @Deprecated
    public static <A> Promise<A> asPromise(Future<A> future) {
        return Promise.wrap(future);
    }

    /**
     * Retrieve the application Akka Actor system.
     */
    public static ActorSystem system() {
        return play.api.libs.concurrent.Akka.system(Play.current());
    }

    /**
     * Executes a block of code asynchronously in the application Akka Actor system.
     * 
     * @deprecated Since 2.2. Use {@link Promise#promise(Function0)} instead.
     */
    @Deprecated
    public static <T> Promise<T> future(Callable<T> callable) {
        return FPromiseHelper.promise(callable, HttpExecution.defaultContext());
    }

    /**
     * Returns a Promise which is redeemed after a period of time.
     *
     * @deprecated Since 2.2. Use {@link Promise#delayed(Function0,long,TimeUnit)} instead.
     */
    @Deprecated
    public static <T> Promise<T> timeout(Callable<T> callable, Long duration, TimeUnit unit) {
        return FPromiseHelper.delayed(callable, duration, unit, HttpExecution.defaultContext());
    }

}
