package play;

import java.util.*;

import play.libs.Scala;

public class Routes {

    public static String javascriptRouter(String name, play.core.Router.JavascriptReverseRoute... routes) {
        return javascriptRouter(name, "jQuery.ajax", routes);
    }

    public static String javascriptRouter(String name, String ajaxMethod, play.core.Router.JavascriptReverseRoute... routes) {
        return play.api.Routes.javascriptRouter(
            name, Scala.Option(ajaxMethod), Scala.toSeq(routes)
        );
    }

}
