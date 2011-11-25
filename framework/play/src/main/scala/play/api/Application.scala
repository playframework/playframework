package play.api

import play.core._

import play.api.mvc._

import java.io._

import scala.collection.JavaConverters._

/**
 * A Play application.
 *
 * Application creation is handled by the framework engine. If you need to create an ad-hoc application,
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
 * @param mode `DEV` or `PROD`, passed as information for the user code
 */
case class Application(path: File, classloader: ApplicationClassLoader, sources: Option[SourceMapper], mode: Play.Mode.Mode) {

  private val initialConfiguration = getExistingFile("conf/application.conf").map(Configuration.fromFile).getOrElse(Configuration.empty)

  // -- Global stuff

  private val globalClass = initialConfiguration.getString("global").getOrElse("Global")

  lazy private val javaGlobal: Option[play.GlobalSettings] = try {
    Option(classloader.loadClassParentLast(globalClass).newInstance().asInstanceOf[play.GlobalSettings])
  } catch {
    case e: InstantiationException => None
    case e: ClassNotFoundException => None
    case e => throw e
  }

  lazy private val scalaGlobal: GlobalSettings = try {
    classloader.loadClassParentLast(globalClass + "$").getDeclaredField("MODULE$").get(null).asInstanceOf[GlobalSettings]
  } catch {
    case e: ClassNotFoundException if !initialConfiguration.getString("global").isDefined => DefaultGlobal
    case e if initialConfiguration.getString("global").isDefined => {
      throw initialConfiguration.reportError("global", "Cannot init the Global object (%s)".format(e.getMessage))
    }
    case e => throw e
  }

  /**
   * The global settings used by this application.
   *
   * @see play.api.GlobalSettings
   */
  val global: GlobalSettings = try {
    javaGlobal.map(new j.JavaGlobalSettingsAdapter(_)).getOrElse(scalaGlobal)
  } catch {
    case e: PlayException => throw e
    case e => throw PlayException(
      "Cannot init the Global object",
      e.getMessage,
      Some(e))
  }

  /**
   * The configuration used by this application.
   *
   * @see play.api.Configuration
   */
  val configuration = global.configuration ++ initialConfiguration

  /** The router used by this application. */
  val routes: Option[Router.Routes] = try {
    Some(classloader.loadClassParentLast("Routes$").getDeclaredField("MODULE$").get(null).asInstanceOf[Router.Routes])
  } catch {
    case e: ClassNotFoundException => None
    case e => throw e
  }

  // Reconfigure logger
  {

    val validValues = Some(Set("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF", "INHERITED"))
    val setLevel = (level: String) => level match {
      case "INHERITED" => null
      case level => ch.qos.logback.classic.Level.toLevel(level)
    }

    Logger.configure(
      getExistingFile("conf/logger.xml").map(_.toURI.toURL).getOrElse {
        resource("conf/logger.xml").getOrElse(null)
      },
      Map("application.home" -> path.getAbsolutePath),
      configuration.getSub("logger").map { loggerConfig =>
        loggerConfig.keys.map { key =>
          key -> loggerConfig.getString(key, validValues).map(setLevel).get
        }.toMap
      }.getOrElse(Map.empty) ++ {
        configuration.getString("logger", validValues).map(setLevel).map(rootLevel => Map("ROOT" -> rootLevel)).getOrElse(Map.empty)
      })

  }

  private[api] val dynamicPlugins: List[String] = Nil

  /**
   * The plugins list used by this application.
   *
   * @see play.api.Plugin
   */
  lazy val plugins: Seq[Plugin] = {

    import scalax.file._
    import scalax.io.JavaConverters._
    import scala.collection.JavaConverters._

    val PluginDeclaration = """([0-9_]+):(.*)""".r

    val pluginFiles = classloader.getResources("play.plugins").asScala.toList ++ classloader.getResources("conf/play.plugins").asScala.toList

    pluginFiles.distinct.map { plugins =>
      (dynamicPlugins ++ plugins.asInput.slurpString.split("\n").map(_.trim)).filterNot(_.isEmpty).map {
        case PluginDeclaration(priority, className) => {
          try {
            val plugin = Integer.parseInt(priority) -> classloader.loadClass(className).getConstructor(classOf[Application]).newInstance(this).asInstanceOf[Plugin]
            if (plugin._2.enabled) Some(plugin) else { Logger("play").debug("Plugin [" + className + "] is disabled"); None }
          } catch {
            case e: java.lang.NoSuchMethodException => {
              try {
                val plugin = Integer.parseInt(priority) -> classloader.loadClass(className).getConstructor(classOf[play.Application]).newInstance(new play.Application(this)).asInstanceOf[Plugin]
                if (plugin._2.enabled) Some(plugin) else { Logger("play").warn("Plugin [" + className + "] is disabled"); None }
              } catch {
                case e: PlayException => throw e
                case e => throw PlayException(
                  "Cannot load plugin",
                  "Plugin [" + className + "] cannot been instantiated.",
                  Some(e))
              }
            }
            case e: PlayException => throw e
            case e => throw PlayException(
              "Cannot load plugin",
              "Plugin [" + className + "] cannot been instantiated.",
              Some(e))
          }
        }
      }
    }.flatten.toList.flatten.sortBy(_._1).map(_._2)

  }

  /**
   * Retrieves a plugin of type `T`.
   *
   * For example, to retrieve the DBPlugin instance:
   * {{{
   * val dbPlugin = application.plugin[DBPlugin].map(_.api).getOrElse(throw new exception("problem with the plugin"))
   * }}}
   *
   * @tparam T the plugin type
   * @return The plugin instance used by this application.
   * @throws Error if no plugins of type T are loaded by this application.
   */
  def plugin[T](implicit m: Manifest[T]): Option[T] = plugin(m.erasure).asInstanceOf[Option[T]]

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
