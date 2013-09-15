package controllers;

import play.*;
import play.mvc.*;

import views.html.*;

//#full-controller-with-todos
//###replace: public class Application extends Controller {
public class ToDoApplication extends Controller {
  
  public static Result index() {
    //###replace:     return ok(index.render("Your new application is ready."));
    return ok(todo.render("Your new application is ready."));
  }
  
  public static Result tasks() {
    return TODO;
  }
  
  public static Result newTask() {
    return TODO;
  }
  
  public static Result deleteTask(Long id) {
    return TODO;
  }
  
}
//#full-controller-with-todos