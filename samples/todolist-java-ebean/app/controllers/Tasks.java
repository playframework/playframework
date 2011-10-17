package controllers;

import play.mvc.*;
import play.data.*;

import java.util.*;

import models.*;
import html.views.*;

@Security.Authenticated(Authentication.Authenticator.class)
public class Tasks extends Controller {
     
    public static Result list() {
        return ok(list.render(Task.find.all()));
    }
    
    public static Result edit(Long id) {
        return ok(form.render(id, form(Task.find.byId(id))));
    }
    
    public static Result create() {
        return ok(form.render(null, form(Task.class)));
    }
    
    public static Result save() {
        Form<Task> taskForm = form(Task.class).bind();
        if(taskForm.hasErrors()) {
            return badRequest(form.render(null, taskForm));
        } else {
            taskForm.get().save();
            return redirect(routes.Tasks.list());
        }
    }
    
    public static Result update(Long id) {
        Form<Task> taskForm = form(Task.class).bind();
        if(taskForm.hasErrors()) {
            return badRequest(form.render(id, taskForm));
        } else {
            taskForm.get().update(id);
            return redirect(routes.Tasks.list());
        }
    }
    
    public static Result delete(Long id) {
        Task.find.ref(id).delete();
        return redirect(routes.Tasks.list());
    }

}
            