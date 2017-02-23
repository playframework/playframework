/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.server;

import play.Application;


/**
 * Provides information about a Play Application running inside a Play server.
 */
public class ApplicationProvider {

    private final Application application;

    public ApplicationProvider(Application application) {
        this.application = application;
    }

    public Application getApplication() {
        return application;
    }

}
