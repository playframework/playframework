package javaguide.di.guice.classfield;

//#class-field-dependency-injection
import com.google.inject.Singleton;
import play.mvc.Result;

@Singleton
public class SubclassController extends BaseController {
    public Result index() {
        return someBaseAction("index");
    }
}
//#class-field-dependency-injection