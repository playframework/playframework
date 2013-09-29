package javaguide.todo.initial.controllrs;

import play.*;
import play.mvc.*;

import javaguide.todo.initial.views.html.*;

//#full-controller-with-todos
//###replace: public class Application extends Controller {
public class ToDoApplication extends Controller {
  
  public static Result index() {
    return ok(index.render("Your new application is ready."));
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