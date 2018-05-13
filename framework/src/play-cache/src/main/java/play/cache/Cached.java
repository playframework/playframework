/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache;
import play.mvc.With;

import java.lang.annotation.*;

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
     * The duration the action should be cached for.  Defaults to 0.
     *
     * @return the duration
     */
    int duration() default 0;
}
