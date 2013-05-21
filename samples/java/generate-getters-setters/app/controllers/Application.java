package controllers;

import play.*;
import play.mvc.*;

import views.html.*;

import models.*;

public class Application extends Controller {

	public static Result index() {

		MyModel model = new MyModel();

		model.firstName = "Guillaume";
		model.lastName = "Bort";
		model.age = 30;

		System.out.println(model.firstName);
		System.out.println(model.lastName);
		System.out.println(model.age);

		return ok(index.render(model));
	}
  
}
