package play.api

import play.core._
import play.utils._

import play.api.mvc._

import java.io._

import annotation.implicitNotFound

import java.lang.reflect.InvocationTargetException
import reflect.ClassTag
import scala.util.control.NonFatal
import scala.concurrent.Future

trait WithDefaultGlobal {
  self: Application with WithDefaultConfiguration =>

  // -- Global stuff

  private lazy val globalClass = initialConfiguration.getString("application.global").getOrElse(initialConfiguration.getString("global").map { g =>
    Play.logger.warn("`global` key is deprecated, please change `global` key to `application.global`")
    g
  }.getOrElse("Global"))

  lazy private val javaGlobal: Option[play.GlobalSettings] = try {
    Option(self.classloader.loadClass(globalClass).newInstance().asInstanceOf[play.GlobalSettings])
  } catch {
    case e: InstantiationException => None
    case e: ClassNotFoundException => None
  }

  lazy private val scalaGlobal: GlobalSettings = try {
    self.classloader.loadClass(globalClass + "$").getDeclaredField("MODULE$").get(null).asInstanceOf[GlobalSettings]
  } catch {
    case e: ClassNotFoundException if !initialConfiguration.getString("application.global").isDefined => DefaultGlobal
    case e if initialConfiguration.getString("application.global").isDefined => {
      throw initialConfiguration.reportError("application.global", "Cannot initialize the custom Global object (%s) (perhaps it's a wrong reference?)", Some(e))
    }
  }

  /**
   * The global settings object used by this application.
   *
   * @see play.api.GlobalSettings
   */
  private lazy val globalInstance: GlobalSettings = Threads.withContextClassLoader(self.classloader) {
    try {
      javaGlobal.map(new j.JavaGlobalSettingsAdapter(_)).getOrElse(scalaGlobal)
    } catch {
      case e: PlayException => throw e
      case e: ThreadDeath => throw e
      case e: VirtualMachineError => throw e
      case e: Throwable => throw new PlayException(
        "Cannot init the Global object",
        e.getMessage,
        e
      )
    }
  }

  def global: GlobalSettings = {
    globalInstance
  }
}

trait WithDefaultConfiguration {
  self: Application =>

  protected lazy val initialConfiguration = Threads.withContextClassLoader(self.classloader) {
    Configuration.load(path, mode, this match {
      case dev: DevSettings => dev.devSettings
      case _ => Map.empty
    })
  }

  private lazy val fullConfiguration = global.onLoadConfig(initialConfiguration, path, classloader, mode)

  def configuration: Configuration = fullConfiguration

}

trait WithDefaultPlugins {
  self: Application =>

  private[api] def pluginClasses: Seq[String] = {

    import scalax.file._
    import scalax.io.JavaConverters._
    import scala.collection.JavaConverters._

    val PluginDeclaration = """([0-9_]+):(.*)""".r

    val pluginFiles = self.classloader.getResources("play.plugins").asScala.toList ++ self.classloader.getResources("conf/play.plugins").asScala.toList

    pluginFiles.distinct.map { plugins =>
      (plugins.asInput.string.split("\n").map(_.replaceAll("#.*$", "").trim)).filterNot(_.isEmpty).map {
        case PluginDeclaration(priority, className) => (priority.toInt, className)
      }
    }.flatten.sortBy(_._1).map(_._2)

  }

  /**
   * The plugins list used by this application.
   *
   * Plugin classes must extend play.api.Plugin and are automatically discovered
   * by searching for all play.plugins files in the classpath.
   *
   * A play.plugins file contains a list of plugin classes to be loaded, and sorted by priority:
   *
   * {{{
   * 100:play.api.i18n.MessagesPlugin
   * 200:play.api.db.DBPlugin
   * 250:play.api.cache.BasicCachePlugin
   * 300:play.db.ebean.EbeanPlugin
   * 400:play.db.jpa.JPAPlugin
   * 500:play.api.db.evolutions.EvolutionsPlugin
   * 1000:play.api.libs.akka.AkkaPlugin
   * 10000:play.api.GlobalPlugin
   * }}}
   *
   * @see play.api.Plugin
   */
  lazy val plugins: Seq[Plugin] = Threads.withContextClassLoader(classloader) {

    pluginClasses.map { className =>
      try {
        val plugin = classloader.loadClass(className).getConstructor(classOf[Application]).newInstance(this).asInstanceOf[Plugin]
        if (plugin.enabled) Some(plugin) else { Play.logger.debug("Plugin [" + className + "] is disabled"); None }
      } catch {
        case e: java.lang.NoSuchMethodException => {
          try {
            val plugin = classloader.loadClass(className).getConstructor(classOf[play.Application]).newInstance(new play.Application(this)).asInstanceOf[Plugin]
            if (plugin.enabled) Some(plugin) else { Play.logger.warn("Plugin [" + className + "] is disabled"); None }
          } catch {
            case e: java.lang.NoSuchMethodException =>
              throw new PlayException("Cannot load plugin",
                "Could not find an appropriate constructor to instantiate plugin [" + className +
                  "]. All Play plugins must define a constructor that accepts a single argument either of type " +
                  "play.Application for Java plugins or play.api.Application for Scala plugins.")
            case e: PlayException => throw e
            case e: VirtualMachineError => throw e
            case e: ThreadDeath => throw e
            case e: Throwable => throw new PlayException(
              "Cannot load plugin",
              "Plugin [" + className + "] cannot be instantiated.",
              e)
          }
        }
        case e: InvocationTargetException => throw new PlayException(
          "Cannot load plugin",
          "An exception occurred during Plugin [" + className + "] initialization",
          e.getTargetException)
        case e: PlayException => throw e
        case e: ThreadDeath => throw e
        case e: VirtualMachineError => throw e
        case e: Throwable => throw new PlayException(
          "Cannot load plugin",
          "Plugin [" + className + "] cannot be instantiated.",
          e)
      }
    }.flatten

  }
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

  def global: GlobalSettings
  def configuration: Configuration
  def plugins: Seq[Plugin]

  /**
   * Retrieves a plugin of type `T`.
   *
   * For example, retrieving the DBPlugin instance:
   * {{{
   * val dbPlugin = application.plugin(classOf[DBPlugin])
   * }}}
   *
   * @tparam T the plugin type
   * @param  pluginClass the plugin’s class
   * @return the plugin instance, wrapped in an option, used by this application
   * @throws Error if no plugins of type `T` are loaded by this application
   */
  def plugin[T](pluginClass: Class[T]): Option[T] =
    plugins.find(p => pluginClass.isAssignableFrom(p.getClass)).map(_.asInstanceOf[T])

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
  def plugin[T](implicit ct: ClassTag[T]): Option[T] = plugin(ct.runtimeClass).asInstanceOf[Option[T]]

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
  private[play] def handleError(request: RequestHeader, e: Throwable): Future[SimpleResult] = try {
    e match {
      case e: UsefulException => throw e
      case e: Throwable => {

        val source = sources.flatMap(_.sourceFor(e))

        throw new PlayException.ExceptionSource(
          "Execution exception",
          "[%s: %s]".format(e.getClass.getSimpleName, e.getMessage),
          e) {
          def line = source.flatMap(_._2).map(_.asInstanceOf[java.lang.Integer]).orNull
          def position = null
          def input = source.map(_._1).map(scalax.file.Path(_).string).orNull
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
  override val mode: Mode.Mode) extends Application with WithDefaultConfiguration with WithDefaultGlobal with WithDefaultPlugins
