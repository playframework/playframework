/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject.guice

import com.google.inject.{ Module => GuiceModule }
import play.api.{ Application, Configuration, Environment, GlobalSettings, Logger, OptionalSourceMapper }
import play.api.inject.{ bind, Injector => PlayInjector }
import play.api.routing.Router
import play.api._
import play.api.inject.{ RoutesProvider, bind }
import play.core.{ DefaultWebCommands, WebCommands }

import scala.collection.mutable

/**
 * A builder for creating Applications using Guice.
 */
final class GuiceApplicationBuilder(
  environment: Environment = Environment.simple(),
  configuration: Configuration = Configuration.empty,
  modules: Seq[GuiceableModule] = Seq.empty,
  overrides: Seq[GuiceableModule] = Seq.empty,
  disabled: Seq[Class[_]] = Seq.empty,
  loadConfiguration: Environment => Configuration = Configuration.load,
  global: Option[GlobalSettings] = None,
  loadModules: (Environment, Configuration) => Seq[GuiceableModule] = GuiceableModule.loadModules) extends GuiceBuilder[GuiceApplicationBuilder](
  environment, configuration, modules, overrides, disabled
) {

  // extra constructor for creating from Java
  def this() = this(environment = Environment.simple())

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
   * Set the global settings object.
   * Overrides the default or any previously configured values.
   */
  def global(globalSettings: GlobalSettings): GuiceApplicationBuilder =
    copy(global = Option(globalSettings))

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
   * Create a new Play application Module for an Application using this configured builder.
   */
  override def applicationModule(): GuiceModule = {
    val initialConfiguration = loadConfiguration(environment)
    val appConfiguration = initialConfiguration ++ configuration
    val globalSettings = global.getOrElse(GlobalSettings(appConfiguration, environment))

    // TODO: Logger should be application specific, and available via dependency injection.
    //       Creating multiple applications will stomp on the global logger configuration.
    Logger.configure(environment)

    if (shouldDisplayLoggerDeprecationMessage(appConfiguration)) {
      Logger.warn("Logger configuration in conf files is deprecated and has no effect. Use a logback configuration file instead.")
    }

    val loadedModules = loadModules(environment, appConfiguration)

    copy(configuration = appConfiguration)
      .bindings(loadedModules: _*)
      .bindings(
        bind[GlobalSettings] to globalSettings,
        bind[OptionalSourceMapper] to new OptionalSourceMapper(None),
        bind[WebCommands] to new DefaultWebCommands
      ).createModule()
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
    loadConfiguration: Environment => Configuration = loadConfiguration,
    global: Option[GlobalSettings] = global,
    loadModules: (Environment, Configuration) => Seq[GuiceableModule] = loadModules): GuiceApplicationBuilder =
    new GuiceApplicationBuilder(environment, configuration, modules, overrides, disabled, loadConfiguration, global, loadModules)

  /**
   * Implementation of Self creation for GuiceBuilder.
   */
  protected def newBuilder(
    environment: Environment,
    configuration: Configuration,
    modules: Seq[GuiceableModule],
    overrides: Seq[GuiceableModule],
    disabled: Seq[Class[_]]): GuiceApplicationBuilder =
    copy(environment, configuration, modules, overrides, disabled)

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
        case (_, value: java.util.Map[String, AnyRef]) =>
          hasDeprecatedValue(value.asScala)
        case _ =>
          false
      }
    }

    if (appConfiguration.underlying.hasPath("logger")) {
      appConfiguration.underlying.getAnyRef("logger") match {
        case value: String =>
          hasDeprecatedValue(mutable.Map("logger" -> value))
        case value: java.util.Map[String, AnyRef] =>
          hasDeprecatedValue(value.asScala)
        case _ =>
          false
      }
    } else {
      false
    }
  }
}
