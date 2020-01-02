/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache.caffeine;

import com.github.benmanes.caffeine.cache.Expiry;

import javax.annotation.Nonnull;

/**
 * @deprecated Deprecated as of 2.8.0. This is an implementation detail and it was not supposed to
 *     be public.
 */
@Deprecated
public final class CaffeineDefaultExpiry implements Expiry<Object, Object> {
  @Override
  public long expireAfterCreate(@Nonnull Object key, @Nonnull Object value, long currentTime) {
    return Long.MAX_VALUE;
  }

  @Override
  public long expireAfterUpdate(
      @Nonnull Object key, @Nonnull Object value, long currentTime, long currentDuration) {
    return currentDuration;
  }

  @Override
  public long expireAfterRead(
      @Nonnull Object key, @Nonnull Object value, long currentTime, long currentDuration) {
    return currentDuration;
  }
}
