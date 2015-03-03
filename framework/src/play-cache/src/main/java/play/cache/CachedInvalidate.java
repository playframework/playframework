/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.cache;

import play.mvc.With;

import java.lang.annotation.*;

/**
 * Mark an action to remove cache keys values when this action trigger.
 */
@With(CachedInvalidateAction.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CachedInvalidate {
    /**
     * The cache keys to remove
     */
    String[] keys();

}
