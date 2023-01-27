/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.http;

import java.util.LinkedHashMap;
import javax.inject.Inject;

/**
 * An HttpErrorHandler that uses either HTML or JSON in the response depending on the client's
 * preference.
 */
public class HtmlOrJsonHttpErrorHandler extends PreferredMediaTypeHttpErrorHandler {

  private static LinkedHashMap<String, HttpErrorHandler> buildMap(
      DefaultHttpErrorHandler htmlHandler, JsonHttpErrorHandler jsonHandler) {
    LinkedHashMap<String, HttpErrorHandler> map = new LinkedHashMap<>();
    map.put("text/html", htmlHandler);
    map.put("application/json", jsonHandler);
    return map;
  }

  @Inject
  public HtmlOrJsonHttpErrorHandler(
      DefaultHttpErrorHandler htmlHandler, JsonHttpErrorHandler jsonHandler) {
    super(buildMap(htmlHandler, jsonHandler));
  }
}
