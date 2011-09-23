package controllers;

import play.mvc.*;
import play.data.*;

import views.html.*;

import java.util.*;

import models.*;

import com.avaje.ebean.*;

public class Tasks extends Controller {
     
    static Call HOME = routes.Tasks.list();

    public static Result list() {
        List<Task> tasks = Ebean.find(Task.class).findList();
        return ok(list.render(tasks));
    }
    
    public static Result edit(Long id) {
        Task task = Ebean.find(Task.class, id);
        return ok(form.render(id, blank(task)));
    }
    
    public static Result create() {
        return ok(form.render(null, blank(Task.class)));
    }
    
    public static Result save() {
        Form<Task> taskForm = form(Task.class);
        if(taskForm.hasErrors()) {
            return badRequest(form.render(null, taskForm));
        } else {
            Ebean.save(taskForm.get());
            return redirect(HOME);
        }
    }
    
    public static Result update(Long id) {
        Form<Task> taskForm = form(Ebean.find(Task.class, id));
        if(taskForm.hasErrors()) {
            return badRequest(form.render(id, taskForm));
        } else {
            Ebean.update(taskForm.get());
            return redirect(HOME);
        }
    }
    
    public static Result delete(Long id) {
        Ebean.delete(Ebean.getReference(Task.class, id));
        return redirect(HOME);
    }

}
            