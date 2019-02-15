/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import akka.Done;
import play.libs.Scala;
import scala.concurrent.duration.Duration;

import scala.compat.java8.OptionConverters;
import static scala.compat.java8.FutureConverters.toJava;

/**
 * Adapts a Scala AsyncCacheApi to a Java AsyncCacheApi. This is Play's default Java AsyncCacheApi implementation.
 */
@Singleton
public class DefaultAsyncCacheApi implements AsyncCacheApi {

    private final play.api.cache.AsyncCacheApi asyncCacheApi;

    @Inject
    public DefaultAsyncCacheApi(play.api.cache.AsyncCacheApi cacheApi) {
        this.asyncCacheApi = cacheApi;
    }

    @Override
    public SyncCacheApi sync() {
        return new SyncCacheApiAdapter(asyncCacheApi.sync());
    }

    @Override
    @Deprecated
    public <T> CompletionStage<T> get(String key) {
        return toJava(asyncCacheApi.get(key, Scala.<T>classTag())).thenApply(Scala::orNull);
    }

    @Override
    public <T> CompletionStage<Optional<T>> getOptional(String key) {
        return toJava(asyncCacheApi.get(key, Scala.<T>classTag())).thenApply(OptionConverters::toJava);
    }

    @Override
    public <T> CompletionStage<T> getOrElseUpdate(String key, Callable<CompletionStage<T>> block, int expiration) {
        return toJava(
            asyncCacheApi.getOrElseUpdate(key, intToDuration(expiration), Scala.asScalaWithFuture(block), Scala.<T>classTag()));
    }

    @Override
    public <T> CompletionStage<T> getOrElseUpdate(String key, Callable<CompletionStage<T>> block) {
        return toJava(asyncCacheApi.getOrElseUpdate(key, Duration.Inf(), Scala.asScalaWithFuture(block), Scala.<T>classTag()));
    }

    @Override
    public CompletionStage<Done> set(String key, Object value, int expiration) {
        return toJava(asyncCacheApi.set(key, value, intToDuration(expiration)));
    }

    @Override
    public CompletionStage<Done> set(String key, Object value) {
        return toJava(asyncCacheApi.set(key, value, Duration.Inf()));
    }

    @Override
    public CompletionStage<Done> remove(String key) {
        return toJava(asyncCacheApi.remove(key));
    }

    @Override
    public CompletionStage<Done> removeAll() {
        return toJava(asyncCacheApi.removeAll());
    }

    private Duration intToDuration(int seconds) {
      return seconds == 0 ? Duration.Inf() : Duration.apply(seconds, TimeUnit.SECONDS);
    }
}
