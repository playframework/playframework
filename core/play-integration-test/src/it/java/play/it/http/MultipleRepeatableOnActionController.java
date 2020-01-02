/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http;

import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import play.it.http.ActionCompositionOrderTest.SomeRepeatable;

public class MultipleRepeatableOnActionController extends MockController {

  @SomeRepeatable // runs two actions
  @SomeRepeatable // plus two more
  public Result action(Http.Request request) {
    return Results.ok();
  }
}
