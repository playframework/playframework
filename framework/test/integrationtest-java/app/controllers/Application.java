package controllers;

import play.*;
import play.mvc.*;

import views.html.*;

public class Application extends Controller {
  
    public static Result index(String name) {
        return ok(index.render(name));
    }
    
    public static Result key() {
        return ok("Key=" + Play.application().configuration().getString("key"));
    }
  
}