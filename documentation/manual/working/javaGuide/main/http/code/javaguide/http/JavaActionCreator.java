/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.http;

// #default
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import java.util.concurrent.CompletionStage;

import java.lang.reflect.Method;

public class JavaActionCreator implements play.http.ActionCreator {
  @Override
  public Action createAction(Http.Request request, Method actionMethod) {
    return new Action.Simple() {
      @Override
      public CompletionStage<Result> call(Http.Context ctx) {
        return delegate.call(ctx);
      }
    };
  }
}
// #default
