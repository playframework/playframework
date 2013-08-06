package play.api

import scala.collection.mutable
import scala.util.Try

import java.lang.reflect.InvocationTargetException

import play.utils.{ PlayIO, Threads }
import play.core.j

/**
 * Provides injection of plugins, controllers, and other dependencies used by the application.
 *
 * The implementation of InjectionProvider is defined in application.conf, e.g.:
 *
 * {{{
 * application.injectionprovider=com.example.MyInjectionProvider
 * }}}
 *
 * The implementation must have a constructor which takes an Application as its only argument.
 *
 * DefaultInjectionProvider is used if no InjectionProvider is defined in the configuration.
 */
trait InjectionProvider {
  /**
   * Get all the plugin instances used by this application.
   *
   * @return The list of plugin instances used by the application
   */
  def plugins: Seq[Plugin]

  /**
   * Retrieves an instance of type `T`.
   *
   * @tparam T the instance type
   * @param  clazz the class
   * @return a Try containing either an instance of the class or an exception thrown while trying to get one.
   */
  def getInstance[T](clazz: Class[T]): Try[T]

  /**
   * Retrieves the plugin of type `T` used by the application.
   *
   * @tparam T the plugin type
   * @param  clazz the plugin’s class
   * @return the plugin instance wrapped in an option if it's loaded by this provider, None otherwise.
   */
  def getPlugin[T <: Plugin](clazz: Class[T]): Option[T]

  /**
   * Returns the `GlobalSettings` instance used by this application, creating it if necessary.
   *
   * @return the `GlobalSettings` instance.
   */
  def global: GlobalSettings
}

object InjectionProvider {

  /**
   * Load the plugin class names from the play.plugins file.
   *
   * @param app the application
   * @return a list of class names
   */
  def loadPluginClassNames(app: Application): Seq[String] = {
    import scala.collection.JavaConverters._

    val PluginDeclaration = """([0-9_]+):(.*)""".r
    val pluginFiles = app.classloader.getResources("play.plugins").asScala.toList ++ app.classloader.getResources("conf/play.plugins").asScala.toList

    pluginFiles.distinct.map { plugins =>
      PlayIO.readUrlAsString(plugins).split("\n").map(_.replaceAll("#.*$", "").trim).filterNot(_.isEmpty).map {
        case PluginDeclaration(priority, className) => (priority.toInt, className)
      }
    }.flatten.sortBy(_._1).map(_._2)
  }

  /**
   * Instantiate the given plugins for this application, assuming each plugin has a constructor which takes the
   * application as its only parameter.
   *
   * @param app the application to load the plugins for
   * @param classNames a list of plugin classes to load
   * @return a list of loaded plugins
   */
  def loadPlugins(app: Application, classNames: Seq[String]): Seq[Plugin] = {
    Threads.withContextClassLoader(app.classloader) {
      classNames.map { className =>
        try {
          val plugin = app.classloader.loadClass(className).getConstructor(classOf[Application]).newInstance(app).asInstanceOf[Plugin]
          if (plugin.enabled) Some(plugin) else { Play.logger.debug("Plugin [" + className + "] is disabled"); None }
        } catch {
          case e: java.lang.NoSuchMethodException => {
            try {
              val plugin = app.classloader.loadClass(className).getConstructor(classOf[play.Application]).newInstance(new play.Application(app)).asInstanceOf[Plugin]
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
                "Plugin [" + className + "] cannot been instantiated.",
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
   * Load the plugins for this application defined in the play.plugins file.
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
   * @param app the application
   * @return a list of loaded plugins
   */
  def loadPlugins(app: Application): Seq[Plugin] = loadPlugins(app, loadPluginClassNames(app))

  /**
   * Create and load the global for this application by reading the configuration
   *
   * @param app the application for which to create the global
   * @return the newly created `GlobalSettings` instance.
   */
  def loadGlobal(app: Application): GlobalSettings = {
    val initialConfiguration = app match {
      case a: WithDefaultConfiguration => a.initialConfiguration
      case _ => Configuration.empty
    }
    lazy val globalClass = initialConfiguration.getString("application.global")
      .getOrElse(initialConfiguration.getString("global").map { g =>
        Play.logger.warn("`global` key is deprecated, please change `global` key to `application.global`")
        g
      }.getOrElse("Global"))

    lazy val javaGlobal: Option[play.GlobalSettings] = try {
      Option(app.classloader.loadClass(globalClass).newInstance().asInstanceOf[play.GlobalSettings])
    } catch {
      case e: InstantiationException => None
      case e: ClassNotFoundException => None
    }

    lazy val scalaGlobal: GlobalSettings = try {
      app.classloader.loadClass(globalClass + "$").getDeclaredField("MODULE$").get(null).asInstanceOf[GlobalSettings]
    } catch {
      case e: ClassNotFoundException if !initialConfiguration.getString("application.global").isDefined => DefaultGlobal
      case e if initialConfiguration.getString("application.global").isDefined => {
        throw initialConfiguration.reportError("application.global",
          s"Cannot initialize the custom Global object ($globalClass) (perhaps it's a wrong reference?)", Some(e))
      }
    }

    Threads.withContextClassLoader(app.classloader) {
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
  }
}

/**
 * A default implementation of InjectionProvider used by Play, which loads plugins from the play.plugins file and
 * loads the global based on the application.global key in the configuration.
 *
 * @param app the application
 */
class DefaultInjectionProvider(app: Application) extends InjectionProvider {
  private lazy val loadedPlugins = InjectionProvider.loadPlugins(app)
  private lazy val globalInstance = InjectionProvider.loadGlobal(app)
  private lazy val classToPlugin = mutable.Map[Class[_], Option[_]]()

  def plugins: Seq[Plugin] = loadedPlugins

  def global: GlobalSettings = globalInstance

  def getInstance[T](clazz: Class[T]): Try[T] = Try(clazz match {
    case c if classOf[Plugin] isAssignableFrom clazz =>
      getPlugin(c.asInstanceOf[Class[Plugin]]) getOrElse c.newInstance()
    case c => c.newInstance()
  }).asInstanceOf[Try[T]]

  def getPlugin[T <: Plugin](clazz: Class[T]): Option[T] =
    classToPlugin.getOrElseUpdate(clazz, plugins.find(clazz isAssignableFrom _.getClass)).asInstanceOf[Option[T]]
}
