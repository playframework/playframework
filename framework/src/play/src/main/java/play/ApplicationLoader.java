/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import play.core.SourceMapper;
import play.core.DefaultWebCommands;
import play.libs.Scala;

/**
 * Loads an application.  This is responsible for instantiating an application given a context.
 *
 * Application loaders are expected to instantiate all parts of an application, wiring everything together. They may
 * be manually implemented, if compile time wiring is preferred, or core/third party implementations may be used, for
 * example that provide a runtime dependency injection framework.
 *
 * During dev mode, an ApplicationLoader will be instantiated once, and called once, each time the application is
 * reloaded. In prod mode, the ApplicationLoader will be instantiated and called once when the application is started.
 *
 * Out of the box Play provides a Java and Scala default implementation based on Guice. The Java implementation is the
 * {@link play.inject.guice.GuiceApplicationLoader} and the Scala implementation is {@link play.api.inject.guice.GuiceApplicationLoader}.
 *
 * A custom application loader can be configured using the `application.loader` configuration property.
 * Implementations must define a no-arg constructor.
 */
public interface ApplicationLoader {

  /**
   * Load an application given the context.
   *
   * @param context the context the apps hould be loaded into
   * @return the loaded application
   */
  Application load(ApplicationLoader.Context context);

  /**
   * The context for loading an application.
   */
  final static class Context {

    private final play.api.ApplicationLoader.Context underlying;

  /**
   * The context for loading an application.
   *
   * @param underlying The Scala context that is being wrapped.
   */
    public Context(play.api.ApplicationLoader.Context underlying) {
        this.underlying = underlying;
    }

  /**
   * The context for loading an application.
   *
   * @param environment the application environment
   */
    public Context(Environment environment) {
        this(environment, new HashMap<>());
    }

  /**
   * The context for loading an application.
   *
   * @param environment the application environment
   * @param initialSettings the initial settings. These settings are merged with the settings from the loaded
   *                        configuration files, and together form the initialConfiguration provided by the context.  It
   *                        is intended for use in dev mode, to allow the build system to pass additional configuration
   *                        into the application.
   */
    public Context(Environment environment, Map<String,Object> initialSettings) {
        this.underlying = new play.api.ApplicationLoader.Context(
           environment.underlying(),
           scala.Option.empty(),
           new play.core.DefaultWebCommands(),
           play.api.Configuration.load(environment.underlying(), play.libs.Scala.asScala(initialSettings)));
    }

    /**
     * Get the wrapped Scala context.
     *
     * @return the wrapped scala context
     */
    public play.api.ApplicationLoader.Context underlying() {
        return underlying;
    }

    /**
     * Get the environment from the context.
     *
     * @return the environment
     */
    public Environment environment() {
        return new Environment(underlying.environment());
    }

    /**
     * Get the configuration from the context. This configuration is not necessarily the same
     * configuration used by the application, as the ApplicationLoader may, through it's own
     * mechanisms, modify it or completely ignore it.
     *
     * @return the initial configuration
     */
    public Configuration initialConfiguration() {
        return new Configuration(underlying.initialConfiguration());
    }

    /**
     * Create a new context with a different environment.
     *
     * @param environment the environment this context should use
     * @return a context using the specified environment
     */
    public Context withEnvironment(Environment environment) {
        play.api.ApplicationLoader.Context scalaContext = new play.api.ApplicationLoader.Context(
           environment.underlying(),
           underlying.sourceMapper(),
           underlying.webCommands(),
           underlying.initialConfiguration());
        return new Context(scalaContext);
    }

    /**
     * Create a new context with a different configuration.
     *
     * @param initialConfiguration the configuration to use in the created context
     * @return the created context
     */
    public Context withConfiguration(Configuration initialConfiguration) {
        play.api.ApplicationLoader.Context scalaContext = new play.api.ApplicationLoader.Context(
           underlying.environment(),
           underlying.sourceMapper(),
           underlying.webCommands(),
           initialConfiguration.getWrappedConfiguration());
        return new Context(scalaContext);
    }

    // The following static methods are on the Context inner class rather
    // than the ApplicationLoader interface because https://issues.scala-lang.org/browse/SI-8852
    // wasn't fixed until Scala 2.11.3, and at the time of writing we need
    // to support Scala 2.10.

    /**
     * Create an application loading context.
     *
     * Locates and loads the necessary configuration files for the application.
     *
     * @param environment The application environment.
     * @param initialSettings The initial settings. These settings are merged with the settings from the loaded
     *                        configuration files, and together form the initialConfiguration provided by the context.  It
     *                        is intended for use in dev mode, to allow the build system to pass additional configuration
     *                        into the application.
     * @return the created context
     */
    public static Context create(Environment environment, Map<String, Object> initialSettings) {
        play.api.ApplicationLoader.Context scalaContext = play.api.ApplicationLoader$.MODULE$.createContext(
            environment.underlying(),
            Scala.asScala(initialSettings),
            Scala.<SourceMapper>None(),
            new DefaultWebCommands());
        return new Context(scalaContext);
    }

    /**
     * Create an application loading context.
     *
     * Locates and loads the necessary configuration files for the application.
     *
     * @param environment The application environment.
     * @return a context created with the provided underlying environment
     */
    public static Context create(Environment environment) {
        return create(environment, Collections.emptyMap());
    }

  }

}
