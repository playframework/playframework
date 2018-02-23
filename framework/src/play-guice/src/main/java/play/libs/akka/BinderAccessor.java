/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.akka;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;

import java.lang.reflect.Method;

/**
 * Accesses an abstract modules binder.
 */
class BinderAccessor {

    /**
     * Get the binder from an AbstractModule.
     */
    static Binder binder(Object module) {
        if (module instanceof AbstractModule) {
            try {
                Method method = AbstractModule.class.getDeclaredMethod("binder");
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                return (Binder) method.invoke(module);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("Module must be an instance of AbstractModule");
        }
    }
}
