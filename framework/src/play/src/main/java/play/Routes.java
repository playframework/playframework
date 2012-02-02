package play;

import play.libs.Scala;

/**
 * Helper utilities related to `Router`.
 */
public class Routes {

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
            name, Scala.Option(ajaxMethod), Scala.toSeq(routes)
        );
    }

}
