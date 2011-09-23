package controllers;

import play.mvc.*;
import play.data.*;
import play.api.templates.*;
import play.libs.F.*;
import static play.libs.F.*;

import views.html.*;
import views.pages.html.*;

import controllers.Security.*;
import controllers.Cache.*;

import java.util.*;

import com.avaje.ebean.*;

public class JavaApplication extends Controller {

    @Cached
    public static Result index() throws Exception {                
        System.out.println("REQUEST -> " + request());
        String url = routes.JavaApplication.hello(5, "World").url();
        return ok(javaIndex.render(url)); 
    }

    @With(LoggingAction.class)
    public static Result hello(Integer repeat, String name) {
        return ok(hello.render(name, repeat));
    }
    
    public static Result back() {
        return redirect(routes.JavaApplication.hello(30, "Redirected"));
    }
    
    static Map<String,Template2<String,String,Html>> pageTemplates = new HashMap<String,Template2<String,String,Html>>();
    static {
        pageTemplates.put("home", home.ref());
        pageTemplates.put("about", about.ref());
    }
    
    public static Result page(String name) {
        Template2<String,String,Html> template = pageTemplates.get(name);
        if(template == null) {
            template = page.ref();
        }
        return ok(template.render(name, "Dummy content"));
    }
    
    // ~~~
    
    public static class LoggingAction extends Action.Simple {

        public Result call(Http.Context ctx) {
            System.out.println("Calling action for " + ctx);
            return deleguate.call(ctx);
        }

    }
    
}

