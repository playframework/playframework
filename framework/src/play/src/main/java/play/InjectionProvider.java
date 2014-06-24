package play;

import java.util.List;

/**
 * Provides injection of plugins, controllers, and other dependencies used by the application.
 *
 * The implementation of InjectionProvider is defined in application.conf, e.g.:
 *
 * {{{
 * application.injectionprovider=com.example.MyInjectionProvider
 * }}}
 *
 * The implementation must have a constructor which takes an Application as its only argument.
 *
 * DefaultInjectionProvider is used if no InjectionProvider is defined in the configuration.
 */
public interface InjectionProvider {
    /**
     * Get all the plugin instances used by this application.
     *
     * @return A list of plugin instances used by the application
     */
    List<play.api.Plugin> plugins() throws Exception;

    /**
     * Retrieves an instance of type `T`.
     *
     * @param  clazz the class
     * @return a Try containing either an instance of the class or an exception thrown while trying to get one.
     */
    <T> T getInstance(Class<T> clazz) throws Exception;

    /**
     * Retrieves the plugin of type `T` used by the application.
     *
     * @param  clazz the plugin’s class
     * @return the plugin instance wrapped in an option if it's loaded by this provider, None otherwise.
     */
    <T extends play.api.Plugin> T getPlugin(Class<T> clazz);

    /**
     * Retrieves the GlobalSettings instance used by this application.
     *
     * @return the current GlobalSettings being used
     */
    GlobalSettings global();
}
