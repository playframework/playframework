//#controller-application
//###replace: package controllers;
package javaguide.todo.initial.controllers;

import play.*;
import play.mvc.*;

//###replace: views.html.*;
import javaguide.todo.initial.views.html.*;

public class Application extends Controller {
  
    public static Result index() {
        return ok(index.render("Your new application is ready."));
    }
  
}
//#controller-application
