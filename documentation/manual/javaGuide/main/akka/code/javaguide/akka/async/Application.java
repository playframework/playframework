/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.akka.async;

//#async
import play.libs.F.*;
import play.mvc.*;
import java.util.concurrent.Callable;

import static play.libs.F.Promise.promise;

public class Application extends Controller {
    public static Promise<Result> index() {
        return promise(new Function0<Integer>() {
            public Integer apply() {
                return longComputation();
            }
        }).map(new Function<Integer,Result>() {
            public Result apply(Integer i) {
            return ok("Got " + i);
            }
        });
    }
    //###skip: 1
    public static int longComputation() { return 2;}
}
//#async
