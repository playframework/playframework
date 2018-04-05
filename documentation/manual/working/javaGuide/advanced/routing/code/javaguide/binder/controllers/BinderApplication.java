/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.binder.controllers;

import javaguide.binder.models.User;
import javaguide.binder.models.AgeRange;
import play.mvc.BaseController;
import play.mvc.Result;

public class BinderApplication extends BaseController {

	//#path
    public Result user(User user){
    	return ok(user.name);
    }    
	//#path
    
	//#query
    public Result age(AgeRange ageRange){
    	return ok(String.valueOf(ageRange.from));
    }    
	//#query    
}

