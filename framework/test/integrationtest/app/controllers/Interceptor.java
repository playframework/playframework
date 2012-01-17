package controllers;

import play.mvc.Result;
import play.mvc.Action.Simple;
import play.mvc.Http.Context;

public class Interceptor extends Simple {
    
    public static String state = "";
    
    @Override
    public Result call(Context ctx) throws Throwable {
        state = "intercepted";
        return delegate.call(ctx);
    }

}
