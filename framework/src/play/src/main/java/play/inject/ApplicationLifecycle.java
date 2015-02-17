/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.inject;

import play.libs.F;

import java.util.concurrent.Callable;

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
     */
    public void addStopHook(Callable<F.Promise<Void>> hook);
}
