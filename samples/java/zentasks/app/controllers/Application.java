package controllers;

import play.*;
import play.mvc.*;
import play.data.*;
import static play.data.Form.*;

import models.*;
import views.html.*;

public class Application extends Controller {
  
    // -- Authentication
    
    public static class Login {
        
        public String email;
        public String password;
        
        public String validate() {
            if(User.authenticate(email, password) == null) {
                return "Invalid user or password";
            }
            return null;
        }
        
    }

    /**
     * Login page.
     */
    public static Result login() {
        return ok(
            login.render(form(Login.class))
        );
    }
    
    /**
     * Handle login form submission.
     */
    public static Result authenticate() {
        Form<Login> loginForm = form(Login.class).bindFromRequest();
        if(loginForm.hasErrors()) {
            return badRequest(login.render(loginForm));
        } else {
            session("email", loginForm.get().email);
            return redirect(
                routes.Projects.index()
            );
        }
    }

    /**
     * Logout and clean the session.
     */
    public static Result logout() {
        session().clear();
        flash("success", "You've been logged out");
        return redirect(
            routes.Application.login()
        );
    }
  
    // -- Javascript routing
    
    public static Result javascriptRoutes() {
        response().setContentType("text/javascript");
        return ok(
            Routes.javascriptRouter("jsRoutes",
            
                // Routes for Projects
                controllers.routes.javascript.Projects.add(), 
                controllers.routes.javascript.Projects.delete(), 
                controllers.routes.javascript.Projects.rename(),
                controllers.routes.javascript.Projects.addGroup(), 
                controllers.routes.javascript.Projects.deleteGroup(), 
                controllers.routes.javascript.Projects.renameGroup(),
                controllers.routes.javascript.Projects.addUser(), 
                controllers.routes.javascript.Projects.removeUser(), 
                
                // Routes for Tasks
                controllers.routes.javascript.Tasks.addFolder(), 
                controllers.routes.javascript.Tasks.renameFolder(), 
                controllers.routes.javascript.Tasks.deleteFolder(), 
                controllers.routes.javascript.Tasks.index(),
                controllers.routes.javascript.Tasks.add(), 
                controllers.routes.javascript.Tasks.update(), 
                controllers.routes.javascript.Tasks.delete()
                
            )
        );
    }

}
