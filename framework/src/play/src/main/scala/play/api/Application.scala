/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import play.core._
import play.utils._

import play.api.mvc._

import java.io._

import annotation.implicitNotFound

import reflect.ClassTag
import scala.util._
import scala.util.control.NonFatal
import scala.concurrent.{ Future, ExecutionException }

trait WithDefaultConfiguration {
  self: Application =>

  protected[api] lazy val initialConfiguration = Threads.withContextClassLoader(self.classloader) {
    Configuration.load(path, mode, this match {
      case dev: DevSettings => dev.devSettings
      case _ => Map.empty
    })
  }

  private lazy val fullConfiguration = global.onLoadConfig(initialConfiguration, path, classloader, mode)

  def configuration: Configuration = fullConfiguration

}

trait WithDefaultInjectionProvider {
  self: Application with WithDefaultConfiguration =>

  private lazy val _injectionProvider = {
    val configProviderClass = initialConfiguration.getString("application.injectionprovider")
    val injectionProviderClass = configProviderClass.getOrElse("play.api.DefaultInjectionProvider")

    lazy val javaProvider: Option[play.InjectionProvider] = try {
      Option(self.classloader.loadClass(injectionProviderClass).newInstance().asInstanceOf[play.InjectionProvider])
    } catch {
      case e: InstantiationException => None
      case e: ClassNotFoundException => None
    }

    lazy val scalaProvider: InjectionProvider = try {
      self.classloader.loadClass(injectionProviderClass).getConstructor(classOf[Application]).newInstance(self).asInstanceOf[InjectionProvider]
    } catch {
      case NonFatal(e) => throw initialConfiguration.reportError("application.injectionprovider",
        s"Cannot initialize the custom InjectionProvider object ($injectionProviderClass) (perhaps it's a wrong reference?)", Some(e))
    }

    javaProvider.map(new j.JavaInjectionProviderAdapter(_)).getOrElse(scalaProvider)
  }

  def injectionProvider: InjectionProvider = _injectionProvider
}

/**
 * A Play application.
 *
 * Application creation is handled by the framework engine.
 *
 * If you need to create an ad-hoc application,
 * for example in case of unit testing, you can easily achieve this using:
 * {{{
 * val application = new DefaultApplication(new File("."), this.getClass.getClassloader, None, Play.Mode.Dev)
 * }}}
 *
 * This will create an application using the current classloader.
 *
 */
@implicitNotFound(msg = "You do not have an implicit Application in scope. If you want to bring the current running Application into context, just add import play.api.Play.current")
trait Application {

  /**
   * The absolute path hosting this application, mainly used by the `getFile(path)` helper method
   */
  def path: File

  /**
   * The application's classloader
   */
  def classloader: ClassLoader

  /**
   * The `SourceMapper` used to retrieve source code displayed in error pages
   */
  def sources: Option[SourceMapper]

  /**
   * `Dev`, `Prod` or `Test`
   */
  def mode: Mode.Mode

  def configuration: Configuration
  def global: GlobalSettings = injectionProvider.global

  def injectionProvider: InjectionProvider
  def plugins: Seq[Plugin] = injectionProvider.plugins
  def plugin[T <: Plugin](pluginClass: Class[T]): Option[T] = injectionProvider.getPlugin(pluginClass)
  def inject[T](clazz: Class[T]): Try[T] = injectionProvider.getInstance(clazz)

  /**
   * Retrieves a plugin of type `T`.
   *
   * For example, to retrieve the DBPlugin instance:
   * {{{
   * val dbPlugin = application.plugin[DBPlugin].map(_.api).getOrElse(sys.error("problem with the plugin"))
   * }}}
   *
   * @tparam T the plugin type
   * @return The plugin instance used by this application.
   * @throws Error if no plugins of type T are loaded by this application.
   */
  def plugin[T <: Plugin](implicit ct: ClassTag[T]): Option[T] = plugin(ct.runtimeClass.asInstanceOf[Class[T]])

  /**
   * Retrieves an instance of type `T`, as defined by the current InjectionProvider
   *
   * @tparam T the class type
   * @return A Try containing either the instance used by this application, or an error thrown while trying to get one
   */
  def inject[T](implicit ct: ClassTag[T]): Try[T] = inject(ct.runtimeClass.asInstanceOf[Class[T]])

  /**
   * The router used by this application (if defined).
   */
  lazy val routes: Option[Router.Routes] = loadRoutes

  protected def loadRoutes: Option[Router.Routes] = try {
    Some(classloader.loadClass(configuration.getString("application.router").map(_ + "$").getOrElse("Routes$")).getDeclaredField("MODULE$").get(null).asInstanceOf[Router.Routes]).map { router =>
      router.setPrefix(configuration.getString("application.context").map { prefix =>
        if (!prefix.startsWith("/")) {
          throw configuration.reportError("application.context", "Invalid application context")
        }
        prefix
      }.getOrElse("/"))
      router
    }
  } catch {
    case e: ClassNotFoundException => configuration.getString("application.router").map { routerName =>
      throw configuration.reportError("application.router", "Router not found: " + routerName)
    }
  }

