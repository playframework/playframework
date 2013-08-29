package play;

import play.libs.F;
import play.mvc.*;
import play.mvc.Http.*;

import java.io.File;
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
     * Called when an exception occurred.
     * 
     * The default is to send the framework's default error page. This is achieved by returning <code>null</code>, 
     * so that the Scala engine handles the excepetion and shows an error page.
     *
     * By overriding this method one can provide an alternative error page.
     *
     * @param t is any throwable
     * @return null as the default implementation
     */
    public F.Promise<SimpleResult> onError(RequestHeader request, Throwable t) {
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
    @SuppressWarnings("rawtypes")
    public Action onRequest(Request request, Method actionMethod) {
        return new Action.Simple() {
            public F.Promise<SimpleResult> call(Context ctx) throws Throwable {
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
     * Called when no action was found to serve a request.
     *
     * The default behavior is to render the framework's default 404 page. This is achieved by returning <code>null</code>, 
     * so that the Scala engine handles <code>onHandlerNotFound</code>. 
     *
     * By overriding this method one can provide an alternative 404 page.
     *
     * @param request the HTTP request
     * @return null in the default implementation, you can return your own custom Result in your Global class.
     */
    public F.Promise<SimpleResult> onHandlerNotFound(RequestHeader request) {
        return null;
    }
    
    /**
     * Called when an action has been found, but the request parsing has failed.
     *
     * The default behavior is to render the framework's default 400 page. This is achieved by returning <code>null</code>, 
     * so that the Scala engine handles <code>onBadRequest</code>.
     *
     * By overriding this method one can provide an alternative 400 page.
     *
     * @param request the HTTP request
     * @return null in the default implementation, you can return your own custom Result in your Global class.
     */
    public F.Promise<SimpleResult> onBadRequest(RequestHeader request, String error) {
        return null;
    }

    /**
     * Manages controllers instantiation.
     *
     * @param controllerClass the controller class to instantiate.
     * @return the appropriate instance for the given controller class.
     */
    public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
        return null;
    }

    /**
     * Called just after configuration has been loaded, to give the application an opportunity to modify it.
     *
     * @param config the loaded configuration
     * @param path the application path
     * @param classloader The applications classloader
     * @return The configuration that the application should use
     */
    public Configuration onLoadConfig(Configuration config, File path, ClassLoader classloader) {
        return null;
    }

    /**
     * Get the filters that should be used to handle each request.
     */
    public <T extends play.api.mvc.EssentialFilter> Class<T>[] filters() {
        return new Class[0];
    }
    
}
