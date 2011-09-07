package controllers;

import play.mvc.*;

import views.html.*;

import controllers.Security.*;
import controllers.Cache.*;

import models.*;

public class JavaApplication extends Controller {

    @Cached
    public static Result index() {
        System.out.println("REQUEST -> " + request());
        String url = routes.JavaApplication().hello(5, "World").url();
        return Html(javaIndex.apply(url));
    }

    @With(LoggingAction.class)
    public static Result hello(Integer repeat, String name) {
        return Html(hello.apply(name, repeat)); 
    }
    
    public static Result back() {
        return Redirect(routes.JavaApplication().hello(30, "Redirected"));
    }
    
    // ~~~
    
    public static class LoggingAction extends Action.Simple {

        public Result call(Http.Context ctx) {
            System.out.println("Calling action for " + ctx);
            return deleguate.call(ctx);
        }

    }
    
}

