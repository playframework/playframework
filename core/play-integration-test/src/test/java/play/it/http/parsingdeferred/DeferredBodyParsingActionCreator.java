/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.parsingdeferred;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import play.http.ActionCreator;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

public final class DeferredBodyParsingActionCreator implements ActionCreator {

  @Override
  public Action<?> createAction(Http.Request request, Method actionMethod) {
    String creation =
        DeferredBodyParsingSpec.buildRequestStateMessage(
            "Action creator creation", request.asScala());
    return new Action.Simple() {
      @Override
      public CompletionStage<Result> call(Http.Request req) {
        String invocation =
            DeferredBodyParsingSpec.buildRequestStateMessage(
                "Action creator invocation", req.asScala());
        String currentInvocation = creation + " | " + invocation;
        String previousInvocations = req.attrs().getOptional(Attrs.ACTION_CREATOR_FLOW).orElse("");
        String invocations =
            previousInvocations.isEmpty()
                ? currentInvocation
                : previousInvocations + " -> " + currentInvocation;
        return delegate.call(req.addAttr(Attrs.ACTION_CREATOR_FLOW, invocations));
      }
    };
  }
}
