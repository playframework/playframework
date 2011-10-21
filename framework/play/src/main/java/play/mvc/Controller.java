package play.mvc;

import play.api.*;
import play.api.mvc.Content;

import play.mvc.Http.*;
import play.mvc.Result.*;

import play.data.*;

import java.util.*;

/**
 * Super class for a Play Java based controller.
 */
public abstract class Controller extends Results {
    
    /**
     * Get the current HTTP context.
     */
    public static Context ctx() {
        return Http.Context.current();
    }
    
    /**
     * Get the current HTTP request.
     */
    public static Request request() {
        return Http.Context.current().request();
    }
    
    /**
     * Get the current HTTP response.
     */
    public static Response response() {
        return Http.Context.current().response();
    }
    
    /**
     * Get the current HTTP session.
     */
    public static Session session() {
        return Http.Context.current().session();
    }
    
    /**
     * Put a new value into the current session.
     */
    public static void session(String key, String value) {
        session().put(key, value);
    }
    
    /**
     * Retrieve a vlue from the session.
     */     
    public static String session(String key) {
        return session().get(key);
    }
    
    /**
     * Get the current HTTP flash scope.
     */
    public static Flash flash() {
        return Http.Context.current().flash();
    }
    
    /**
     * Put a new value into the flash scope.
     */
    public static void flash(String key, String value) {
        flash().put(key, value);
    }
    
    /**
     * Retrieve a vlue from the flash scope.
     */
    public static String flash(String key) {
        return flash().get(key);
    }
    
    // -- Form
    
    /**
     * Instantiate a dynamic form.
     */
    public static DynamicForm form() {
        return new DynamicForm();
    }
    
    /**
     * Instantiate a new form wrapping the specified class.
     */
    public static <T> Form<T> form(Class<T> clazz) {
        return new Form(clazz);
    }
    
}