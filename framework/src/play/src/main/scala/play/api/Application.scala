package play.api

import play.core._
import play.utils._

import play.api.mvc._

import java.io._

import scala.collection.JavaConverters._

import annotation.implicitNotFound

import java.lang.reflect.InvocationTargetException


trait WithDefaultGlobal {
  self: Application with WithDefaultConfiguration =>

  // -- Global stuff

  private lazy val globalClass = initialConfiguration.getString("application.global").getOrElse(initialConfiguration.getString("global").map { g =>
    Logger("play").warn("`global` key is deprecated, please change `global` key to `application.global`")
    g
  }.getOrElse("Global"))

  lazy private val javaGlobal: Option[play.GlobalSettings] = try {
    Option(self.classloader.loadClass(globalClass).newInstance().asInstanceOf[play.GlobalSettings])
  } catch {
    case e: InstantiationException => None
    case e: ClassNotFoundException => None
    case e => throw e
  }

  lazy private val scalaGlobal: GlobalSettings = try {
    self.classloader.loadClass(globalClass + "$").getDeclaredField("MODULE$").get(null).asInstanceOf[GlobalSettings]
  } catch {
    case e: ClassNotFoundException if !initialConfiguration.getString("application.global").isDefined => DefaultGlobal
    case e if initialConfiguration.getString("application.global").isDefined => {
      throw initialConfiguration.reportError("application.global", "Cannot initialize the custom Global object (%s) (perhaps it's a wrong reference?)".format(e.getMessage))
    }
    case e => throw e
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
      case e => throw new PlayException(
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
    Configuration.load(path, mode)
  }

  private lazy val fullConfiguration = {
    initialConfiguration ++ global.configuration
  }

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
      (plugins.asInput.string.split("\n").map(_.trim)).filterNot(_.isEmpty).map {
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
        if (plugin.enabled) Some(plugin) else { Logger("play").debug("Plugin [" + className + "] is disabled"); None }
      } catch {
        case e: java.lang.NoSuchMethodException => {
          try {
            val plugin = classloader.loadClass(className).getConstructor(classOf[play.Application]).newInstance(new play.Application(this)).asInstanceOf[Plugin]
            if (plugin.enabled) Some(plugin) else { Logger("play").warn("Plugin [" + className + "] is disabled"); None }
          } catch {
            case e: PlayException => throw e
            case e => throw new PlayException(
              "Cannot load plugin",
              "Plugin [" + className + "] cannot been instantiated.",
              e)
          }
        }
        case e: InvocationTargetException => throw new PlayException(
          "Cannot load plugin",
          "An exception occurred during Plugin [" + className + "] initialization",
          e.getTargetException)
        case e: PlayException => throw e
        case e => throw new PlayException(
          "Cannot load plugin",
          "Plugin [" + className + "] cannot been instantiated.",
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
 * val application = Application(new File("."), this.getClass.getClassloader, None, Play.Mode.Dev)
 * }}}
 *
 * This will create an application using the current classloader.
 *
 * @param path the absolute path hosting this application, mainly used by the `getFile(path)` helper method
 * @param classloader the application's classloader
 * @param sources the `SourceMapper` used to retrieve source code displayed in error pages
 * @param mode `Dev` or `Prod`, passed as information for the user code
 */
@implicitNotFound(msg = "You do not have an implicit Application in scope. If you want to bring the current running Application into context, just add import play.api.Play.current")
trait Application {

  def path: File
  def classloader: ClassLoader
  def sources: Option[SourceMapper]
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
  def plugin[T](implicit m: Manifest[T]): Option[T] = plugin(m.erasure).asInstanceOf[Option[T]]


  /**
   * The router used by this application (if defined).
   */
  lazy val routes: Option[Router.Routes] = try {
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
    case e => throw e
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
  private[play] def handleError(request: RequestHeader, e: Throwable): Result = try {
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
    case e => try {
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
      case e => DefaultGlobal.onError(request, e)
    }
  }

  /**
   * Retrieves a file relative to the application root path.
   *
   * For example, to retrieve a configuration file:
   * {{{
   * val myConf = application.getFile("conf/myConf.yml")
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
   * For example, to retrieve a configuration file:
   * {{{
   * val myConf = application.getExistingFile("conf/myConf.yml")
   * }}}
   *
   * @param relativePath the relative path of the file to fetch
   * @return an existing file
   */
  def getExistingFile(relativePath: String): Option[File] = Option(getFile(relativePath)).filter(_.exists)

  /**
   * Scans the application classloader to retrieve all types within a specific package.
   *
   * This method is useful for some plugins, for example the EBean plugin will automatically detect all types
   * within the models package, using:
   * {{{
   * val entities = application.getTypes("models")
   * }}}
   *
   * Note that it is better to specify a very specific package to avoid expensive searches.
   *
   * @param packageName the root package to scan
   * @return a set of types names specifying the condition
   */
  def getTypes(packageName: String): Set[String] = {
    import org.reflections._
    new Reflections(
      new util.ConfigurationBuilder()
        .addUrls(util.ClasspathHelper.forPackage(packageName, classloader))
        .filterInputsBy(new util.FilterBuilder().include(util.FilterBuilder.prefix(packageName + ".")))
        .setScanners(new scanners.TypesScanner())).getStore.get(classOf[scanners.TypesScanner]).keySet.asScala.toSet
  }

  /**
   * Scans the application classloader to retrieve all types annotated with a specific annotation.
   *
   * This method is useful for some plugins, for example the EBean plugin will automatically detect all types
   * annotated with `@javax.persistance.Entity`, using:
   * {{{
   * val entities = application.getTypesAnnotatedWith("models", classOf[javax.persistance.Entity])
   * }}}
   *
   * Note that it is better to specify a very specific package to avoid expensive searches.
   *
   * @tparam T the annotation type
   * @param packageName the root package to scan
   * @param annotation the annotation class
   * @return a set of types names specifying the condition
   */
  def getTypesAnnotatedWith[T <: java.lang.annotation.Annotation](packageName: String, annotation: Class[T]): Set[String] = {
    import org.reflections._
    new Reflections(
      new util.ConfigurationBuilder()
        .addUrls(util.ClasspathHelper.forPackage(packageName, classloader))
        .setScanners(new scanners.TypeAnnotationsScanner())).getStore.getTypesAnnotatedWith(annotation.getName).asScala.toSet
  }

  /**
   * Scans the application classloader to retrieve a resource.
   *
   * For example, to retrieve a configuration file:
   * {{{
   * val maybeConf = application.resource("conf/logger.xml")
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
   * For example, to retrieve a configuration file:
   * {{{
   * val maybeConf = application.resourceAsStream("conf/logger.xml")
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
  override val mode: Mode.Mode
) extends Application with WithDefaultConfiguration with WithDefaultGlobal with WithDefaultPlugins
