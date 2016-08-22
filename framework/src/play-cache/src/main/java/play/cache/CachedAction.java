/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.cache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.inject.Inject;

import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;

/**
 * Cache another action.
 */
public class CachedAction extends Action<Cached> {

    private AsyncCacheApi cacheApi;

    @Inject
    public CachedAction(AsyncCacheApi cacheApi) {
        this.cacheApi = cacheApi;
    }

    public CompletionStage<Result> call(Context ctx, Function<Context, CompletionStage<Result>> delegate) {
        final String key = configuration.key();
        final Integer duration = configuration.duration();
        return cacheApi.<Result>get(key).thenComposeAsync(cacheResult -> {
            if (cacheResult != null) {
                return CompletableFuture.completedFuture(cacheResult);
            }
            return delegate.apply(ctx).thenComposeAsync(result ->
                    cacheApi.set(key, result, duration).thenApply(ignore -> result)
            );
        });
    }

}
