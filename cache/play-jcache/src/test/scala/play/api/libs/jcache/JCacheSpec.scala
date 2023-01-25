/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.jcache

import javax.cache.CacheManager

import play.api.test._

/**
 */
class JCacheSpec extends PlaySpecification {
  "CacheManager" should {
    "be instantiated" in new WithApplication() with Injecting {
      val cacheManager = inject[CacheManager]
      cacheManager must not beNull
    }
  }
}
