/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;

import play.libs.Scala;

/**
 * Adapts a Scala SyncCacheApi to a Java SyncCacheApi
 */
public class SyncCacheApiAdapter implements SyncCacheApi {

  private final play.api.cache.SyncCacheApi scalaApi;

  public SyncCacheApiAdapter(play.api.cache.SyncCacheApi scalaApi) {
    this.scalaApi = scalaApi;
  }

  @Override
  public <T> T get(String key) {
    scala.Option<T> opt = scalaApi.get(key, Scala.classTag());
    if (opt.isDefined()) {
      return opt.get();
    } else {
      return null;
    }
  }

  @Override
  public <T> T getOrElseUpdate(String key, Callable<T> block, int expiration) {
    return scalaApi.getOrElseUpdate(key, intToDuration(expiration), Scala.asScala(block), Scala.classTag());
  }

  @Override
  public <T> T getOrElseUpdate(String key, Callable<T> block) {
    return scalaApi.getOrElseUpdate(key, Duration.Inf(), Scala.asScala(block), Scala.classTag());
  }

  @Override
  public void set(String key, Object value, int expiration) {
    scalaApi.set(key, value, intToDuration(expiration));
  }

  @Override
  public void set(String key, Object value) {
    scalaApi.set(key, value, Duration.Inf());
  }

  @Override
  public void remove(String key) {
    scalaApi.remove(key);
  }

  private Duration intToDuration(int seconds) {
    return seconds == 0 ? Duration.Inf() : Duration.apply(seconds, TimeUnit.SECONDS);
  }
}
