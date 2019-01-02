/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.jcache

import javax.cache.CacheManager

import play.api.test._

/**
 *
 */
class JCacheSpec extends PlaySpecification {

  "CacheManager" should {

    "be instantiated" in new WithApplication() with Injecting {
      val cacheManager = inject[CacheManager]
      cacheManager must not beNull
    }
  }
}
