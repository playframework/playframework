/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import java.io._

import akka.actor.ActorSystem
import akka.actor.CoordinatedShutdown
import akka.stream.Materializer
import javax.inject.Inject
import javax.inject.Singleton
import play.api.ApplicationLoader.DevContext
import play.api.http._
import play.api.i18n.I18nComponents
import play.api.inject.ApplicationLifecycle
import play.api.inject._
import play.api.internal.libs.concurrent.CoordinatedShutdownSupport
import play.api.libs.Files._
import play.api.libs.concurrent.AkkaComponents
import play.api.libs.concurrent.AkkaTypedComponents
import play.api.libs.concurrent.CoordinatedShutdownProvider
import play.api.libs.crypto._
import play.api.mvc._
import play.api.mvc.request.DefaultRequestFactory
import play.api.mvc.request.RequestFactory
import play.api.routing.Router
import play.core.j.JavaContextComponents
import play.core.j.JavaHelpers
import play.core.DefaultWebCommands
import play.core.SourceMapper
import play.core.WebCommands
import play.utils._

import scala.annotation.implicitNotFound
import scala.concurrent.Future
import scala.reflect.ClassTag

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
@implicitNotFound(
  msg = "You do not have an implicit Application in scope. If you want to bring the current running Application into context, please use dependency injection."
)
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
  def mode: Mode = environment.mode

  /**
   * The application's environment
   */
  def environment: Environment

  private[play] def isDev  = (mode == Mode.Dev)
  private[play] def isTest = (mode == Mode.Test)
  private[play] def isProd = (mode == Mode.Prod)

  def configuration: Configuration

  private[play] lazy val httpConfiguration =
    HttpConfiguration.fromConfiguration(configuration, environment)

  /**
   * The default ActorSystem used by the application.
   */
  def actorSystem: ActorSystem

  /**
   * The default Materializer used by the application.
   */
  implicit def materializer: Materializer

  /**
   * The default CoordinatedShutdown to stop the Application
   */
  def coordinatedShutdown: CoordinatedShutdown

  /**
   * The factory used to create requests for this application.
   */
  def requestFactory: RequestFactory

  /**
   * The HTTP request handler
   */
  def requestHandler: HttpRequestHandler

  /**
   * The HTTP error handler
   */
  def errorHandler: HttpErrorHandler

  /**
   * Return the application as a Java application.
   */
  def asJava: play.Application = {
    new play.DefaultApplication(this, configuration.underlying, injector.asJava, environment.asJava)
  }

  /**
   * Stop the application.  The returned future will be redeemed when all stop hooks have been run.
   */
  def stop(): Future[_]

  /**
   * Get the runtime injector for this application. In a runtime dependency injection based application, this can be
   * used to obtain components as bound by the DI framework.
   *
   * @return The injector.
   */
  def injector: Injector = NewInstanceInjector

  /**
   * Returns true if the global application is enabled for this app. If set to false, this changes the behavior of
   * Play.start to disallow access to the global application instance,
   * also affecting the deprecated Play APIs that use these.
   */
  lazy val globalApplicationEnabled: Boolean = {
    configuration.getOptional[Boolean](Play.GlobalAppConfigKey).getOrElse(true)
  }
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
   * The cache uses a SoftReference to both the Application and
   * the returned instance so it will not cause memory leaks.
   * Unlike WeakHashMap it doesn't use a ReferenceQueue, so values
   * will still be cleaned even if the ReferenceQueue is never
   * activated.
   */
  def instanceCache[T: ClassTag]: Application => T =
    new InlineCache((app: Application) => app.injector.instanceOf[T])
}

@Singleton
class DefaultApplication @Inject() (
    override val environment: Environment,
    applicationLifecycle: ApplicationLifecycle,
    override val injector: Injector,
    override val configuration: Configuration,
    override val requestFactory: RequestFactory,
    override val requestHandler: HttpRequestHandler,
    override val errorHandler: HttpErrorHandler,
    override val actorSystem: ActorSystem,
    override val materializer: Materializer,
    override val coordinatedShutdown: CoordinatedShutdown
) extends Application {
  def this(
      environment: Environment,
      applicationLifecycle: ApplicationLifecycle,
      injector: Injector,
      configuration: Configuration,
      requestFactory: RequestFactory,
      requestHandler: HttpRequestHandler,
      errorHandler: HttpErrorHandler,
      actorSystem: ActorSystem,
      materializer: Materializer
  ) = this(
    environment,
    applicationLifecycle,
    injector,
    configuration,
    requestFactory,
    requestHandler,
    errorHandler,
    actorSystem,
    materializer,
    new CoordinatedShutdownProvider(actorSystem, applicationLifecycle).get
  )

  override def path: File = environment.rootPath

  override def classloader: ClassLoader = environment.classLoader

  override def stop(): Future[_] =
    CoordinatedShutdownSupport.asyncShutdown(actorSystem, ApplicationStoppedReason)
}

private[play] final case object ApplicationStoppedReason extends CoordinatedShutdown.Reason

/**
 * Helper to provide the Play built in components.
 */
trait BuiltInComponents extends I18nComponents with AkkaComponents with AkkaTypedComponents {

  /** The application's environment, e.g. it's [[ClassLoader]] and root path. */
  def environment: Environment

  /** Helper to locate the source code for the application. Only available in dev mode. */
  @deprecated("Use devContext.map(_.sourceMapper) instead", "2.7.0")
  def sourceMapper: Option[SourceMapper] = devContext.map(_.sourceMapper)

