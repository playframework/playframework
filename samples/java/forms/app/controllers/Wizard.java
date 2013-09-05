package controllers;

import play.mvc.*;
import play.data.*;
import static play.data.Form.*;

import views.html.wizard.*;

import models.*;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class Wizard extends Controller {
    
	/**
	 * Read a map and convert it to an User object
	 */
	private static User toUser(Map<String, String> session) {
		 // Fill the form with data session
		User user = new User();
		user.username = session.get("username");
		user.email = session.get("email");
		user.password = session.get("password");
		user.profile = new User.Profile();
		user.profile.address = session.get("profile.address");
		user.profile.country = session.get("profile.country");

		String age = session.get("profile.age");
		if (StringUtils.isNotEmpty(age)) {
			user.profile.age = Integer.valueOf(age);
		}
		
		return user;
	}
	
	/**
	 * Serialize our form data to a Map
 	 */
	private static Map<String, String> toMap(Form<?> user) {
		 // Fill the form with data session
		Map<String, String> map = new HashMap<String, String>();
		map.put("username", user.field("username").valueOr(""));
		map.put("email", user.field("email").valueOr(""));
		map.put("password", user.field("password").valueOr(""));
		map.put("profile.address", user.field("profile.address").valueOr(""));
		map.put("profile.country", user.field("profile.country").valueOr(""));
		map.put("profile.age", user.field("profile.age").valueOr(""));
	
		return map ;
	}

    /**
     * Navigate between states. We saved the current user into the session. 
     * This is of course only possible because we have little data.
     */ 
    public static Result step(int step) {
	
	   	User user = toUser(session());
		Form<User> signupForm = form(User.class).fill(user);
		if (step == 1) {
        	return ok(form1.render(signupForm));
		} else {
			return ok(form2.render(signupForm));
		}
    }

	/**
	 * Submit each data part from the registration wizard
	 */
	public static Result submit(int step) {
	  	
		if (step == 1) {
			return handleStep1Submission();
		} else {
			return handleStep2Submission();
		}
    }
  
    private static Result handleStep1Submission() {
		// We restrict the validation to the Step1 "group"
		Form<User> filledForm = form(User.class, User.Step1.class).bindFromRequest();
	
		// Check repeated password
        if(!filledForm.field("password").valueOr("").isEmpty()) {
            if(!filledForm.field("password").valueOr("").equals(filledForm.field("repeatPassword").value())) {
                filledForm.reject("repeatPassword", "Password don't match");
            }
        }

        // Check if the username is valid
        if(!filledForm.hasErrors()) {
            if(filledForm.get().username.equals("admin") || filledForm.get().username.equals("guest")) {
                filledForm.reject("username", "This username is already taken");
            }
        }
		if(filledForm.hasErrors()) {
	        return badRequest(form1.render(filledForm));
        } else {
	        // We saved our info in the session
			session().putAll(toMap(filledForm));
			return redirect(routes.Wizard.step(2));
	   }
	}
	
	private static Result handleStep2Submission() {
		// We restrict the validation to the Step2 "group"
		Form<User> filledForm = form(User.class, User.Step2.class).bindFromRequest();
	
		// If previous go back but first save data
		if ("Previous".equals(filledForm.field("action").value())) {
	        // We saved our info in the session
			session().putAll(toMap(filledForm));
            return redirect(routes.Wizard.step(1));
		}
		
		// Check accept conditions
		if(!"true".equals(filledForm.field("accept").value())) {
            filledForm.reject("accept", "You must accept the terms and conditions");
        }	

		if(filledForm.hasErrors()) {
		    return badRequest(form2.render(filledForm));
		} else {
			User created = filledForm.get();
			// Clear the session
			session().clear();
		    return ok(summary.render(created));
		}
    }
}