  // Reconfigure logger
  {

    val validValues = Set("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF", "INHERITED")
    val setLevel = (level: String) => level match {
      case "INHERITED" => null
      case level => ch.qos.logback.classic.Level.toLevel(level)
    }

    Logger.configure(
      Map("application.home" -> path.getAbsolutePath),
      configuration.getConfig("logger").map { loggerConfig =>
        loggerConfig.keys.map {
          case "resource" | "file" | "url" => "" -> null
          case key @ "root" => "ROOT" -> loggerConfig.getString(key, Some(validValues)).map(setLevel).get
          case key => key -> loggerConfig.getString(key, Some(validValues)).map(setLevel).get
        }.toMap
      }.getOrElse(Map.empty),
      mode)

  }

  /**
   * Handle a runtime error during the execution of an action
   */
  private[play] def handleError(request: RequestHeader, e: Throwable): Future[Result] = try {
    e match {
      case e: UsefulException => throw e
      case e: ExecutionException => handleError(request, e.getCause)
      case e: Throwable => {

        val source = sources.flatMap(_.sourceFor(e))

        throw new PlayException.ExceptionSource(
          "Execution exception",
          "[%s: %s]".format(e.getClass.getSimpleName, e.getMessage),
          e) {
          def line = source.flatMap(_._2).map(_.asInstanceOf[java.lang.Integer]).orNull
          def position = null
          def input = source.map(_._1).map(PlayIO.readFileAsString).orNull
          def sourceName = source.map(_._1.getAbsolutePath).orNull
        }
      }
    }
  } catch {
    case NonFatal(e) => try {
      Logger.error(
        """
        |
        |! %sInternal server error, for (%s) [%s] ->
        |""".stripMargin.format(e match {
          case p: PlayException => "@" + p.id + " - "
          case _ => ""
        }, request.method, request.uri),
        e
      )
      global.onError(request, e)
    } catch {
      case NonFatal(e) => DefaultGlobal.onError(request, e)
    }
  }

  /**
   * Retrieves a file relative to the application root path.
   *
   * Note that it is up to you to manage the files in the application root path in production.  By default, there will
   * be nothing available in the application root path.
   *
   * For example, to retrieve some deployment specific data file:
   * {{{
   * val myDataFile = application.getFile("data/data.xml")
   * }}}
   *
   * @param relativePath relative path of the file to fetch
   * @return a file instance; it is not guaranteed that the file exists
   */
  def getFile(relativePath: String): File = new File(path, relativePath)

  /**
   * Retrieves a file relative to the application root path.
   * This method returns an Option[File], using None if the file was not found.
   *
   * Note that it is up to you to manage the files in the application root path in production.  By default, there will
   * be nothing available in the application root path.
   *
   * For example, to retrieve some deployment specific data file:
   * {{{
   * val myDataFile = application.getExistingFile("data/data.xml")
   * }}}
   *
   * @param relativePath the relative path of the file to fetch
   * @return an existing file
   */
  def getExistingFile(relativePath: String): Option[File] = Option(getFile(relativePath)).filter(_.exists)

  /**
   * Scans the application classloader to retrieve a resource.
   *
   * The conf directory is included on the classpath, so this may be used to look up resources, relative to the conf
   * directory.
   *
   * For example, to retrieve the conf/logger.xml configuration file:
   * {{{
   * val maybeConf = application.resource("logger.xml")
   * }}}
   *
   * @param name the absolute name of the resource (from the classpath root)
   * @return the resource URL, if found
   */
  def resource(name: String): Option[java.net.URL] = {
    Option(classloader.getResource(Option(name).map {
      case s if s.startsWith("/") => s.drop(1)
      case s => s
    }.get))
  }

  /**
   * Scans the application classloader to retrieve a resource’s contents as a stream.
   *
   * The conf directory is included on the classpath, so this may be used to look up resources, relative to the conf
   * directory.
   *
   * For example, to retrieve the conf/logger.xml configuration file:
   * {{{
   * val maybeConf = application.resourceAsStream("logger.xml")
   * }}}
   *
   * @param name the absolute name of the resource (from the classpath root)
   * @return a stream, if found
   */
  def resourceAsStream(name: String): Option[InputStream] = {
    Option(classloader.getResourceAsStream(Option(name).map {
      case s if s.startsWith("/") => s.drop(1)
      case s => s
    }.get))
  }

}

class DefaultApplication(
  override val path: File,
  override val classloader: ClassLoader,
  override val sources: Option[SourceMapper],
  override val mode: Mode.Mode) extends Application with WithDefaultConfiguration with WithDefaultInjectionProvider
