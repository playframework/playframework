/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.test

import javax.inject.{ Inject, Provider }

import play.api._
import play.api.http._
import play.api.inject.guice.GuiceApplicationLoader
import play.api.inject._
import play.api.libs.{ Crypto, CryptoConfigParser, CryptoConfig }
import play.api.mvc._
import play.api.libs.json.JsValue
import play.core.{ DefaultWebCommands, WebCommands }
import play.utils.{ Reflect, Threads }
import scala.concurrent.Future
import xml.NodeSeq
import play.core.Router
import scala.runtime.AbstractPartialFunction
import play.api.libs.Files.TemporaryFile

/**
 * Fake HTTP headers implementation.
 *
 * @param data Headers data.
 */
case class FakeHeaders(override val data: Seq[(String, Seq[String])] = Seq.empty) extends Headers

/**
 * Fake HTTP request implementation.
 *
 * @tparam A The body type.
 * @param method The request HTTP method.
 * @param uri The request uri.
 * @param headers The request HTTP headers.
 * @param body The request body.
 * @param remoteAddress The client IP.
 */
case class FakeRequest[A](method: String, uri: String, headers: FakeHeaders, body: A, remoteAddress: String = "127.0.0.1", version: String = "HTTP/1.1", id: Long = 666, tags: Map[String, String] = Map.empty[String, String], secure: Boolean = false) extends Request[A] {

  private def _copy[B](
    id: Long = this.id,
    tags: Map[String, String] = this.tags,
    uri: String = this.uri,
    path: String = this.path,
    method: String = this.method,
    version: String = this.version,
    headers: FakeHeaders = this.headers,
    remoteAddress: String = this.remoteAddress,
    secure: Boolean = this.secure,
    body: B = this.body): FakeRequest[B] = {
    new FakeRequest[B](
      method, uri, headers, body, remoteAddress, version, id, tags, secure
    )
  }

  /**
   * The request path.
   */
  lazy val path = uri.split('?').take(1).mkString

  /**
   * The request query String
   */
  lazy val queryString: Map[String, Seq[String]] =
    play.core.parsers.FormUrlEncodedParser.parse(rawQueryString)

  /**
   * Constructs a new request with additional headers. Any existing headers of the same name will be replaced.
   */
  def withHeaders(newHeaders: (String, String)*): FakeRequest[A] = {
    _copy(headers = FakeHeaders({
      val newData = newHeaders.map {
        case (k, v) => (k, Seq(v))
      }
      (Map() ++ (headers.data ++ newData)).toSeq
    }))
  }

  /**
   * Constructs a new request with additional Flash.
   */
  def withFlash(data: (String, String)*): FakeRequest[A] = {
    withHeaders(play.api.http.HeaderNames.COOKIE ->
      Cookies.merge(headers.get(play.api.http.HeaderNames.COOKIE).getOrElse(""),
        Seq(Flash.encodeAsCookie(new Flash(flash.data ++ data)))
      )
    )
  }

  /**
   * Constructs a new request with additional Cookies.
   */
  def withCookies(cookies: Cookie*): FakeRequest[A] = {
    withHeaders(play.api.http.HeaderNames.COOKIE ->
      Cookies.merge(headers.get(play.api.http.HeaderNames.COOKIE).getOrElse(""), cookies)
    )
  }

  /**
   * Constructs a new request with additional session.
   */
  def withSession(newSessions: (String, String)*): FakeRequest[A] = {
    withHeaders(play.api.http.HeaderNames.COOKIE ->
      Cookies.merge(headers.get(play.api.http.HeaderNames.COOKIE).getOrElse(""),
        Seq(Session.encodeAsCookie(new Session(session.data ++ newSessions)))
      )
    )
  }

  /**
   * Set a Form url encoded body to this request.
   */
  def withFormUrlEncodedBody(data: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = {
    _copy(body = AnyContentAsFormUrlEncoded(play.utils.OrderPreserving.groupBy(data.toSeq)(_._1)))
  }

  def certs = Future.successful(IndexedSeq.empty)

  /**
   * Adds a JSON body to the request.
   */
  def withJsonBody(json: JsValue): FakeRequest[AnyContentAsJson] = {
    _copy(body = AnyContentAsJson(json))
  }

  /**
   * Adds an XML body to the request.
   */
  def withXmlBody(xml: NodeSeq): FakeRequest[AnyContentAsXml] = {
    _copy(body = AnyContentAsXml(xml))
  }

  /**
   * Adds a text body to the request.
   */
  def withTextBody(text: String): FakeRequest[AnyContentAsText] = {
    _copy(body = AnyContentAsText(text))
  }

  /**
   * Adds a raw body to the request
   */
  def withRawBody(bytes: Array[Byte]): FakeRequest[AnyContentAsRaw] = {
    _copy(body = AnyContentAsRaw(RawBuffer(bytes.length, bytes)))
  }

  /**
   * Adds a multipart form data body to the request
   */
  def withMultipartFormDataBody(form: MultipartFormData[TemporaryFile]) = {
    _copy(body = AnyContentAsMultipartFormData(form))
  }

  /**
   * Adds a body to the request.
   */
  def withBody[B](body: B): FakeRequest[B] = {
    _copy(body = body)
  }

  /**
   * Returns the current method
   */
  def getMethod: String = method
}

/**
 * Helper utilities to build FakeRequest values.
 */
object FakeRequest {

  /**
   * Constructs a new GET / fake request.
   */
  def apply(): FakeRequest[AnyContentAsEmpty.type] = {
    FakeRequest("GET", "/", FakeHeaders(), AnyContentAsEmpty)
  }

  /**
   * Constructs a new request.
   */
  def apply(method: String, path: String): FakeRequest[AnyContentAsEmpty.type] = {
    FakeRequest(method, path, FakeHeaders(), AnyContentAsEmpty)
  }

  def apply(call: Call): FakeRequest[AnyContentAsEmpty.type] = {
    apply(call.method, call.url)
  }
}

/**
 * A Fake application.
 *
 * @param path The application path
 * @param classloader The application classloader
 * @param additionalPlugins Additional plugins class names loaded by this application
 * @param withoutPlugins Plugins class names to disable
 * @param additionalConfiguration Additional configuration
 * @param withRoutes A partial function of method name and path to a handler for handling the request
 */

import play.api.Application
case class FakeApplication(
    override val path: java.io.File = new java.io.File("."),
    override val classloader: ClassLoader = classOf[FakeApplication].getClassLoader,
    val additionalPlugins: Seq[String] = Nil,
    val withoutPlugins: Seq[String] = Nil,
    val additionalConfiguration: Map[String, _ <: Any] = Map.empty,
    val withGlobal: Option[play.api.GlobalSettings] = None,
    val withRoutes: PartialFunction[(String, String), Handler] = PartialFunction.empty) extends Application {

  private val environment = Environment(path, classloader, Mode.Test)
  private val initialConfiguration = Threads.withContextClassLoader(environment.classLoader) {
    Configuration.load(environment.rootPath, environment.mode)
  }
  override val global = withGlobal.getOrElse(GlobalSettings(initialConfiguration, environment))
  private val config =
    global.onLoadConfig(initialConfiguration, path, classloader, environment.mode) ++
      play.api.Configuration.from(additionalConfiguration)

  Logger.configure(environment, configuration)

  val applicationLifecycle = new DefaultApplicationLifecycle

  override val injector = {
    // Load all modules, and filter out the built in module
    val modules = Modules.locate(environment, configuration)
      .filterNot(_.getClass == classOf[BuiltinModule])
    val webCommands = new DefaultWebCommands

    val loader = config.getString("play.application.loader").fold[ApplicationLoader](
      new GuiceApplicationLoader
    ) { className =>
        Reflect.createInstance[ApplicationLoader](className, classloader)
      }

    loader.createInjector(environment, configuration,
      modules :+ new FakeBuiltinModule(environment, configuration, this, global, applicationLifecycle, webCommands, withRoutes)
    ).getOrElse(NewInstanceInjector)
  }

  def configuration = config

  def mode = environment.mode

  def stop() = applicationLifecycle.stop()

  override val errorHandler = injector.instanceOf[HttpErrorHandler]

  override val plugins = {
    Plugins.loadPlugins(
      additionalPlugins ++ Plugins.loadPluginClassNames(environment).diff(withoutPlugins),
      environment, injector
    )
  }

  override lazy val requestHandler = injector.instanceOf[HttpRequestHandler]
}

private class FakeRoutes(override val errorHandler: HttpErrorHandler,
    injected: PartialFunction[(String, String), Handler], fallback: Router.Routes) extends Router.Routes {
  def documentation = fallback.documentation
  // Use withRoutes first, then delegate to the parentRoutes if no route is defined
  val routes = new AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B >: Handler](rh: A, default: A => B) =
      injected.applyOrElse((rh.method, rh.path), (_: (String, String)) => default(rh))
    def isDefinedAt(rh: RequestHeader) = injected.isDefinedAt((rh.method, rh.path))
  } orElse new AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B >: Handler](rh: A, default: A => B) =
      fallback.routes.applyOrElse(rh, default)
    def isDefinedAt(x: RequestHeader) = fallback.routes.isDefinedAt(x)
  }
  def withPrefix(prefix: String) = {
    new FakeRoutes(errorHandler, injected, fallback.withPrefix(prefix))
  }
}

