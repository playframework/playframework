/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache.caffeine

import scala.concurrent.duration._
import scala.concurrent.duration.Duration

import com.github.benmanes.caffeine.cache.Expiry
import org.apache.pekko.annotation.InternalApi
import play.api.cache.ExpirableCacheValue

@InternalApi
private[caffeine] class DefaultCaffeineExpiry extends Expiry[String, ExpirableCacheValue[Any]] {
  def expireAfterCreate(key: String, value: ExpirableCacheValue[Any], currentTime: Long): Long = {
    calculateExpirationTime(value.durationMaybe)
  }

  def expireAfterUpdate(
      key: String,
      value: ExpirableCacheValue[Any],
      currentTime: Long,
      currentDuration: Long
  ): Long = {
    calculateExpirationTime(value.durationMaybe)
  }

  def expireAfterRead(key: String, value: ExpirableCacheValue[Any], currentTime: Long, currentDuration: Long): Long = {
    currentDuration
  }

  private def calculateExpirationTime(durationMaybe: Option[Duration]): Long = {
    durationMaybe match {
      case Some(duration) if duration.isFinite && duration.lteq(0.second) => 1.second.toNanos
      case Some(duration) if duration.isFinite                            => duration.toNanos
      case _                                                              => Long.MaxValue
    }
  }
}
