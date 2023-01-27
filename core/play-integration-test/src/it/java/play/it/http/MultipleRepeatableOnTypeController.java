/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http;

import play.it.http.ActionCompositionOrderTest.SomeRepeatable;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

@SomeRepeatable // runs two actions
@SomeRepeatable // once more, so makes it four
public class MultipleRepeatableOnTypeController extends MockController {

  public Result action(Http.Request request) {
    return Results.ok();
  }
}
