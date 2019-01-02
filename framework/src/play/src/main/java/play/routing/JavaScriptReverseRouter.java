/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing;

import play.api.routing.JavaScriptReverseRoute;
import play.libs.Scala;
import play.twirl.api.JavaScript;

/**
 * Helpers for creating JavaScript reverse routers
 */
public class JavaScriptReverseRouter {

    /**
     * Generates a JavaScript reverse router.
     *
     * @param name the router's name
     * @param ajaxMethod which asynchronous call method the user's browser will use (e.g. "jQuery.ajax")
     * @param host the host to use for the reverse route
     * @param routes the reverse routes for this router
     * @return the router
     */
    public static JavaScript create(String name, String ajaxMethod, String host, JavaScriptReverseRoute... routes) {
        return play.api.routing.JavaScriptReverseRouter.apply(
            name, Scala.Option(ajaxMethod), host, Scala.toSeq(routes)
        );
    }

    /**
     * Generates a JavaScript reverse router.
     *
     * @param name the router's name
     * @param ajaxMethod which asynchronous call method the user's browser will use (e.g. "jQuery.ajax")
     * @param routes the reverse routes for this router
     * @return the router
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #create(String, String, String, JavaScriptReverseRoute...)} instead.
     */
    @Deprecated
    public static JavaScript create(String name, String ajaxMethod, JavaScriptReverseRoute... routes) {
        return create(name, ajaxMethod, play.mvc.Http.Context.current().request().host(), routes);
    }
    // TODO:
    // After removing the above create(String, String, JavaScriptReverseRoute...) method we can instead add:
    // public static JavaScript create(String name, String host, JavaScriptReverseRoute... routes) { ... }
    // (Right now they are ambiguous)

    /**
     * Generates a JavaScript reverse router.
     *
     * @param name the router's name
     * @param routes the reverse routes for this router
     * @return the router
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #create(String, String, String, JavaScriptReverseRoute...)} instead.
     */
    @Deprecated
    public static JavaScript create(String name, JavaScriptReverseRoute... routes) {
        return create(name, "jQuery.ajax", routes);
    }
}
