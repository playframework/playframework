/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.inject;


import play.libs.F;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Callable;

@Singleton
public class DelegateApplicationLifecycle implements ApplicationLifecycle {
    private final play.api.inject.ApplicationLifecycle delegate;

    @Inject
    public DelegateApplicationLifecycle(play.api.inject.ApplicationLifecycle delegate) {
        this.delegate = delegate;
    }

    @Override
    public void addStopHook(final Callable<F.Promise<Void>> hook) {
        delegate.addStopHook(hook);
    }
}
