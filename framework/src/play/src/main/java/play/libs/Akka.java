package play.libs;

import akka.actor.*;
import akka.dispatch.Future;

import play.libs.F.*;

public class Akka {
    
    public static <A> Promise<A> asPromise(Future<A> akkaFuture) {
        return new Promise<A>(
            new play.api.libs.akka.AkkaPromise<A>(akkaFuture)
        );
    }
    
}