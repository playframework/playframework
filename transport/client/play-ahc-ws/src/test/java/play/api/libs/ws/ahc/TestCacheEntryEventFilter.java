/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.ws.ahc;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryEventFilter;

public final class TestCacheEntryEventFilter implements CacheEntryEventFilter<Integer, Integer> {

  @Override
  public boolean evaluate(CacheEntryEvent<? extends Integer, ? extends Integer> event) {
    return false;
  }
}
