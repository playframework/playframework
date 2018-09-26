/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache.caffeine;

import play.api.cache.caffeine.CaffeineCacheApi;
import play.api.cache.caffeine.CaffeineCacheManager;
import play.api.cache.caffeine.NamedCaffeineCacheProvider$;
import play.cache.AsyncCacheApi;
import play.cache.DefaultAsyncCacheApi;
import play.components.AkkaComponents;
import play.components.ConfigurationComponents;


/**
 * Caffeine Cache Java Components for compile time injection.
 *
 * <p>Usage:</p>
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
public interface CaffeineCacheComponents extends ConfigurationComponents, AkkaComponents {
    default AsyncCacheApi cacheApi(String name) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager(config().getConfig("play.cache.defaultCache"));

        play.api.cache.AsyncCacheApi scalaAsyncCacheApi = new CaffeineCacheApi(
            NamedCaffeineCacheProvider$.MODULE$.getNamedCache(name, caffeineCacheManager, configuration()),
            executionContext()
        );
        return new DefaultAsyncCacheApi(scalaAsyncCacheApi);
    }

    default AsyncCacheApi defaultCacheApi() {
        return cacheApi(config().getString("play.cache.defaultCache"));
    }
}
