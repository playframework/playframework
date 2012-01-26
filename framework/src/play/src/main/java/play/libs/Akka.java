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
            new play.api.libs.akka.AkkaPromise<A>(akkaFuture)
        );
    }
    
    /**
     * Retrieve the application Akka Actor system.
     */
    public static ActorSystem system() {
        return play.api.libs.akka.Akka.system(Play.current());
    }
    
}