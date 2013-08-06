package play;

import play.api.*;

import java.util.List;

import static scala.collection.JavaConversions.seqAsJavaList;

/**
 * Java utility methods to be used in InjectionProvider implementations.
 */
public class InjectionProviders {
    /**
     * Load the default plugins from the play.plugins file.
     *
     * @param app the application
     * @return a list of instantiated plugins
     */
    public static List<play.api.Plugin> loadPlugins(Application app) {
        return seqAsJavaList(InjectionProvider$.MODULE$.loadPlugins(app.getWrappedApplication()));
    }
}
