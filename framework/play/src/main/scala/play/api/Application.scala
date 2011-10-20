package play.api

import play.core._

import play.api.mvc._

import java.io._

import scala.collection.JavaConverters._

/**
 * A Play application.
 *
 * Application creation is handled by the framework engine. If you need to create an adhoc application
 * for example in case of unit testing, you can easily achieve it using:
 *
 * {{{
 * val application = Application(new File("."), this.getClass.getClassloader, None, Play.Mode.DEV)
 * }}}
 *
 * It will create an application using the current classloader.
 *
 * @param path The absolute path hosting this application. Mainly used by the getFile(path) helper method.
 * @param classloader Application's classloader.
 * @param sources SourceMapper used to retrieve source code displayed in error pages.
 * @param mode DEV or PROD, passed as information for the user code.
 */
case class Application(path: File, classloader: ApplicationClassLoader, sources: Option[SourceMapper], mode: Play.Mode.Mode) {

  // Reconfigure logger
  Logger.configure(
    getExistingFile("conf/logger.xml").map(_.toURI.toURL).getOrElse {
      resource("conf/logger.xml").getOrElse(null)
    },
    Map("application.home" -> path.getAbsolutePath))

  /**
   * The global settings used by this application.
   * @see play.api.GlobalSettings
   */
  private val javaGlobal: Option[play.GlobalSettings] = try {
    // lookup java application Global
    Option(classloader.loadClassParentLast("Global").newInstance().asInstanceOf[play.GlobalSettings])
  } catch {
    case e: InstantiationException => None
    case e: ClassNotFoundException => None
    case e => throw e
  }

  private val scalaGlobal: GlobalSettings = try {
    // lookup application's global.scala
    classloader.loadClassParentLast("Global$").getDeclaredField("MODULE$").get(null).asInstanceOf[GlobalSettings]
  } catch {
    case e: ClassNotFoundException => DefaultGlobal
    case e => throw e
  }

  val global: GlobalSettings = javaGlobal.map(new JavaGlobalSettingsAdapter(_)).getOrElse(scalaGlobal)

  println("Using GlobalSettings " + global.getClass.getName)

  global.beforeStart(this)

  /**
   * The router used by this application.
   */
  val routes: Option[Router.Routes] = try {
    Some(classloader.loadClassParentLast("Routes$").getDeclaredField("MODULE$").get(null).asInstanceOf[Router.Routes])
  } catch {
    case e: ClassNotFoundException => None
    case e => throw e
  }

  /**
   * The configuration used by this application.
   * @see play.api.Configuration
   */
  val configuration = Configuration.fromFile(new File(path, "conf/application.conf"))

  /**
   * The plugins list used by this application.
   * @see play.api.Plugin
   */
  val plugins: Map[Class[_], Plugin] = {

    import scalax.file._
    import scalax.io.JavaConverters._
    import scala.collection.JavaConverters._

    val PluginDeclaration = """([0-9_]+):(.*)""".r

    classloader.getResources("play.plugins").asScala.toList.distinct.map { plugins =>
      plugins.asInput.slurpString.split("\n").map(_.trim).filterNot(_.isEmpty).map {
        case PluginDeclaration(priority, className) => {
          try {
            Integer.parseInt(priority) -> classloader.loadClass(className).getConstructor(classOf[Application]).newInstance(this).asInstanceOf[Plugin]
          } catch {
            case e: java.lang.NoSuchMethodException => {
              try {
                Integer.parseInt(priority) -> classloader.loadClass(className).getConstructor(classOf[play.Application]).newInstance(new play.Application(this)).asInstanceOf[Plugin]
              } catch {
                case e => throw PlayException(
                  "Cannot load plugin",
                  "Plugin [" + className + "] cannot been instantiated.",
                  Some(e))
              }
            }
            case e => throw PlayException(
              "Cannot load plugin",
              "Plugin [" + className + "] cannot been instantiated.",
              Some(e))
          }
        }
      }
    }.flatten.toList.sortBy(_._1).map(_._2).map(p => p.getClass -> p).toMap

  }

  /**
   * Retrieve a plugin of type T.
   *
   * For example, retrieving the DBPlugin instance:
   * {{{
   * val dbPlugin = application.plugin[DBPlugin]
   * }}}
   *
   * @tparam T Plugin type.
   * @return The plugin instance used by this application.
   * @throws Error if no plugins of type T are loaded by this application.
   */
  def plugin[T](implicit m: Manifest[T]): T = plugin(m.erasure).asInstanceOf[T]

  /**
   * Retrieve a plugin of type T.
   *
   * For example, retrieving the DBPlugin instance:
   * {{{
   * val dbPlugin = application.plugin(classOf[DBPlugin])
   * }}}
   *
   * @tparam T Plugin type.
   * @param  pluginClass Plugin's class
   * @return The plugin instance used by this application.
   * @throws Error if no plugins of type T are loaded by this application.
   */
  def plugin[T](pluginClass: Class[T]): T = plugins.get(pluginClass).get.asInstanceOf[T]

  /**
   * Retrieve a file relatively to the application root path.
   *
   * For example to retrieve a configuration file:
   * {{{
   * val myConf = application.getFile("conf/myConf.yml")
   * }}}
   *
   * @param relativePath Relative path of the file to fetch
   * @return A file instance, but it is not guaranteed that the file exist.
   */
  def getFile(relativePath: String): File = new File(path, relativePath)

  /**
   * Retrieve a file relatively to the application root path.
   *
   * For example to retrieve a configuration file:
   * {{{
   * val myConf = application.getFile("conf/myConf.yml")
   * }}}
   *
   * @param relativePath Relative path of the file to fetch
   * @return Maybe an existing file.
   */
  def getExistingFile(relativePath: String): Option[File] = Option(getFile(relativePath)).filter(_.exists)

  /**
   * Scan the application classloader to retrieve all types annotated with a specific annotation.
   *
   * This method is useful for some plugins, for example the EBean plugin will automatically detect all types
   * annotated with @javax.persistance.Entity, using:
   * {{{
   * val entities = application.getTypesAnnotatedWith("models", classOf[javax.persistance.Entity])
   * }}}
   *
   * Note that it is better to specify a very specific package to avoid too expensive searchs.
   *
   * @tparam T The annotation type
   * @param packageName The root package to scan,
   * @param annotation Annotation class.
   * @return A set of types names statifying the condition.
   */
  def getTypesAnnotatedWith[T <: java.lang.annotation.Annotation](packageName: String, annotation: Class[T]): Set[String] = {
    import org.reflections._
    new Reflections(
      new util.ConfigurationBuilder()
        .addUrls(util.ClasspathHelper.forPackage(packageName, classloader))
        .setScanners(new scanners.TypeAnnotationsScanner())).getStore.getTypesAnnotatedWith(annotation.getName).asScala.toSet
  }

  def resource(name: String): Option[java.net.URL] = {
    Option(classloader.getResource(Option(name).map {
      case s if s.startsWith("/") => s.drop(1)
      case s => s
    }.get))
  }

  def resourceAsStream(name: String): Option[InputStream] = {
    Option(classloader.getResourceAsStream(Option(name).map {
      case s if s.startsWith("/") => s.drop(1)
      case s => s
    }.get))
  }

}