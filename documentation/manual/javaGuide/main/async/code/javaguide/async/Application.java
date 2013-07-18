package javaguide.async;

import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;

public class Application extends Controller {
    //#async
    public static Result index() {
      Promise<Integer> promiseOfInt = Promise.promise(
        new Function0<Integer>() {
          public Integer apply() {
            return intensiveComputation();
          }
        }
      );
      return async(
        promiseOfInt.map(
          new Function<Integer,Result>() {
            public Result apply(Integer i) {
              return ok("Got result: " + i);
            } 
          }
        )
      );
    }
    //#async
    public static int intensiveComputation() { return 2;}
}
