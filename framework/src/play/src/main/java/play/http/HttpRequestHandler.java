/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.http;

import play.mvc.Action;
import play.mvc.Http.*;

import java.lang.reflect.Method;

/**
 * An HTTP request handler
 */
public interface HttpRequestHandler {

    /**
     * Call to create the root Action of a request for a Java application.
     *
     * The request and actionMethod values are passed for information.  Implementations of this method should create
     * an instance of Action that invokes the injected action delegate.
     *
     * @param request The HTTP Request
     * @param actionMethod The action method containing the user code for this Action.
     * @return The default implementation returns a raw Action calling the method.
     */
    Action createAction(Request request, Method actionMethod);

    /**
     * Call to wrap the outer action of a Java application.
     *
     * This method is passed a fully composed action, allowing a last final global interceptor to be added to the
     * action if required.
     *
     * @param action The action to wrap.
     * @return A wrapped action.
     */
    Action wrapAction(Action action);
}
