package play.server;

import play.Application;

import java.io.File;

/**
 * Provides information about a Play Application running inside a Play server.
 */
public class ApplicationProvider {

    private final Application application;
    private final File path;

    public ApplicationProvider(Application application, File path) {
        this.application = application;
        this.path = path;
    }

    public Application getApplication() {
        return application;
    }

    public File getPath() {
        return path;
    }
}
