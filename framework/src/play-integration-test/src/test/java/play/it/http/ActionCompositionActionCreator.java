/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import play.http.ActionCreator;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Results;
import play.test.Helpers;

public class ActionCompositionActionCreator implements ActionCreator {

    @Override
    public Action createAction(Http.Request request, Method actionMethod) {
        return new Action.Simple() {
            @Override
            public CompletionStage<Result> call(Http.Context ctx, Function<Context, CompletionStage<Result>> delegate) {
                return delegate.apply(ctx).thenApply(result -> {
                    String newContent = "actioncreator" + Helpers.contentAsString(result);
                    return Results.ok(newContent);
                });
            }
        };
    }
}
