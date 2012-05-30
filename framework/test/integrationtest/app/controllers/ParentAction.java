package controllers;

import play.*;
import play.mvc.*;

import views.html.*;

public abstract class ParentAction extends Controller {
  
	public Result index() {
		return ok("I'm the parent action");
	}
  
}
