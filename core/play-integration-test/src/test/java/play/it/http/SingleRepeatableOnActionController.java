/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http;

import play.mvc.Result;
import play.mvc.Results;

import play.it.http.ActionCompositionOrderTest.SomeRepeatable;

public class SingleRepeatableOnActionController extends MockController {

  @SomeRepeatable // runs two actions
<<<<<<< HEAD:core/play-integration-test/src/test/java/play/it/http/SingleRepeatableOnActionController.java
  public Result action() {
=======
  public Result action(Http.Request request) {
>>>>>>> bd30e5f6aa... Java code format for integration tests:core/play-integration-test/src/it/java/play/it/http/SingleRepeatableOnActionController.java
    return Results.ok();
  }
}
