//#controller-application
package controllers;

import play.*;
import play.mvc.*;

import views.html.*;

public class Application extends Controller {
  
    public static Result index() {
        //###replace: 	return ok(index.render("Your new application is ready."));
        return ok(todo.render("Your new application is ready."));
    }
  
}
//#controller-application
