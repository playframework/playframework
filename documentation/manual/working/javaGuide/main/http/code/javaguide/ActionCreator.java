/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

// #default
import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

public class ActionCreator implements play.http.ActionCreator {
  @Override
  public Action createAction(Http.Request request, Method actionMethod) {
    return new Action.Simple() {
      @Override
      public CompletionStage<Result> call(Http.Request req) {
        return delegate.call(req);
      }
    };
  }
}
// #default
