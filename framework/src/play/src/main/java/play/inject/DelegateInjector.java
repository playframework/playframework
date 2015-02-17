/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.inject;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DelegateInjector implements Injector {
    public final play.api.inject.Injector injector;

    @Inject
    public DelegateInjector(play.api.inject.Injector injector) {
        this.injector = injector;
    }

    @Override
    public <T> T instanceOf(Class<T> clazz) {
        return injector.instanceOf(clazz);
    }
}
