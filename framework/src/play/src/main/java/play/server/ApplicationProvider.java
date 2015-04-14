package play.server;

import play.Application;

import java.io.File;

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
