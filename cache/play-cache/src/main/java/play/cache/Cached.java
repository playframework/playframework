/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache;

import java.lang.annotation.*;
import play.mvc.With;

/**
 * Mark an action to be cached on server side.
 *
 * @see CachedAction
 */
@With(CachedAction.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cached {
  /**
   * The cache key to store the result in
   *
   * @return the cache key
   */
  String key();

  /**
   * The duration the action should be cached for. Defaults to 0.
   *
   * @return the duration
   */
  int duration() default 0;
}
