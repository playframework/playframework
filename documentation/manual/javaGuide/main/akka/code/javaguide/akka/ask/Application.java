package javaguide.akka.ask;

//#ask
import akka.actor.*;
import play.mvc.*;
import play.libs.Akka;
import play.libs.F.Function;
import static akka.pattern.Patterns.ask;

public class Application extends Controller {

    public static Result index() {
        ActorRef myActor = Akka.system().actorFor("user/my-actor");
        return async(
            Akka.asPromise(ask(myActor, "hello", 1000)).map(
                new Function<Object, Result>() {
                    public Result apply(Object response) {
                        return ok(response.toString());
                    }
                }
            )
        );
    }
}
//#ask
