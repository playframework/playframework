/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache.caffeine;

import com.github.benmanes.caffeine.cache.Expiry;

import javax.annotation.Nonnull;

public final class CaffeineDefaultExpiry implements Expiry<Object, Object> {
    @Override
    public long expireAfterCreate(@Nonnull Object key, @Nonnull Object value, long currentTime) {
        return Long.MAX_VALUE;
    }

    @Override
    public long expireAfterUpdate(@Nonnull Object key, @Nonnull Object value, long currentTime, long currentDuration) {
        return currentDuration;
    }

    @Override
    public long expireAfterRead(@Nonnull Object key, @Nonnull Object value, long currentTime, long currentDuration) {
        return currentDuration;
    }
}