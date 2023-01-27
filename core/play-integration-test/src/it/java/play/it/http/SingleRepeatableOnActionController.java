/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http;

import play.it.http.ActionCompositionOrderTest.SomeRepeatable;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

public class SingleRepeatableOnActionController extends MockController {

  @SomeRepeatable // runs two actions
  public Result action(Http.Request request) {
    return Results.ok();
  }
}
