/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import java.io.File

import play.api.inject.guice.GuiceApplicationLoader
import play.core.SourceMapper
import play.utils.{ Threads, Reflect }

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
 * Out of the box Play provides one default implementation, the [[play.api.inject.guice.GuiceApplicationLoader]].
 *
 * A custom application loader can be configured using the `application.loader` configuration property.
 * Implementations must define a noarg constructor.
 */
trait ApplicationLoader {

  /**
   * Load an application given the context.
   */
  def load(context: ApplicationLoader.Context): Application
}

object ApplicationLoader {

  /**
   * The context for loading an application.
   *
   * @param environment The environment
   * @param sourceMapper An optional source mapper
   * @param initialConfiguration The initial configuration.  This configuration is not necessarily the same
   *                             configuration used by the application, as the ApplicationLoader may, through it's own
   *                             mechanisms, modify it or completely ignore it.
   */
  final case class Context(environment: Environment, sourceMapper: Option[SourceMapper], initialConfiguration: Configuration)

  /**
   * Locate and instantiate the ApplicationLoader.
   */
  def apply(context: Context): ApplicationLoader = {
    context.initialConfiguration.getString("application.loader").fold[ApplicationLoader](new GuiceApplicationLoader) { loaderClass =>
      Reflect.createInstance[ApplicationLoader](loaderClass, context.environment.classLoader)
    }
  }

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
   * @param sourceMapper An optional source mapper.
   */
  def createContext(environment: Environment,
    initialSettings: Map[String, String] = Map.empty[String, String],
    sourceMapper: Option[SourceMapper] = None) = {
    val configuration = Threads.withContextClassLoader(environment.classLoader) {
      Configuration.load(environment.rootPath, environment.mode, initialSettings)
    }

    Context(environment, sourceMapper, configuration)
  }
}
