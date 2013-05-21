package controllers;

import play.mvc.SimpleResult;
import play.mvc.Action.Simple;
import play.mvc.Http.Context;
import static play.libs.F.Promise;

public class Interceptor extends Simple {
    
    public static String state = "";
    
    @Override
    public Promise<SimpleResult> call(Context ctx) throws Throwable {
        state = "intercepted";
        return delegate.call(ctx);
    }

}
