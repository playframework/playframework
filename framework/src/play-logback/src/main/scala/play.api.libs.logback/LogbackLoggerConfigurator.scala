/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.logback

import java.io.File
import java.net.URL

import ch.qos.logback.classic._
import ch.qos.logback.classic.joran._
import ch.qos.logback.core.util._
import org.slf4j.LoggerFactory
import org.slf4j.bridge._
import org.slf4j.impl.StaticLoggerBinder
import play.api._

class LogbackLoggerConfigurator extends LoggerConfigurator {

  /**
   * Initialize the Logger when there's no application ClassLoader available.
   */
  def init(rootPath: java.io.File, mode: Mode.Mode): Unit = {
    val properties = Map("application.home" -> rootPath.getAbsolutePath)
    val resourceName = if (mode == Mode.Dev) "logback-play-dev.xml" else "logback-play-default.xml"
    val resourceUrl = Option(this.getClass.getClassLoader.getResource(resourceName))
    configure(properties, resourceUrl)
  }

  /**
   * Reconfigures the underlying logback infrastructure.
   */
  def configure(env: Environment): Unit = {
    val properties = Map("application.home" -> env.rootPath.getAbsolutePath)

    // Get an explicitly configured resource URL
    // Fallback to a file in the conf directory if the resource wasn't found on the classpath
    def explicitResourceUrl = sys.props.get("logger.resource").map { r =>
      env.resource(r).getOrElse(new File(env.getFile("conf"), r).toURI.toURL)
    }

    // Get an explicitly configured file URL
    def explicitFileUrl = sys.props.get("logger.file").map(new File(_).toURI.toURL)

    // application-logger.xml and logger.xml are no longer supported methods for supplying the configuration
    // Support removed in Play 2.5. This notice can be removed in future versions of Play
    if (!env.resource("application-logger.xml").orElse(env.resource("logger.xml")).isEmpty) {
      System.err.println("application-logger.xml and logger.xml are no longer supported. Please name your file logback.xml");
    }

    // logback.xml is the documented method, logback-play-default.xml is the fallback that Play uses
    // if no other file is found
    def resourceUrl = env.resource("logback.xml")
      .orElse(env.resource(
        if (env.mode == Mode.Dev) "logback-play-dev.xml" else "logback-play-default.xml"
      ))

    val configUrl = explicitResourceUrl orElse explicitFileUrl orElse resourceUrl

    configure(properties, configUrl)
  }

  /**
   * Reconfigures the underlying logback infrastructure.
   */
  def configure(properties: Map[String, String], config: Option[URL]): Unit = synchronized {
    // Redirect JUL -> SL4FJ

    Option(java.util.logging.Logger.getLogger("")).map { root =>
      root.setLevel(java.util.logging.Level.FINEST)
      root.getHandlers.foreach(root.removeHandler)
    }

    SLF4JBridgeHandler.install()

    // Configure logback

    val ctx = StaticLoggerBinder.getSingleton.getLoggerFactory.asInstanceOf[LoggerContext]
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
  def shutdown() {

    val ctx = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    ctx.stop()

    org.slf4j.bridge.SLF4JBridgeHandler.uninstall()
  }

}
