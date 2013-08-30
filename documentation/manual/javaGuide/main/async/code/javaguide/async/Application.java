package javaguide.async;

import play.mvc.SimpleResult;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Controller;

public class Application extends Controller {
    //#async
    public static Promise<SimpleResult> index() {
      Promise<Integer> promiseOfInt = Promise.promise(
        new Function0<Integer>() {
          public Integer apply() {
            return intensiveComputation();
          }
        }
      );
      return promiseOfInt.map(
          new Function<Integer, SimpleResult>() {
            public SimpleResult apply(Integer i) {
              return ok("Got result: " + i);
            } 
          }
        );
    }
    //#async
    public static int intensiveComputation() { return 2;}
}
