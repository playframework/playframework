/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.logback

import java.io.File
import java.net.URL

import ch.qos.logback.classic._
import ch.qos.logback.classic.joran._
import ch.qos.logback.core.util._
import org.slf4j.ILoggerFactory
import org.slf4j.bridge._
import org.slf4j.impl.StaticLoggerBinder
import play.api._

class LogbackLoggerConfigurator extends LoggerConfigurator {

  lazy val loggerFactory: ILoggerFactory = StaticLoggerBinder.getSingleton.getLoggerFactory

  /**
   * Initialize the Logger when there's no application ClassLoader available.
   */
  def init(rootPath: java.io.File, mode: Mode.Mode): Unit = {
    // Set the global application mode for logging
    play.api.Logger.setApplicationMode(mode)

    val properties = Map("application.home" -> rootPath.getAbsolutePath)
    val resourceName = if (mode == Mode.Dev) "logback-play-dev.xml" else "logback-play-default.xml"
    val resourceUrl = Option(this.getClass.getClassLoader.getResource(resourceName))
    configure(properties, resourceUrl)
  }

  def configure(env: Environment): Unit = {
    configure(env, Configuration.empty, Map.empty)
  }

  def configure(env: Environment, configuration: Configuration, optionalProperties: Map[String, String]): Unit = {
    // Get an explicitly configured resource URL
    // Fallback to a file in the conf directory if the resource wasn't found on the classpath
    def explicitResourceUrl = sys.props.get("logger.resource").map { r =>
      env.resource(r).getOrElse(new File(env.getFile("conf"), r).toURI.toURL)
    }

    // Get an explicitly configured file URL
    def explicitFileUrl = sys.props.get("logger.file").map(new File(_).toURI.toURL)

    // Get an explicitly configured URL
    def explicitUrl = sys.props.get("logger.url").map(new URL(_))

    // logback.xml is the documented method, logback-play-default.xml is the fallback that Play uses
    // if no other file is found
    def resourceUrl = env.resource("logback.xml")
      .orElse(env.resource(
        if (env.mode == Mode.Dev) "logback-play-dev.xml" else "logback-play-default.xml"
      ))

    val configUrl = explicitResourceUrl orElse explicitFileUrl orElse explicitUrl orElse resourceUrl

    val properties = LoggerConfigurator.generateProperties(env, configuration, optionalProperties)

    configure(properties, configUrl)
  }

  def configure(properties: Map[String, String], config: Option[URL]): Unit = synchronized {
    // Redirect JUL -> SL4FJ

    Option(java.util.logging.Logger.getLogger("")).map { root =>
      root.setLevel(java.util.logging.Level.FINEST)
      root.getHandlers.foreach(root.removeHandler)
    }

    SLF4JBridgeHandler.install()

    // Configure logback

    val ctx = loggerFactory.asInstanceOf[LoggerContext]
    val configurator = new JoranConfigurator
    configurator.setContext(ctx)
    ctx.reset()

    // Ensure that play.Logger and play.api.Logger are ignored when detecting file name and line number for
    // logging
    val frameworkPackages = ctx.getFrameworkPackages
    frameworkPackages.add(classOf[play.Logger].getName)
    frameworkPackages.add(classOf[play.api.Logger].getName)

    properties.foreach { case (k, v) => ctx.putProperty(k, v) }

    config match {
      case Some(url) => configurator.doConfigure(url)
      case None =>
        System.err.println("Could not detect a logback configuration file, not configuring logback")
    }

    StatusPrinter.printIfErrorsOccured(ctx)

  }

  /**
   * Shutdown the logger infrastructure.
   */
  def shutdown(): Unit = {

    val ctx = loggerFactory.asInstanceOf[LoggerContext]
    ctx.stop()

    org.slf4j.bridge.SLF4JBridgeHandler.uninstall()

    // Unset the global application mode for logging
    play.api.Logger.unsetApplicationMode()
  }

}
