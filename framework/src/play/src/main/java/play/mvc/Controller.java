package play.mvc;

import play.api.*;

import play.mvc.Http.*;
import play.mvc.Result.*;

import play.data.*;

import java.util.*;

/**
 * Superclass for a Java-based controller.
 */
public abstract class Controller extends Results implements Status, HeaderNames {
    
    /**
     * Returns the current HTTP context.
     */
    public static Context ctx() {
        return Http.Context.current();
    }
    
    /**
     * Returns the current HTTP request.
     */
    public static Request request() {
        return Http.Context.current().request();
    }
    
    /**
     * Returns the current lang.
     */
    public static play.i18n.Lang lang() {
        return play.i18n.Lang.preferred(Context.current().request().acceptLanguages());
    }
    
    /**
     * Returns the current HTTP response.
     */
    public static Response response() {
        return Http.Context.current().response();
    }
    
    /**
     * Returns the current HTTP session.
     */
    public static Session session() {
        return Http.Context.current().session();
    }
    
    /**
     * Puts a new value into the current session.
     */
    public static void session(String key, String value) {
        session().put(key, value);
    }
    
    /**
     * Returns a vlue from the session.
     */     
    public static String session(String key) {
        return session().get(key);
    }
    
    /**
     * Returns the current HTTP flash scope.
     */
    public static Flash flash() {
        return Http.Context.current().flash();
    }
    
    /**
     * Puts a new value into the flash scope.
     */
    public static void flash(String key, String value) {
        flash().put(key, value);
    }
    
    /**
     * Returns a value from the flash scope.
     */
    public static String flash(String key) {
        return flash().get(key);
    }
    
    // -- Form
    
    /**
     * Instantiates a dynamic form.
     */
    public static DynamicForm form() {
        return new DynamicForm();
    }
    
    /**
     * Instantiates a new form that wraps the specified class.
     */
    public static <T> Form<T> form(Class<T> clazz) {
        return new Form(clazz);
    }
    
    /**
     * Instantiates a new form that wraps the specified class.
     */
    public static <T> Form<T> form(String name, Class<T> clazz) {
        return new Form(name, clazz);
    }
    
}