/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

import java.lang.annotation.*;

/**
 * Decorates an <code>Action</code> or a <code>Controller</code> with another <code>Action</code>.
 */ 
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME )
public @interface With {
    Class<? extends Action<?>>[] value();
}
