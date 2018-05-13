/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache.ehcache;

import net.sf.ehcache.CacheManager;
import play.Environment;
import play.api.cache.ehcache.CacheManagerProvider;
import play.api.cache.ehcache.EhCacheApi;
import play.api.cache.ehcache.NamedEhCacheProvider$;
import play.cache.AsyncCacheApi;
import play.cache.DefaultAsyncCacheApi;
import play.components.AkkaComponents;
import play.components.ConfigurationComponents;
import play.inject.ApplicationLifecycle;

/**
 * EhCache Java Components for compile time injection.
 *
 * <p>Usage:</p>
 *
 * <pre>
 * public class MyComponents extends BuiltInComponentsFromContext implements EhCacheComponents {
 *
 *   public MyComponents(ApplicationLoader.Context context) {
 *       super(context);
 *   }
 *
 *   // A service class that depends on cache APIs
 *   public CachedService someService() {
 *       // defaultCacheApi is provided by EhCacheComponents
 *       return new CachedService(defaultCacheApi());
 *   }
 *
 *   // Another service that depends on a specific named cache
 *   public AnotherService someService() {
 *       // cacheApi provided by EhCacheComponents and
 *       // "anotherService" is the name of the cache.
 *       return new CachedService(cacheApi("anotherService"));
 *   }
 *
 *   // other methods
 * }
 * </pre>
 */
public interface EhCacheComponents extends ConfigurationComponents, AkkaComponents {

    Environment environment();

    ApplicationLifecycle applicationLifecycle();

    default CacheManager ehCacheManager() {
        return new CacheManagerProvider(
            environment().asScala(),
            configuration(),
            applicationLifecycle().asScala()
        ).get();
    }

    default AsyncCacheApi cacheApi(String name) {
        boolean createNamedCaches = config().getBoolean("play.cache.createBoundCaches");
        play.api.cache.AsyncCacheApi scalaAsyncCacheApi = new EhCacheApi(
            NamedEhCacheProvider$.MODULE$.getNamedCache(name, ehCacheManager(), createNamedCaches),
            executionContext()
        );
        return new DefaultAsyncCacheApi(scalaAsyncCacheApi);
    }

    default AsyncCacheApi defaultCacheApi() {
        return cacheApi("play");
    }
}
