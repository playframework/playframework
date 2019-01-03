/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import java.net.URL
import java.util.Properties

import com.typesafe.config.ConfigValueType
import org.slf4j.ILoggerFactory

/**
 * Runs through underlying logger configuration.
 */
trait LoggerConfigurator {

  /**
   * Initialize the Logger when there's no application ClassLoader available.
   */
  def init(rootPath: java.io.File, mode: Mode): Unit

  /**
   * This is a convenience method that adds no extra properties.
   */
  def configure(env: Environment): Unit

  /**
   * Configures the logger with the environment and the application configuration.
   *
   * This is what full applications will run, and the place to put extra properties,
   * either through optionalProperties or by setting configuration properties and
   * having "play.logger.includeConfigProperties=true" in the config.
   *
   * @param env the application environment
   * @param configuration the application's configuration
   * @param optionalProperties any optional properties (you can use Map.empty otherwise)
   */
  def configure(env: Environment, configuration: Configuration, optionalProperties: Map[String, String]): Unit

  /**
   * Configures the logger with a list of properties and an optional URL.
   *
   * This is the engine's entrypoint method that has all the properties pre-assembled.
   */
  def configure(properties: Map[String, String], config: Option[URL]): Unit

  /**
   * Returns the logger factory for the configurator.  Only safe to call after configuration.
   * @return an instance of ILoggerFactory
   */
  def loggerFactory: ILoggerFactory

  /**
   * Shutdown the logger infrastructure.
   */
  def shutdown()

}

object LoggerConfigurator {

  def apply(classLoader: ClassLoader): Option[LoggerConfigurator] = {
    findFromResources(classLoader).flatMap { className =>
      apply(className, classLoader)
    }
  }

  /**
   * Generates the map of properties used by the logging framework.
   */
  def generateProperties(env: Environment, config: Configuration, optionalProperties: Map[String, String]): Map[String, String] = {
    import scala.collection.JavaConverters._
    val mutableMap = new scala.collection.mutable.HashMap[String, String]()
    mutableMap.put("application.home", env.rootPath.getAbsolutePath)

    if (config.getOptional[Boolean]("play.logger.includeConfigProperties").contains(true)) {
      val entrySet = config.underlying.entrySet.asScala
      for (entry <- entrySet) {
        val value = entry.getValue
        value.valueType() match {
          case ConfigValueType.STRING =>
            mutableMap.put(entry.getKey, value.unwrapped().asInstanceOf[String])
          case _ =>
            mutableMap.put(entry.getKey, value.render())
        }
      }
    }
    (mutableMap ++ optionalProperties).toMap
  }

  def apply(loggerConfiguratorClassName: String, classLoader: ClassLoader): Option[LoggerConfigurator] = {
    try {
      val loggerConfiguratorClass: Class[_] = classLoader.loadClass(loggerConfiguratorClassName)
      Some(loggerConfiguratorClass.getDeclaredConstructor().newInstance().asInstanceOf[LoggerConfigurator])
    } catch {
      case ex: Exception =>
        val msg =
          s"""
             |Play cannot load "$loggerConfiguratorClassName". Please make sure you have logback (or another module
             |that implements play.api.LoggerConfigurator) in your classpath.
             """.stripMargin
        System.err.println(msg)
        ex.printStackTrace()
        None
    }
  }

  private def findFromResources(classLoader: ClassLoader): Option[String] = {
    val in = classLoader.getResourceAsStream("logger-configurator.properties")
    if (in != null) {
      try {
        val props = new Properties()
        props.load(in)
        Option(props.getProperty("play.logger.configurator"))
      } catch {
        case ex: Exception =>
          ex.printStackTrace()
          None
      } finally {
        in.close()
      }
    } else {
      None
    }
  }

}
