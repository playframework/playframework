/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.http;

import javax.inject.Inject;
import java.util.LinkedHashMap;

/**
 * An HttpErrorHandler that uses either HTML or JSON in the response depending on the client's preference.
 */
public class HtmlOrJsonHttpErrorHandler extends PreferredMediaTypeHttpErrorHandler {

  private static LinkedHashMap<String, HttpErrorHandler> buildMap(
      DefaultHttpErrorHandler htmlHandler, JsonHttpErrorHandler jsonHandler
  ) {
    LinkedHashMap<String, HttpErrorHandler> map = new LinkedHashMap<>();
    map.put("text/html", htmlHandler);
    map.put("application/json", jsonHandler);
    return map;
  }

  @Inject
  public HtmlOrJsonHttpErrorHandler(DefaultHttpErrorHandler htmlHandler, JsonHttpErrorHandler jsonHandler) {
    super(buildMap(htmlHandler, jsonHandler));
  }

}
