/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http;

import play.mvc.Result;
import play.mvc.Results;
import play.mvc.With;

import play.it.http.ActionCompositionOrderTest.FirstAction;
import play.it.http.ActionCompositionOrderTest.SecondAction;

@With({FirstAction.class, SecondAction.class})
public class WithOnTypeAndActionController extends MockController {

  @With({FirstAction.class, SecondAction.class})
<<<<<<< HEAD:core/play-integration-test/src/test/java/play/it/http/WithOnTypeAndActionController.java
  public Result action() {
=======
  public Result action(Http.Request request) {
>>>>>>> bd30e5f6aa... Java code format for integration tests:core/play-integration-test/src/it/java/play/it/http/WithOnTypeAndActionController.java
    return Results.ok();
  }
}
