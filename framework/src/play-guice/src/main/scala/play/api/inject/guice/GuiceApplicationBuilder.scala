/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.inject.guice

import javax.inject.{ Inject, Provider, Singleton }

import com.google.inject.{ Module => GuiceModule }
import org.slf4j.ILoggerFactory
import play.api._
import play.api.inject.{ RoutesProvider, bind }
import play.api.mvc.{ Handler, RequestHeader }
import play.api.routing.Router
import play.core.{ DefaultWebCommands, WebCommands }

import scala.runtime.AbstractPartialFunction

/**
 * A builder for creating Applications using Guice.
 */
final case class GuiceApplicationBuilder(
    environment: Environment = Environment.simple(),
    configuration: Configuration = Configuration.empty,
    modules: Seq[GuiceableModule] = Seq.empty,
    overrides: Seq[GuiceableModule] = Seq.empty,
    disabled: Seq[Class[_]] = Seq.empty,
    binderOptions: Set[BinderOption] = BinderOption.defaults,
    eagerly: Boolean = false,
    loadConfiguration: Environment => Configuration = Configuration.load,
    loadModules: (Environment, Configuration) => Seq[GuiceableModule] = GuiceableModule.loadModules) extends GuiceBuilder[GuiceApplicationBuilder](
  environment, configuration, modules, overrides, disabled, binderOptions, eagerly
) {

  // extra constructor for creating from Java
  def this() = this(environment = Environment.simple())

  /**
   * Sets the configuration key to enable/disable global application state
   */
  def globalApp(enabled: Boolean): GuiceApplicationBuilder =
    configure(Play.GlobalAppConfigKey -> enabled)

  /**
   * Set the initial configuration loader.
   * Overrides the default or any previously configured values.
   */
  def loadConfig(loader: Environment => Configuration): GuiceApplicationBuilder =
    copy(loadConfiguration = loader)

  /**
   * Set the initial configuration.
   * Overrides the default or any previously configured values.
   */
  def loadConfig(conf: Configuration): GuiceApplicationBuilder =
    loadConfig(env => conf)

  /**
   * Set the module loader.
   * Overrides the default or any previously configured values.
   */
  def load(loader: (Environment, Configuration) => Seq[GuiceableModule]): GuiceApplicationBuilder =
    copy(loadModules = loader)

  /**
   * Override the module loader with the given modules.
   */
  def load(modules: GuiceableModule*): GuiceApplicationBuilder =
    load((env, conf) => modules)

  /**
   * Override the router with a fake router having the given routes, before falling back to the default router
   */
  def appRoutes(routes: Application => PartialFunction[(String, String), Handler]): GuiceApplicationBuilder =
    bindings(bind[FakeRouterConfig] to FakeRouterConfig(routes))
      .overrides(bind[Router].toProvider[FakeRouterProvider].in[Singleton])

  def routes(routesFunc: PartialFunction[(String, String), Handler]): GuiceApplicationBuilder =
    appRoutes(_ => routesFunc)

  /**
   * Override the router with the given router.
   */
  def router(router: Router): GuiceApplicationBuilder =
    overrides(bind[Router].toInstance(router))

  /**
   * Override the router with a router that first tries to route to the passed in additional router, before falling
   * back to the default router.
   */
  def additionalRouter(router: Router): GuiceApplicationBuilder =
    overrides(bind[Router].to(new AdditionalRouterProvider(router)))

  /**
   * Create a new Play application Module for an Application using this configured builder.
   */
  override def applicationModule(): GuiceModule = {
    val initialConfiguration = loadConfiguration(environment)
    val appConfiguration = initialConfiguration ++ configuration

    val loggerFactory = configureLoggerFactory(appConfiguration)

    val loadedModules = loadModules(environment, appConfiguration)

    copy(configuration = appConfiguration)
      .bindings(loadedModules: _*)
      .bindings(
        bind[ILoggerFactory] to loggerFactory,
        bind[OptionalDevContext] to new OptionalDevContext(None),
        bind[OptionalSourceMapper].toProvider[OptionalSourceMapperProvider],
        bind[WebCommands] to new DefaultWebCommands
      ).createModule()
  }

  /**
   * Configures the SLF4J logger factory.  This is where LoggerConfigurator is
   * called from.
   *
   * @param configuration play.api.Configuration
   * @return the app wide ILoggerFactory.  Useful for testing and DI.
   */
  def configureLoggerFactory(configuration: Configuration): ILoggerFactory = {
    val loggerFactory: ILoggerFactory = LoggerConfigurator(environment.classLoader).map { lc =>
      lc.configure(environment, configuration, Map.empty)
      lc.loggerFactory
    }.getOrElse(org.slf4j.LoggerFactory.getILoggerFactory)

    if (shouldDisplayLoggerDeprecationMessage(configuration)) {
      val logger = loggerFactory.getLogger("application")
      logger.warn("Logger configuration in conf files is deprecated and has no effect. Use a logback configuration file instead.")
    }

    loggerFactory
  }

  /**
   * Create a new Play Application using this configured builder.
   */
  def build(): Application = injector().instanceOf[Application]

  /**
   * Internal copy method with defaults.
   */
  private def copy(
    environment: Environment = environment,
    configuration: Configuration = configuration,
    modules: Seq[GuiceableModule] = modules,
    overrides: Seq[GuiceableModule] = overrides,
    disabled: Seq[Class[_]] = disabled,
    binderOptions: Set[BinderOption] = binderOptions,
    eagerly: Boolean = eagerly,
    loadConfiguration: Environment => Configuration = loadConfiguration,
    loadModules: (Environment, Configuration) => Seq[GuiceableModule] = loadModules): GuiceApplicationBuilder =
    new GuiceApplicationBuilder(environment, configuration, modules, overrides, disabled, binderOptions, eagerly, loadConfiguration, loadModules)

  /**
   * Implementation of Self creation for GuiceBuilder.
   */
  protected def newBuilder(
    environment: Environment,
    configuration: Configuration,
    modules: Seq[GuiceableModule],
    overrides: Seq[GuiceableModule],
    disabled: Seq[Class[_]],
    binderOptions: Set[BinderOption] = binderOptions,
    eagerly: Boolean): GuiceApplicationBuilder =
    copy(environment, configuration, modules, overrides, disabled, binderOptions, eagerly)

  /**
   * Checks if the path contains the logger path
   * and whether or not one of the keys contains a deprecated value
   *
   * @param appConfiguration The app configuration
   * @return Returns true if one of the keys contains a deprecated value, otherwise false
   */
  def shouldDisplayLoggerDeprecationMessage(appConfiguration: Configuration): Boolean = {
    import scala.collection.JavaConverters._
    import scala.collection.mutable

    val deprecatedValues = List("DEBUG", "WARN", "ERROR", "INFO", "TRACE", "OFF")

    // Recursively checks each key to see if it contains a deprecated value
    def hasDeprecatedValue(values: mutable.Map[String, AnyRef]): Boolean = {
      values.exists {
        case (_, value: String) if deprecatedValues.contains(value) =>
          true
        case (_, value: java.util.Map[_, _]) =>
          val v = value.asInstanceOf[java.util.Map[String, AnyRef]]
          hasDeprecatedValue(v.asScala)
        case _ =>
          false
      }
    }

    if (appConfiguration.underlying.hasPath("logger")) {
      appConfiguration.underlying.getAnyRef("logger") match {
        case value: String =>
          hasDeprecatedValue(mutable.Map("logger" -> value))
        case value: java.util.Map[_, _] =>
          val v = value.asInstanceOf[java.util.Map[String, AnyRef]]
          hasDeprecatedValue(v.asScala)
        case _ =>
          false
      }
    } else {
      false
    }
  }
}

