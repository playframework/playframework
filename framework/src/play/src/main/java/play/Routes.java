/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play;

import play.routing.Router;
import play.twirl.api.JavaScript;

/**
 * Helper utilities related to `Router`.
 *
 * @deprecated as of 2.5. This class will be removed in future versions of Play.
 */
@Deprecated
public class Routes {

    /**
     * @deprecated Use play.routing.Router.Tags.ROUTE_VERB
     */
    @Deprecated
    public final static String ROUTE_VERB = Router.Tags.ROUTE_VERB;

    /**
     * @deprecated Use play.routing.Router.Tags.ROUTE_PATTERN
     */
    @Deprecated
    public final static String ROUTE_PATTERN = Router.Tags.ROUTE_PATTERN;

    /**
     * @deprecated Use play.routing.Router.Tags.ROUTE_CONTROLLER
     */
    @Deprecated
    public final static String ROUTE_CONTROLLER = Router.Tags.ROUTE_CONTROLLER;

    /**
     * @deprecated Use play.routing.Router.Tags.ROUTE_ACTION_METHOD
     */
    @Deprecated
    public final static String ROUTE_ACTION_METHOD = Router.Tags.ROUTE_ACTION_METHOD;

    /**
     * Generates a JavaScript router.
     *
     * @deprecated Use play.routing.JavaScriptReverseRouter.create
     *
     * @param name the router's name
     * @param routes the reverse routes for this router
     * @return the router
     */
    @Deprecated
    public static JavaScript javascriptRouter(String name, play.api.routing.JavaScriptReverseRoute... routes) {
        return javascriptRouter(name, "jQuery.ajax", routes);
    }

    /**
     * Generates a JavaScript router.
     *
     * @deprecated Use play.routing.JavaScriptReverseRouter.create
     *
     * @param name the router's name
     * @param ajaxMethod which asynchronous call method the user's browser will use (e.g. "jQuery.ajax")
     * @param routes the reverse routes for this router
     * @return the router
     */
    @Deprecated
    public static JavaScript javascriptRouter(String name, String ajaxMethod, play.api.routing.JavaScriptReverseRoute... routes) {
        return play.routing.JavaScriptReverseRouter.create(name, ajaxMethod, routes);
    }
}
