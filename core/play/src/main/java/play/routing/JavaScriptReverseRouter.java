/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing;

import play.api.routing.JavaScriptReverseRoute;
import play.libs.Scala;
import play.twirl.api.JavaScript;

/** Helpers for creating JavaScript reverse routers */
public class JavaScriptReverseRouter {

  /**
   * Generates a JavaScript reverse router.
   *
   * @param name the router's name
   * @param ajaxMethod which asynchronous call method the user's browser will use (e.g.
   *     "jQuery.ajax")
   * @param host the host to use for the reverse route
   * @param routes the reverse routes for this router
   * @return the router
   */
  public static JavaScript create(
      String name, String ajaxMethod, String host, JavaScriptReverseRoute... routes) {
    return play.api.routing.JavaScriptReverseRouter.apply(
        name, Scala.Option(ajaxMethod), host, Scala.varargs(routes));
  }
}
