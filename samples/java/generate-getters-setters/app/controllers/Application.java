package controllers;

import play.*;
import play.mvc.*;

import views.html.*;

import models.*;

public class Application extends Controller {
  
    public static Result index() {

    	MyModel myModel = new MyModel();

    	myModel.gender = "female";
    	myModel.age = 35;

        return ok(index.render(myModel));
    }
  
}
