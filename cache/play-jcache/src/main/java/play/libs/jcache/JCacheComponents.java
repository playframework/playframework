/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.jcache;

import javax.cache.CacheManager;
import javax.cache.Caching;
import play.Environment;

/** JCache components */
public interface JCacheComponents {

  Environment environment();

  default CacheManager cacheManager() {
    return Caching.getCachingProvider(environment().classLoader()).getCacheManager();
  }
}
