import play.GlobalSettings;
import play.mvc.Http;
import play.mvc.SimpleResult;
import play.mvc.Results;
import play.libs.F;

public class Global extends GlobalSettings {
    @Override
    public F.Promise<SimpleResult> onHandlerNotFound(Http.RequestHeader requestHeader) {
        // This is here to make sure that the context is set, there is a test that asserts
        // that this is true
        Http.Context.current().session().put("onHandlerNotFound", "true");
        return F.Promise.<SimpleResult>pure(Results.notFound());
    }
}
