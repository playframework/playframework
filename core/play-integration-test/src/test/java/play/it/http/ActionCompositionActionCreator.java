/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import play.http.ActionCreator;
import play.mvc.*;
import play.test.Helpers;

public class ActionCompositionActionCreator implements ActionCreator {

  @Override
  public Action createAction(Http.Request request, Method actionMethod) {
    return new Action.Simple() {
      @Override
      public CompletionStage<Result> call(Http.Request req) {
        return delegate
            .call(req)
            .thenApply(
                result -> {
                  String newContent = "actioncreator" + Helpers.contentAsString(result);
                  return Results.ok(newContent);
                });
      }
    };
  }
}