  /** Helper to interact with the Play build environment. Only available in dev mode. */
  def devContext: Option[DevContext] = None

  // Define a private val so that webCommands can remain a `def` instead of a `val`
  private val defaultWebCommands: WebCommands = new DefaultWebCommands

  /** Commands that intercept requests before the rest of the application handles them. Used by Evolutions. */
  def webCommands: WebCommands = defaultWebCommands

  /** The application's configuration. */
  def configuration: Configuration

  /** A registry to receive application lifecycle events, e.g. to close resources when the application stops. */
  def applicationLifecycle: ApplicationLifecycle

  /** The router that's used to pass requests to the correct handler. */
  def router: Router

  /**
   * The runtime [[Injector]] instance provided to the [[DefaultApplication]]. This injector is set up to allow
   * existing (deprecated) legacy APIs to function. It is not set up to support injecting arbitrary Play components.
   */
  lazy val injector: Injector = {
    val simple = new SimpleInjector(NewInstanceInjector) +
      cookieSigner +      // play.api.libs.Crypto (for cookies)
      httpConfiguration + // play.api.mvc.BodyParsers trait
      tempFileCreator +   // play.api.libs.TemporaryFileCreator object
      messagesApi +       // play.api.i18n.Messages object
      langs               // play.api.i18n.Langs object
    new ContextClassLoaderInjector(simple, environment.classLoader)
  }

  lazy val playBodyParsers: PlayBodyParsers =
    PlayBodyParsers(tempFileCreator, httpErrorHandler, httpConfiguration.parser)(materializer)
  lazy val defaultBodyParser: BodyParser[AnyContent]  = playBodyParsers.default
  lazy val defaultActionBuilder: DefaultActionBuilder = DefaultActionBuilder(defaultBodyParser)

  lazy val httpConfiguration: HttpConfiguration =
    HttpConfiguration.fromConfiguration(configuration, environment)
  lazy val requestFactory: RequestFactory = new DefaultRequestFactory(httpConfiguration)
  lazy val httpErrorHandler: HttpErrorHandler =
    new DefaultHttpErrorHandler(environment, configuration, devContext.map(_.sourceMapper), Some(router))

  /**
   * List of filters, typically provided by mixing in play.filters.HttpFiltersComponents
   * or play.api.NoHttpFiltersComponents.
   *
   * In most cases you will want to mixin HttpFiltersComponents and append your own filters:
   *
   * {{{
   * class MyComponents(context: ApplicationLoader.Context)
   *   extends BuiltInComponentsFromContext(context)
   *   with play.filters.HttpFiltersComponents {
   *
   *   lazy val loggingFilter = new LoggingFilter()
   *   override def httpFilters = {
   *     super.httpFilters :+ loggingFilter
   *   }
   * }
   * }}}
   *
   * If you want to filter elements out of the list, you can do the following:
   *
   * {{{
   * class MyComponents(context: ApplicationLoader.Context)
   *   extends BuiltInComponentsFromContext(context)
   *   with play.filters.HttpFiltersComponents {
   *   override def httpFilters = {
   *     super.httpFilters.filterNot(_.getClass == classOf[CSRFFilter])
   *   }
   * }
   * }}}
   */
  def httpFilters: Seq[EssentialFilter]

  lazy val httpRequestHandler: HttpRequestHandler =
    new DefaultHttpRequestHandler(
      webCommands,
      devContext,
      () => router,
      httpErrorHandler,
      httpConfiguration,
      httpFilters
    )

  lazy val application: Application = new DefaultApplication(
    environment,
    applicationLifecycle,
    injector,
    configuration,
    requestFactory,
    httpRequestHandler,
    httpErrorHandler,
    actorSystem,
    materializer,
    coordinatedShutdown
  )

  lazy val cookieSigner: CookieSigner = new CookieSignerProvider(httpConfiguration.secret).get

  lazy val csrfTokenSigner: CSRFTokenSigner = new CSRFTokenSignerProvider(cookieSigner).get

  lazy val tempFileReaper: TemporaryFileReaper =
    new DefaultTemporaryFileReaper(actorSystem, TemporaryFileReaperConfiguration.fromConfiguration(configuration))
  lazy val tempFileCreator: TemporaryFileCreator =
    new DefaultTemporaryFileCreator(applicationLifecycle, tempFileReaper, configuration)

  lazy val fileMimeTypes: FileMimeTypes = new DefaultFileMimeTypesProvider(httpConfiguration.fileMimeTypes).get

  @deprecated(
    "Use the corresponding methods that provide MessagesApi, Langs, FileMimeTypes or HttpConfiguration",
    "2.8.0"
  )
  lazy val javaContextComponents: JavaContextComponents =
    JavaHelpers.createContextComponents(messagesApi, langs, fileMimeTypes, httpConfiguration)

  // NOTE: the following helpers are declared as protected since they are only meant to be used inside BuiltInComponents
  // This also makes them not conflict with other methods of the same type when used with Macwire.

  /**
   * Alias method to [[defaultActionBuilder]]. This just helps to keep the idiom of using `Action`
   * when creating `Router`s using the built in components.
   *
   * @return the default action builder.
   */
  protected def Action: DefaultActionBuilder = defaultActionBuilder

  /**
   * Alias method to [[playBodyParsers]].
   */
  protected def parse: PlayBodyParsers = playBodyParsers
}

/**
 * A component to mix in when no default filters should be mixed in to BuiltInComponents.
 *
 * @see [[BuiltInComponents.httpFilters]]
 */
trait NoHttpFiltersComponents {
  val httpFilters: Seq[EssentialFilter] = Nil
}
