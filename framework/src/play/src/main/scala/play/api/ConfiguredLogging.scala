/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import org.slf4j.ILoggerFactory

trait ConfiguredLogging {

  val environment: Environment

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
