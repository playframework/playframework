/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.inject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

@Singleton
public class DelegateApplicationLifecycle implements ApplicationLifecycle {
    private final play.api.inject.ApplicationLifecycle delegate;

    @Inject
    public DelegateApplicationLifecycle(play.api.inject.ApplicationLifecycle delegate) {
        this.delegate = delegate;
    }

    @Override
    public void addStopHook(final Callable<? extends CompletionStage<?>> hook) {
        delegate.addStopHook(hook);
    }
}
