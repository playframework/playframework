package play.libs;

import akka.actor.*;
import akka.dispatch.Future;

import play.api.*;
import play.libs.F.*;

public class Akka {
    
    public static <A> Promise<A> asPromise(Future<A> akkaFuture) {
        return new Promise<A>(
            new play.api.libs.akka.AkkaPromise<A>(akkaFuture)
        );
    }
    
    public static ActorSystem system() {
        return play.api.libs.akka.Akka.system(Play.current());
    }
    
}