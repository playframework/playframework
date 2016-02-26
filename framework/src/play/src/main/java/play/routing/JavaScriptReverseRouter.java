package play.routing;

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
     * @param routes the reverse routes for this router
     * @return the router
     */
    public static JavaScript create(String name, String ajaxMethod, play.api.routing.JavaScriptReverseRoute... routes) {
        return play.api.routing.JavaScriptReverseRouter.apply(
            name, Scala.Option(ajaxMethod), play.mvc.Http.Context.current().request().host(), Scala.toSeq(routes)
        );
    }

    /**
     * Generates a JavaScript reverse router.
     *
     * @param name the router's name
     * @param routes the reverse routes for this router
     * @return the router
     */
    public static JavaScript create(String name, play.api.routing.JavaScriptReverseRoute... routes) {
        return create(name, "jQuery.ajax", routes);
    }
}
