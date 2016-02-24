/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api

import java.net.URL
import java.util.Properties

/**
 * Runs through underlying logger configuration.
 */
trait LoggerConfigurator {

  /**
   * Initialize the Logger when there's no application ClassLoader available.
   */
  def init(rootPath: java.io.File, mode: Mode.Mode): Unit

  /**
   * Reconfigures the underlying logger infrastructure.
   */
  def configure(env: Environment): Unit

  /**
   * Reconfigures the underlying  logger infrastructure.
   */
  def configure(properties: Map[String, String], config: Option[URL]): Unit

  /**
   * Shutdown the logger infrastructure.
   */
  def shutdown()
}

object LoggerConfigurator {

  def apply(classLoader: ClassLoader): Option[LoggerConfigurator] = {
    findFromResources(classLoader).flatMap { className =>
      apply(className, this.getClass.getClassLoader)
    }
  }

  def apply(loggerConfiguratorClassName: String, classLoader: ClassLoader): Option[LoggerConfigurator] = {
    try {
      val loggerConfiguratorClass: Class[_] = classLoader.loadClass(loggerConfiguratorClassName)
      Some(loggerConfiguratorClass.newInstance().asInstanceOf[LoggerConfigurator])
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
