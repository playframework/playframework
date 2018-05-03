/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
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
 * An implementation of SyncCacheApi that wraps AsyncCacheApi
 *
 * Note: this class is really not the "default" implementation of the CacheApi in Play. SyncCacheApiAdapter is actually
 * used in the default Ehcache implementation. A better name for this class might be "BlockingSyncCacheApi" since it
 * blocks on the futures from the async implementation.
 */
public class DefaultSyncCacheApi implements SyncCacheApi {

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
