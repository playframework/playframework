package play;

import play.libs.Scala;

/**
 * Helper utilities related to `Router`.
 */
public class Routes {

    public final static String ROUTE_VERB = "ROUTE_VERB";
    public final static String ROUTE_PATTERN = "ROUTE_PATTERN";
    public final static String ROUTE_CONTROLLER = "ROUTE_CONTROLLER";
    public final static String ROUTE_ACTION_METHOD = "ROUTE_ACTION_METHOD";

    /**
     * Generates a JavaScript reverse router with no built-in ajax support.
     * @param name Name of the generated JavaScript object
     * @param routes Routes to include in the JavaScript reverse router
     */
    public static String javascriptRouter(String name, play.core.Router.JavascriptReverseRoute... routes) {
        return javascriptRouter(name, false, routes);
    }
    
    /**
     * Generates a JavaScript reverse router.
     * @param name Name of the generated JavaScript object
     * @param ajaxMethod Set it to true if you want to generate an ajax method to each route
     * @param routes Routes to include in the JavaScript reverse router
     */
    public static String javascriptRouter(String name, Boolean ajaxMethod, play.core.Router.JavascriptReverseRoute... routes) {
        return play.api.Routes.javascriptRouter(name, ajaxMethod, play.mvc.Http.Context.current().request().host(), Scala.toSeq(routes));
    }
    
    /**
     * Generates a JavaScript reverse router.
     * @param name Name of the generated JavaScript object
     * @param ajaxMethod JavaScript function that will be called by the ajax method of routes objects
     * @param routes Routes to include in the JavaScript reverse router
     */
    public static String javascriptRouter(String name, String ajaxMethod, play.core.Router.JavascriptReverseRoute... routes) {
        return play.api.Routes.javascriptRouter(
            name, Scala.Option(ajaxMethod), play.mvc.Http.Context.current().request().host(), Scala.toSeq(routes)
        );
    }

}
