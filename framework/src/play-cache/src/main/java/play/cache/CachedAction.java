/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.cache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;

/**
 * Cache another action.
 */
public class CachedAction extends Action<Cached> {

    public CompletionStage<Result> call(Context ctx) {
        try {
            final String key = configuration.key();
            final Integer duration = configuration.duration();

            Result cacheResult = (Result) Cache.get(key);

            if (cacheResult == null) {
                return delegate.call(ctx).thenApply(result -> {
                    Cache.set(key, result, duration);
                    return result;
                });
            } else {
                return CompletableFuture.completedFuture(cacheResult);
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
