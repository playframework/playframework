/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache.caffeine;

import play.api.cache.caffeine.CaffeineCacheApi;
import play.api.cache.caffeine.CaffeineCacheManager;
import play.api.cache.caffeine.NamedCaffeineCacheProvider$;
import play.cache.AsyncCacheApi;
import play.cache.DefaultAsyncCacheApi;
import play.components.ConfigurationComponents;
import play.components.PekkoComponents;

/**
 * Caffeine Cache Java Components for compile time injection.
 *
 * <p>Usage:
 *
 * <pre>
 * public class MyComponents extends BuiltInComponentsFromContext implements CaffeineCacheComponents {
 *
 *   public MyComponents(ApplicationLoader.Context context) {
 *       super(context);
 *   }
 *
 *   // A service class that depends on cache APIs
 *   public CachedService someService() {
 *       // defaultCacheApi is provided by CaffeineCacheComponents
 *       return new CachedService(defaultCacheApi());
 *   }
 *
 *   // Another service that depends on a specific named cache
 *   public AnotherService someService() {
 *       // cacheApi provided by CaffeineCacheComponents and
 *       // "anotherService" is the name of the cache.
 *       return new CachedService(cacheApi("anotherService"));
 *   }
 *
 *   // other methods
 * }
 * </pre>
 */
public interface CaffeineCacheComponents extends ConfigurationComponents, PekkoComponents {
  default AsyncCacheApi cacheApi(String name) {
    CaffeineCacheManager caffeineCacheManager =
        new CaffeineCacheManager(config().getConfig("play.cache.caffeine"), actorSystem());

    play.api.cache.AsyncCacheApi scalaAsyncCacheApi =
        new CaffeineCacheApi(
            NamedCaffeineCacheProvider$.MODULE$.getNamedCache(
                name, caffeineCacheManager, configuration()));
    return new DefaultAsyncCacheApi(scalaAsyncCacheApi);
  }

  default AsyncCacheApi defaultCacheApi() {
    return cacheApi(config().getString("play.cache.defaultCache"));
  }
}
