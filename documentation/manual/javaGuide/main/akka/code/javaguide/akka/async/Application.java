package javaguide.akka.async;

//#async
import play.libs.F.*;
import play.mvc.*;
import java.util.concurrent.Callable;

import static play.libs.Akka.future;

public class Application extends Controller {
    public static Result index() {
        return async(
            future(new Callable<Integer>() {
                public Integer call() {
                    return longComputation();
                }
            }).map(new Function<Integer,Result>() {
                public Result apply(Integer i) {
                return ok("Got " + i);
                }
            })
        );
    }
    //###skip 1
    public static int longComputation() { return 2;}
}
//#async
