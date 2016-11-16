/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */

//#default
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import java.lang.reflect.Method;

public class ActionCreator implements play.http.ActionCreator {
    @Override
    public Action createAction(Http.Request request, Method actionMethod) {
        return new Action.Simple() {
            @Override
            public CompletionStage<Result> call(Http.Context ctx, Function<Http.Context, CompletionStage<Result>> delegate) {
                return delegate.apply(ctx);
            }
        };
    }
}
//#default
