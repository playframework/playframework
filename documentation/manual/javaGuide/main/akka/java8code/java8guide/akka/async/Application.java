/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.akka.async;

//#async
import play.libs.F.Promise;
import play.mvc.*;

import static play.libs.F.Promise.promise;

public class Application extends Controller {
    public static Promise<Result> index() {
        return promise(() -> longComputation())
                  .map((Integer i) -> ok("Got " + i));
    }
    //###skip: 1
    public static int longComputation() { return 2; }
}
//#async
