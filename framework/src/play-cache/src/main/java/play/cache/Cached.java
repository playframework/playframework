/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.cache;
import play.mvc.With;

import java.lang.annotation.*;

/**
 * Mark an action to be cached on server side or remove cache key value when this action trigger.
 */
@With(CachedAction.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cached {
    /**
     * The cache key to store the result in or remove
     */
    String key();

    /**
     * The duration the action should be cached for.  Defaults to 0.
     */
    int duration() default 0;
    
    /**
     *  Indicate remove cache key or store. Defaults is false.
     */    
    boolean remove() default false;
}
