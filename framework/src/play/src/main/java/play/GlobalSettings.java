/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play;

import play.mvc.*;
import play.mvc.Http.*;

import java.lang.reflect.*;
import java.util.concurrent.CompletionStage;

/**
 * GlobalSettings is instantiated by the framework when an application starts, to let you perform specific tasks
 * at start-up or shut-down.
 * <p>
 * How to use it: create a <code>Global.java</code> class in your Java application and override the methods you want.
 *
 * @deprecated This class is deprecated in 2.5.0.  Please use dependency injection in preference to GlobalSettings.
 */
@Deprecated
public class GlobalSettings {

    /**
     * Executed before any plugin - you can set-up your database schema here, for instance.
     *
     * @param app the bootstrapping application
     */
    public void beforeStart(Application app) {
    }

    /**
     * Executed after all plugins, including the database set-up with Evolutions and the EBean wrapper.
     * This is a good place to execute some of your application code to create entries, for instance.
     *
     * @param app the bootstrapped application
     */
    public void onStart(Application app) {
    }

    /**
     * Executed when the application stops.
     *
     * @param app the application that is shutting down
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
     * @param request header of the HTTP request being processed
     * @param t is any throwable
     * @return null as the default implementation
     */
    public CompletionStage<Result> onError(RequestHeader request, Throwable t) {
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
            public CompletionStage<Result> call(Context ctx) {
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
    public CompletionStage<Result> onHandlerNotFound(RequestHeader request) {
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
     * @param error an arbitrary string describing the error
     * @return null in the default implementation, you can return your own custom Result in your Global class.
     */
    public CompletionStage<Result> onBadRequest(RequestHeader request, String error) {
        return null;
    }

    /**
     * Get the filters that should be used to handle each request.
     *
     * @param <T> the filter type
     * @return an instance of the filter type
     */
    public <T extends play.api.mvc.EssentialFilter> Class<T>[] filters() {
        return new Class[0];
    }

}
