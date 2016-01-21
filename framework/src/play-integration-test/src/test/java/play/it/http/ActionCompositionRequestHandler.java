/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http;

import play.http.HttpRequestHandler;
import play.mvc.*;
import play.test.Helpers;

import java.lang.reflect.Method;

import java.util.concurrent.CompletionStage;

public class ActionCompositionRequestHandler implements HttpRequestHandler {

    @Override
    public Action createAction(Http.Request request, Method actionMethod) {
        return new Action.Simple() {
            @Override
            public CompletionStage<Result> call(Http.Context ctx) {
                return delegate.call(ctx).thenApply(result -> {
                    String newContent = "requesthandler" + Helpers.contentAsString(result);
                    return Results.ok(newContent);
                });
            }
        };
    }

    @Override
    public Action wrapAction(Action action) {
        return action;
    }
}