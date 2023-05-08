/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.http;

import jakarta.inject.Inject;
import java.util.LinkedHashMap;

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
