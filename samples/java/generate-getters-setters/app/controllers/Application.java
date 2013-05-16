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
    	myModel.lifeStory = "Well when I was a kid, I always wanted to be a model, so I went to the gym every day yada yada yada";

        return ok(index.render(myModel));
    }
  
}
