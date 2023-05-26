package javaguide.di.guice.classfield;

//#class-field-dependency-injection
import com.google.inject.Inject;
import play.mvc.Controller;
import play.mvc.Result;

public class BaseController extends Controller {
    // LiveCounter will be injected
    @Inject
    volatile protected Counter counter = new NoopCounter();

    public Result someBaseAction(String source) {
        counter.inc(source);
        return ok(source);
    }
}
//#class-field-dependency-injection