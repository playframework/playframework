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
     * Generates a JavaScript router.
     */
    public static String javascriptRouter(String name, play.core.Router.JavascriptReverseRoute... routes) {
        return javascriptRouter(name, "jQuery.ajax", routes);
    }

    /**
     * Generates a JavaScript router.
     */
    public static String javascriptRouter(String name, String ajaxMethod, play.core.Router.JavascriptReverseRoute... routes) {
        return play.api.Routes.javascriptRouter(
            name, Scala.Option(ajaxMethod), play.mvc.Http.Context.current().request().host(), Scala.toSeq(routes)
        );
    }

}
