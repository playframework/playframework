/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

/**
 * A default implementation of SyncCacheApi that wraps AsyncCacheApi
 */
public class DefaultSyncCacheApi implements SyncCacheApi, CacheApi {

    private final AsyncCacheApi cacheApi;

    protected long awaitTimeoutMillis = 5000;

    @Inject
    public DefaultSyncCacheApi(AsyncCacheApi cacheApi) {
        this.cacheApi = cacheApi;
    }

    @Override
    public <T> T get(String key) {
        return blocking(cacheApi.get(key));
    }

    /**
     * @deprecated Use getOrElseUpdate instead (2.6.0)
     */
    @Override
    @Deprecated
    public <T> T getOrElse(String key, Callable<T> block, int expiration) {
        return getOrElseUpdate(key, block, expiration);
    }

    /**
     * @deprecated Use getOrElseUpdate instead (2.6.0)
     */
    @Override
    @Deprecated
    public <T> T getOrElse(String key, Callable<T> block) {
        return getOrElseUpdate(key, block);
    }

    @Override
    public <T> T getOrElseUpdate(String key, Callable<T> block, int expiration) {
        return blocking(cacheApi.getOrElseUpdate(key, () -> CompletableFuture.completedFuture(block.call()), expiration));
    }

    @Override
    public <T> T getOrElseUpdate(String key, Callable<T> block) {
        return blocking(cacheApi.getOrElseUpdate(key, () -> CompletableFuture.completedFuture(block.call())));
    }

    @Override
    public void set(String key, Object value, int expiration) {
        blocking(cacheApi.set(key, value, expiration));
    }

    @Override
    public void set(String key, Object value) {
        blocking(cacheApi.set(key, value));
    }

    @Override
    public void remove(String key) {
        blocking(cacheApi.remove(key));
    }

    private <T> T blocking(CompletionStage<T> stage) {
        boolean interrupted = false;
        try {
            for (;;) {
                try {
                    return stage.toCompletableFuture().get(awaitTimeoutMillis, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
