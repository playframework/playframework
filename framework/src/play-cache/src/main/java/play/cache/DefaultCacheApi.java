/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.cache;

import play.libs.Scala;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Singleton
public class DefaultCacheApi implements CacheApi {

    private final play.api.cache.CacheApi cacheApi;

    @Inject
    public DefaultCacheApi(play.api.cache.CacheApi cacheApi) {
        this.cacheApi = cacheApi;
    }

    public <T> T get(String key) {
        return Scala.orNull(cacheApi.get(key, Scala.<T>classTag()));
    }

    public <T> T getOrElse(String key, Callable<T> block, int expiration) {
        return cacheApi.getOrElse(key, intToDuration(expiration),
                Scala.asScala(block), Scala.<T>classTag());
    }

    public <T> T getOrElse(String key, Callable<T> block) {
        return cacheApi.getOrElse(key, Duration.Inf(),
                Scala.asScala(block), Scala.<T>classTag());
    }

    public void set(String key, Object value, int expiration) {
        cacheApi.set(key, value, intToDuration(expiration));
    }

    public void set(String key, Object value) {
        cacheApi.set(key, value, Duration.Inf());
    }

    public void remove(String key) {
        cacheApi.remove(key);
    }

    private Duration intToDuration(int seconds) {
      return seconds == 0 ? Duration.Inf() : Duration.apply(seconds, TimeUnit.SECONDS);
    }
}
