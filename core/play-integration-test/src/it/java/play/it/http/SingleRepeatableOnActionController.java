/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http;

import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import play.it.http.ActionCompositionOrderTest.SomeRepeatable;

public class SingleRepeatableOnActionController extends MockController {

  @SomeRepeatable // runs two actions
  public Result action(Http.Request request) {
    return Results.ok();
  }
}
