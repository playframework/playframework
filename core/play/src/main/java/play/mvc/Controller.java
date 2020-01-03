/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static play.mvc.Http.*;

/** Superclass for a Java-based controller. */
public abstract class Controller extends Results implements Status, HeaderNames {

  /** Generates a 501 NOT_IMPLEMENTED simple result. */
  public static Result TODO(Request request) {
    return status(NOT_IMPLEMENTED, views.html.defaultpages.todo.render(request.asScala()));
  }
}
