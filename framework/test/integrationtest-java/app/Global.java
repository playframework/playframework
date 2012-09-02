import play.GlobalSettings;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

public class Global extends GlobalSettings {
    @Override
    public Result onHandlerNotFound(Http.RequestHeader requestHeader) {
        // This is here to make sure that the context is set, there is a test that asserts
        // that this is true
        Http.Context.current().session().put("onHandlerNotFound", "true");
        return Results.notFound();
    }
}
