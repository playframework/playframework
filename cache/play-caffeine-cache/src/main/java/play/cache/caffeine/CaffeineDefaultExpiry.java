/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache.caffeine;

import com.github.benmanes.caffeine.cache.Expiry;

/**
 * @deprecated Deprecated as of 2.8.0. This is an implementation detail and it was not supposed to
 *     be public.
 */
@Deprecated
public final class CaffeineDefaultExpiry implements Expiry<Object, Object> {
  @Override
  public long expireAfterCreate(Object key, Object value, long currentTime) {
    return Long.MAX_VALUE;
  }

  @Override
  public long expireAfterUpdate(Object key, Object value, long currentTime, long currentDuration) {
    return currentDuration;
  }

  @Override
  public long expireAfterRead(Object key, Object value, long currentTime, long currentDuration) {
    return currentDuration;
  }
}
