/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache.caffeine

import com.github.benmanes.caffeine.cache.Expiry

class DefaultCaffeineExpiry extends Expiry[String, ExpirableCacheValue[Any]] {

  def expireAfterCreate(key: String, value: ExpirableCacheValue[Any], currentTime: Long): Long = {
    value.durationMaybe match {
      case Some(duration) if duration.isFinite => duration.toNanos
      case _ => Long.MaxValue
    }
  }

  def expireAfterUpdate(key: String, value: ExpirableCacheValue[Any], currentTime: Long, currentDuration: Long): Long = {
    currentDuration
  }

  def expireAfterRead(key: String, value: ExpirableCacheValue[Any], currentTime: Long, currentDuration: Long): Long = {
    currentDuration
  }

}