private class FakeBuiltinModule(environment: Environment,
    configuration: Configuration,
    app: Application,
    global: GlobalSettings,
    appLifecycle: ApplicationLifecycle,
    webCommands: WebCommands,
    withRoutes: PartialFunction[(String, String), Handler]) extends Module {
  def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[Environment] to environment,
    bind[Configuration] to configuration,
    bind[HttpConfiguration].toProvider[HttpConfiguration.HttpConfigurationProvider],
    bind[Application] to app,
    bind[GlobalSettings] to global,
    bind[ApplicationLifecycle] to appLifecycle,
    bind[WebCommands] to webCommands,
    bind[OptionalSourceMapper] to new OptionalSourceMapper(None),
    bind[play.inject.Injector].to[play.inject.DelegateInjector],
    bind[CryptoConfig].toProvider[CryptoConfigParser],
    bind[Crypto].toSelf,
    bind[Router.Routes].to(new FakeRoutesProvider(withRoutes))
  ) ++ HttpErrorHandler.bindingsFromConfiguration(environment, configuration) ++
    HttpFilters.bindingsFromConfiguration(environment, configuration) ++
    HttpRequestHandler.bindingsFromConfiguration(environment, configuration)
}

private class FakeRoutesProvider(withRoutes: PartialFunction[(String, String), Handler]) extends Provider[Router.Routes] {
  @Inject
  var injector: Injector = _
  lazy val get = {
    val parent = injector.instanceOf[RoutesProvider].get
    new FakeRoutes(injector.instanceOf[HttpErrorHandler], withRoutes, parent)
  }
}