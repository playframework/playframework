/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
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
