package play;

import java.util.*;

import play.libs.Scala;

public class Routes {
    
    public static String javascriptRouter(String name, play.core.Router.JavascriptReverseRoute... routes) {
        return play.api.Routes.javascriptRouter(
            name, Scala.toSeq(routes)
        );
    }
    
}