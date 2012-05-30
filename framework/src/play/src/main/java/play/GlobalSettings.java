package play;

import play.mvc.*;
import play.mvc.Http.*;

import java.lang.reflect.*;

/**
 * GlobalSettings is instantiated by the framework when an application starts, to let you perform specific tasks
 * at start-up or shut-down.
 * <p>
 * How to use it: create a <code>Global.java</code> class in your Java application and override the methods you want.
 */
public class GlobalSettings {
    
    /**
     * Executed before any plugin - you can set-up your database schema here, for instance.
     */
    public void beforeStart(Application app) {
    }

    /**
     * Executed after all plugins, including the database set-up with Evolutions and the EBean wrapper.
     * This is a good place to execute some of your application code to create entries, for instance.
     */
    public void onStart(Application app) {
    }

    /**
     * Executed when the application stops.
     */
    public void onStop(Application app) {
    }

    /**
     * Returns a Result that could be a custom error page.
     * The default implementation returns <code>null</code>, so that the Scala engine handles the excepetion and show an error page.
     *
     * @param t is any throwable
     * @return null as the default implementation
     */
    public Result onError(RequestHeader request, Throwable t) {
        return null;
    }
    
    /**
     * Call to create the root Action of a request for a Java application.
     * The request and actionMethod values are passed for information.
     *
     * @param request The HTTP Request
     * @param actionMethod The action method containing the user code for this Action.
     * @return The default implementation returns a raw Action calling the method.
     */
    public Action onRequest(Request request, Method actionMethod) {
        return new Action.Simple() {
            public Result call(Context ctx) throws Throwable {
                return delegate.call(ctx);
            }
        };
    }

    /**
    *
    * Called when an HTTP request has been received.
    * The default implementation (return null) means to use the application router to find the appropriate action 
    * 
    * By overriding this method one can provide an alternative routing mechanism. 
    * Please note, though, this API is very low level, useful for plugin/module authors only.
    * 
    * @param request the HTTP request header as seen by the core framework (the body has not been parsed yet)
    * @return an action to handle this request - if no action is returned, a 404 not found result will be sent to client
    */
    public play.api.mvc.Handler onRouteRequest(RequestHeader request) {
        return null;
    }

    /**
     * Triggered when a resource was requested but not found. The default implementation returns <code>null</code>, so that
     * the Scala engine handles the <code>onActionNotFound</code>.
     *
     * @param request the HTTP request
     * @return null in the default implementation, you can return your own custom Result in your Global class.
     */
    public Result onHandlerNotFound(RequestHeader request) {
        return null;
    }
    
    /**
     * Triggered when a resource was requested but not found, the default implementation returns <code>null</code>, so that
     * the Scala engine handles the <code>onBadRequest</code>.
     *
     * @param request the HTTP request
     * @return null in the default implementation, you can return your own custom Result in your Global class.
     */
    public Result onBadRequest(RequestHeader request, String error) {
        return null;
    }
}
