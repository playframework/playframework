/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package shutdown;

import akka.actor.CoordinatedShutdown;

import javax.inject.Inject;

//#shutdown-task
public class ResourceAllocatingJavaClass {

    private final Resources resources;

    @Inject
    public ResourceAllocatingJavaClass(CoordinatedShutdown cs) {

        // Some resource allocation happens here: A connection
        // pool is created, some client library is started, ...
        resources = Resources.allocate();

        // Register a shutdown task as soon as possible.
        cs.addTask(
            CoordinatedShutdown.PhaseServiceUnbind(),
            "free-some-resource",
            () -> resources.release()
        );
    }

    // ... some more code
}
//#shutdown-task
