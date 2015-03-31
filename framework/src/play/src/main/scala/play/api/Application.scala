/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import javax.inject.Inject

import com.google.inject.Singleton
import play.api.http._
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.api.inject.{ SimpleInjector, NewInstanceInjector, Injector, DefaultApplicationLifecycle }
import play.api.libs.{ Crypto, CryptoConfigParser, CryptoConfig }
import play.core.{ SourceMapper, WebCommands }
import play.utils._

import java.io._

import annotation.implicitNotFound

import reflect.ClassTag
import scala.concurrent.Future

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
   * `Dev`, `Prod` or `Test`
   */
  def mode: Mode.Mode

  def global: GlobalSettings = injector.instanceOf[GlobalSettings]
  def configuration: Configuration
  def plugins: Seq[Plugin.Deprecated]

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
   * Cached value of `routes`. For performance, don't synchronize
   * the value. We always use the same logic to calculate its value
   * so it will end up consistent across threads anyway.
   */
  private var cachedRoutes: Router = null

  /**
   * The router used by this application.
   */
  @deprecated("Either use HttpRequestHandler, or have the router injected", "2.4.0")
  def routes: Router = {
    // Use a cached value because the injector might be slow
    if (cachedRoutes != null) cachedRoutes else {
      cachedRoutes = injector.instanceOf[Router]
      cachedRoutes
    }
  }

  /**
   * The HTTP request handler
   */
  def requestHandler: HttpRequestHandler

  /**
   * The HTTP error handler
   */
  def errorHandler: HttpErrorHandler

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

  /**
   * Stop the application.  The returned future will be redeemed when all stop hooks have been run.
   */
  def stop(): Future[Unit]

  /**
   * Get the injector for this application.
   *
   * @return The injector.
   */
  def injector: Injector = NewInstanceInjector
}

object Application {
  /**
   * Creates a function that caches results of calls to
   * `app.injector.instanceOf[T]`. The cache speeds up calls
   * when called with the same Application each time, which is
   * a big benefit in production. It still works properly if
   * called with a different Application each time, such as
   * when running unit tests, but it will run more slowly.
   *
   * Since values are cached, it's important that this is only
   * used for singleton values.
   *
   * This method avoids synchronization so it's possible that
   * the injector might be called more than once for a single
   * instance if this method is called from different threads
   * at the same time.
   *
   * The cache uses a WeakReference to both the Application and
   * the returned instance so it will not cause memory leaks.
   * Unlike WeakHashMap it doesn't use a ReferenceQueue, so values
   * will still be cleaned even if the ReferenceQueue is never
   * activated.
   */
  def instanceCache[T: ClassTag]: Application => T =
    new InlineCache((app: Application) => app.injector.instanceOf[T])
}

class OptionalSourceMapper(val sourceMapper: Option[SourceMapper])

@Singleton
class DefaultApplication @Inject() (environment: Environment,
    applicationLifecycle: DefaultApplicationLifecycle,
    override val injector: Injector,
    override val configuration: Configuration,
    override val requestHandler: HttpRequestHandler,
    override val errorHandler: HttpErrorHandler,
    override val plugins: Plugins) extends Application {

  def path = environment.rootPath

  def classloader = environment.classLoader

  def mode = environment.mode

  def stop() = applicationLifecycle.stop()
}

/**
 * Helper to provide the Play built in components.
 */
trait BuiltInComponents {
  def environment: Environment
  def sourceMapper: Option[SourceMapper]
  def webCommands: WebCommands
  def configuration: Configuration

  def router: Router

  lazy val injector: Injector = new SimpleInjector(NewInstanceInjector) + router + crypto + httpConfiguration

  lazy val httpConfiguration: HttpConfiguration = HttpConfiguration.fromConfiguration(configuration)
  lazy val httpRequestHandler: HttpRequestHandler = new DefaultHttpRequestHandler(router, httpErrorHandler, httpConfiguration, httpFilters: _*)
  lazy val httpErrorHandler: HttpErrorHandler = new DefaultHttpErrorHandler(environment, configuration, sourceMapper,
    Some(router))
  lazy val httpFilters: Seq[EssentialFilter] = Nil

  lazy val applicationLifecycle: DefaultApplicationLifecycle = new DefaultApplicationLifecycle
  lazy val application: Application = new DefaultApplication(environment, applicationLifecycle, injector,
    configuration, httpRequestHandler, httpErrorHandler, Plugins.empty)

  lazy val cryptoConfig: CryptoConfig = new CryptoConfigParser(environment, configuration).get
  lazy val crypto: Crypto = new Crypto(cryptoConfig)
}
