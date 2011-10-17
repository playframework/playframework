package controllers;

import play.mvc.*;
import play.data.*;

import html.views.*;

import play.data.format.*;
import play.data.validation.*;

public class Authentication extends Controller {
    
    public static class Authenticator extends Security.Authenticator {
        
        public Result onUnauthorized(Http.Context ctx) {
            return redirect(routes.Authentication.login());
        }
        
    }
    
    public static class Login {
        
        @Constraints.Required
        public String username;
        public String password;
        
        public String validate() {
            if(username.equals(password)) {
                return null;
            } else {
                return "Invalid user or password";
            }
        }
        
    }
    
    public static Result login() {
        return unauthorized(login.render(form(Login.class)));
    }
    
    public static Result authenticate() {
        Form<Login> loginForm = form(Login.class).bind();
        if(loginForm.hasErrors()) {
            return badRequest(login.render(loginForm));
        } else {
            session().put("username", loginForm.get().username);
            return redirect(routes.Tasks.list());
        }
    }
    
    public static Result logout() {
        session().clear();
        return redirect(routes.Authentication.login());
    }
    
    
}