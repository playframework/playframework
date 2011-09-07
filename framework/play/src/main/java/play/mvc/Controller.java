package play.mvc;

import play.mvc.Http.*;
import play.mvc.Result.*;

public abstract class Controller {
    
    public static Request request() {
        return Http.Context.current().request();
    }
    
    public static Result Text(Object any, String... args) {
        final String text;
        if(any == null) {
            text = "";
        } else {
            text = any.toString();
        }
        final String formatted = String.format(text, (Object)args);
        return new Text(formatted);
    }
    
    public static Result Html(Object any) {
        final String html;
        if(any == null) {
            html = "";
        } else {
            html = any.toString();
        }
        return new Html(html);
    }
    
    public static Result Redirect(String url) {
        return new Redirect(url);
    }
    
    public static Result Redirect(play.api.mvc.Call call) {
        if(!call.method().equals("GET")) {
            throw new RuntimeException("Cannot issue a redirect for a " + call.method() + " method.");
        }
        return new Redirect(call.url());
    }
    
}