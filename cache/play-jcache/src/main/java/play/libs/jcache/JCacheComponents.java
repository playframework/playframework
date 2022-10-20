/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
