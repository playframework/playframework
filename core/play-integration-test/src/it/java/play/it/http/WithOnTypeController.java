/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http;

import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.With;

import play.it.http.ActionCompositionOrderTest.FirstAction;
import play.it.http.ActionCompositionOrderTest.SecondAction;

@With({FirstAction.class, SecondAction.class})
public class WithOnTypeController extends MockController {

  public Result action(Http.Request request) {
    return Results.ok();
  }
}
