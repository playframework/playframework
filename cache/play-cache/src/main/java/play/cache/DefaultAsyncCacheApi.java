/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache;

import static scala.jdk.javaapi.FutureConverters.asJava;

import org.apache.pekko.Done;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.libs.Scala;
import scala.concurrent.duration.Duration;
import scala.jdk.javaapi.OptionConverters;

/**
 * Adapts a Scala AsyncCacheApi to a Java AsyncCacheApi. This is Play's default Java AsyncCacheApi
 * implementation.
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
  public <T> CompletionStage<Optional<T>> get(String key) {
    return asJava(asyncCacheApi.get(key, Scala.<T>classTag())).thenApply(OptionConverters::toJava);
  }

  @Override
  public <T> CompletionStage<T> getOrElseUpdate(
      String key, Callable<CompletionStage<T>> block, int expiration) {
    return asJava(
        asyncCacheApi.getOrElseUpdate(
            key, intToDuration(expiration), Scala.asScalaWithFuture(block), Scala.<T>classTag()));
  }

  @Override
  public <T> CompletionStage<T> getOrElseUpdate(String key, Callable<CompletionStage<T>> block) {
    return asJava(
        asyncCacheApi.getOrElseUpdate(
            key, Duration.Inf(), Scala.asScalaWithFuture(block), Scala.<T>classTag()));
  }

  @Override
  public CompletionStage<Done> set(String key, Object value, int expiration) {
    return asJava(asyncCacheApi.set(key, value, intToDuration(expiration)));
  }

  @Override
  public CompletionStage<Done> set(String key, Object value) {
    return asJava(asyncCacheApi.set(key, value, Duration.Inf()));
  }

  @Override
  public CompletionStage<Done> remove(String key) {
    return asJava(asyncCacheApi.remove(key));
  }

  @Override
  public CompletionStage<Done> removeAll() {
    return asJava(asyncCacheApi.removeAll());
  }

  private Duration intToDuration(int seconds) {
    return seconds == 0 ? Duration.Inf() : Duration.apply(seconds, TimeUnit.SECONDS);
  }
}