private class AdditionalRouterProvider(additional: Router) extends Provider[Router] {
  @Inject private var fallback: RoutesProvider = _
  lazy val get = Router.from(additional.routes.orElse(fallback.get.routes))
}

private class FakeRoutes(
    injected: => PartialFunction[(String, String), Handler], fallback: Router) extends Router {
  def documentation = fallback.documentation
  // Use withRoutes first, then delegate to the parentRoutes if no route is defined
  val routes = new AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B >: Handler](rh: A, default: A => B) =
      injected.applyOrElse((rh.method, rh.path), (_: (String, String)) => default(rh))
    def isDefinedAt(rh: RequestHeader) = injected.isDefinedAt((rh.method, rh.path))
  } orElse new AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B >: Handler](rh: A, default: A => B) =
      fallback.routes.applyOrElse(rh, default)
    def isDefinedAt(x: RequestHeader) = fallback.routes.isDefinedAt(x)
  }
  def withPrefix(prefix: String) = {
    new FakeRoutes(injected, fallback.withPrefix(prefix))
  }
}

private case class FakeRouterConfig(withRoutes: Application => PartialFunction[(String, String), Handler])

private class FakeRouterProvider @Inject() (config: FakeRouterConfig, parent: RoutesProvider, appProvider: Provider[Application]) extends Provider[Router] {
  def get: Router = new FakeRoutes(config.withRoutes(appProvider.get), parent.get)
}
