/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

/**
 * Application lifecycle register.
 *
 * This is used to hook into Play lifecycle events, specifically, when Play is stopped.
 *
 * Stop hooks are executed when the application is shutdown, in reverse from when they were registered.
 *
 * To use this, declare a dependency on ApplicationLifecycle, and then register the stop hook when the component is
 * started.
 */
public interface ApplicationLifecycle {

    /**
     * Add a stop hook to be called when the application stops.
     *
     * The stop hook should redeem the returned future when it is finished shutting down.  It is acceptable to stop
     * immediately and return a successful future.
     * @param hook    the stop hook.
     */
    void addStopHook(Callable<? extends CompletionStage<?>> hook);

    /**
     * @return The Scala version for this Application Lifecycle.
     */
    play.api.inject.ApplicationLifecycle asScala();
}
