package controllers;

import play.mvc.*;
import play.data.*;

import java.util.*;

import models.*;
import html.views.*;

public class Application extends Controller {
    
    static List<User> users = new ArrayList<User>();

    public static Result index() {
        return ok(index.render());
    }
    
    public static Result list() {
        return ok(list.render(users));
    }
    
    public static Result add() {
        Form<User> form = form(User.class).bind();
        if(form.hasErrors()) {
            return badRequest();
        }
        users.add(form.get());
        return ok();
    }
    
    public static Result delete(String name) {
        List<User> newList = new ArrayList<User>();
        for(User user:users) {
            if(!user.name.equals(name)) {
                newList.add(user);
            }
        }
        users = newList;
        return ok();
    }

}
            