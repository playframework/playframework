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

    	System.out.println(myModel.gender);
    	System.out.println(myModel.age);
    	System.out.println(myModel.getAge());
    	System.out.println(myModel.lifeStory);

        return ok(index.render(myModel));
    }
  
}
