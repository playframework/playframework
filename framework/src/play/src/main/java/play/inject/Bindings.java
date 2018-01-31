/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import play.api.inject.BindingKey;

public class Bindings {

    /**
     * Create a binding key for the given class.
     * @param <T> the type of the bound class
     * @param clazz    the class to bind
     * @return the binding key for the given class
     */
    public static final <T> BindingKey<T> bind(Class<T> clazz) {
        return new BindingKey(clazz);
    }

}
