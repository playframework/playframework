/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.logback

import java.io.File
import java.net.URL

import org.slf4j.LoggerFactory
import play.api._

import scala.util.control.NonFatal

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

    // Get an explicitly configured URL
    def explicitUrl = sys.props.get("logger.url").map(new URL(_))

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

    val configUrl = explicitResourceUrl orElse explicitFileUrl orElse explicitUrl orElse resourceUrl

    configure(properties, configUrl)
  }

  /**
   * Reconfigures the underlying logback infrastructure.
   */
  def configure(properties: Map[String, String], config: Option[URL]): Unit = synchronized {
    // Redirect JUL -> SL4FJ
    {
      import java.util.logging._

      import org.slf4j.bridge._

      Option(java.util.logging.Logger.getLogger("")).map { root =>
        root.setLevel(Level.FINEST)
        root.getHandlers.foreach(root.removeHandler(_))
      }

      SLF4JBridgeHandler.install()
    }

    // Configure logback
    {
      import ch.qos.logback.classic._
      import ch.qos.logback.classic.joran._
      import ch.qos.logback.core.util._
      import org.slf4j._

      try {
        val ctx = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
        val configurator = new JoranConfigurator
        configurator.setContext(ctx)
        ctx.reset()

        // Ensure that play.Logger and play.api.Logger are ignored when detecting file name and line number for
        // logging
        val frameworkPackages = ctx.getFrameworkPackages
        frameworkPackages.add(classOf[play.Logger].getName)
        frameworkPackages.add(classOf[play.api.Logger].getName)

        properties.foreach {
          case (name, value) => ctx.putProperty(name, value)
        }

        try {
          config match {
            case Some(url) => configurator.doConfigure(url)
            case None =>
              System.err.println("Could not detect a logback configuration file, not configuring logback")
          }
        } catch {
          case NonFatal(e) =>
            System.err.println("Error encountered while configuring logback:")
            e.printStackTrace()
        }

        StatusPrinter.printIfErrorsOccured(ctx)
      } catch {
        case NonFatal(_) =>
      }

    }

  }

  /**
   * Shutdown the logger infrastructure.
   */
  def shutdown() {
    import ch.qos.logback.classic._

    val ctx = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    ctx.stop()

    org.slf4j.bridge.SLF4JBridgeHandler.uninstall()
  }

}
