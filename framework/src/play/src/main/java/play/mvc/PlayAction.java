/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import java.util.concurrent.CompletionStage;

public abstract class PlayAction<T> extends Action<T> {

    public PlayAction<?> next;

    @Override
    public final CompletionStage<Result> call(Http.Context ctx) {
        return call(ctx.request());
    }

    public abstract CompletionStage<Result> call(Http.Request request);
}