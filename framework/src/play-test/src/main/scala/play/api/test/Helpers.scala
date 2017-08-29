/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.test

import java.nio.file.Path

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import akka.stream._
import akka.util.{ ByteString, Timeout }
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox._
import org.openqa.selenium.htmlunit._
import play.api._
import play.api.http._
import play.api.i18n._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.Files
import play.api.libs.json.{ JsValue, Json }
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.mvc.Http.RequestBody
import play.twirl.api.Content

import scala.concurrent.{ Await, ExecutionContext, ExecutionContextExecutor, Future }
import scala.concurrent.duration._
import scala.language.reflectiveCalls
import scala.reflect.ClassTag
import scala.util.Try

/**
 * Helper functions to run tests.
 */
trait PlayRunners extends HttpVerbs {

  val HTMLUNIT = classOf[HtmlUnitDriver]
  val FIREFOX = classOf[FirefoxDriver]

  /**
   * Returns `true` if this application needs to run sequentially, rather than in parallel with other applications.
   * Typically that means the application and/or test relies on some global state, so this defaults to the
   * globalApplicationEnabled setting. This method is provided so the behavior can be customized if needed.
   */
  protected def shouldRunSequentially(app: Application): Boolean = app.globalApplicationEnabled

  private[play] def runSynchronized[T](app: Application)(block: => T): T = {
    if (shouldRunSequentially(app)) {
      PlayRunners.mutex.synchronized(block)
    } else {
      block
    }
  }

  /**
   * The base builder used in the running method.
   */
  lazy val baseApplicationBuilder = new GuiceApplicationBuilder()

  def running[T]()(block: Application => T): T = {
    val app = baseApplicationBuilder.build()
    running(app)(block(app))
  }

  /**
   * Executes a block of code in a running application.
   */
  def running[T](app: Application)(block: => T): T = {
    runSynchronized(app) {
      try {
        Play.start(app)
        block
      } finally {
        Play.stop(app)
      }
    }
  }

  def running[T](builder: GuiceApplicationBuilder => GuiceApplicationBuilder)(block: Application => T): T = {
    val app = builder(baseApplicationBuilder).build()
    running(app)(block(app))
  }

  /**
   * Executes a block of code in a running server.
   */
  def running[T](testServer: TestServer)(block: => T): T = {
    runSynchronized(testServer.application) {
      try {
        testServer.start()
        block
      } finally {
        testServer.stop()
      }
    }
  }

  /**
   * Executes a block of code in a running server, with a test browser.
   */
  def running[T, WEBDRIVER <: WebDriver](testServer: TestServer, webDriver: Class[WEBDRIVER])(block: TestBrowser => T): T = {
    running(testServer, WebDriverFactory(webDriver))(block)
  }

  /**
   * Executes a block of code in a running server, with a test browser.
   */
  def running[T](testServer: TestServer, webDriver: WebDriver)(block: TestBrowser => T): T = {
    var browser: TestBrowser = null
    runSynchronized(testServer.application) {
      try {
        testServer.start()
        browser = TestBrowser(webDriver, None)
        block(browser)
      } finally {
        if (browser != null) {
          browser.quit()
        }
        testServer.stop()
      }
    }
  }

  /**
   * The port to use for a test server. Defaults to 19001. May be configured using the system property
   * testserver.port
   */
  lazy val testServerPort: Int = Option(System.getProperty("testserver.port")).map(_.toInt).getOrElse(19001)

  /**
   * Constructs a in-memory (h2) database configuration to add to an Application.
   */
  def inMemoryDatabase(name: String = "default", options: Map[String, String] = Map.empty[String, String]): Map[String, String] = {
    val optionsForDbUrl = options.map { case (k, v) => k + "=" + v }.mkString(";", ";", "")

    Map(
      ("db." + name + ".driver") -> "org.h2.Driver",
      ("db." + name + ".url") -> ("jdbc:h2:mem:play-test-" + scala.util.Random.nextInt + optionsForDbUrl)
    )
  }

}

object PlayRunners {
  /**
   * This mutex is used to ensure that no two tests that set the global application can run at the same time.
   */
  private[play] val mutex: AnyRef = new AnyRef()
}

trait Writeables {
  def writeableOf_AnyContentAsJson(codec: Codec, contentType: Option[String] = None): Writeable[AnyContentAsJson] =
    Writeable.writeableOf_JsValue(codec, contentType).map(_.json)

  implicit def writeableOf_AnyContentAsJson: Writeable[AnyContentAsJson] =
    Writeable.writeableOf_JsValue.map(_.json)

  implicit def writeableOf_AnyContentAsXml(implicit codec: Codec): Writeable[AnyContentAsXml] =
    Writeable.writeableOf_NodeSeq.map(c => c.xml)

  implicit def writeableOf_AnyContentAsFormUrlEncoded(implicit code: Codec): Writeable[AnyContentAsFormUrlEncoded] =
    Writeable.writeableOf_urlEncodedForm.map(c => c.data)

  implicit def writeableOf_AnyContentAsRaw: Writeable[AnyContentAsRaw] =
    Writeable.wBytes.map(c => c.raw.initialData)

  implicit def writeableOf_AnyContentAsText(implicit code: Codec): Writeable[AnyContentAsText] =
    Writeable.wString.map(c => c.txt)

  implicit def writeableOf_AnyContentAsEmpty(implicit code: Codec): Writeable[AnyContentAsEmpty.type] =
    Writeable(_ => ByteString.empty, None)

  implicit def writeableOf_AnyContentAsMultipartForm(implicit codec: Codec): Writeable[AnyContentAsMultipartFormData] =
    Writeable.writeableOf_MultipartFormData(codec, None).map(_.mfd)

  implicit def writeableOf_AnyContentAsMultipartForm(contentType: Option[String])(implicit codec: Codec): Writeable[AnyContentAsMultipartFormData] =
    Writeable.writeableOf_MultipartFormData(codec, contentType).map(_.mfd)
}

trait DefaultAwaitTimeout {

  /**
   * The default await timeout.  Override this to change it.
   */
  implicit def defaultAwaitTimeout: Timeout = 20.seconds

  /**
   * How long we should wait for something that we expect *not* to happen, e.g.
   * waiting to make sure that a channel is *not* closed by some concurrent process.
   *
   * NegativeTimeout is a separate type to a normal Timeout because we'll want to
   * set it to a lower value. This is because in normal usage we'll need to wait
   * for the full length of time to show that nothing has happened in that time.
   * If the value is too high then we'll spend a lot of time waiting during normal
   * usage. If it is too low, however, we may miss events that occur after the
   * timeout has finished. This is a necessary tradeoff.
   *
   * Where possible, tests should avoid using a NegativeTimeout. Tests will often
   * know exactly when an event should occur. In this case they can perform a
   * check for the event immediately rather than using using NegativeTimeout.
   */
  case class NegativeTimeout(t: Timeout)
  implicit val defaultNegativeTimeout = NegativeTimeout(200.millis)

}

trait FutureAwaits {
  self: DefaultAwaitTimeout =>

  import java.util.concurrent.TimeUnit

  /**
   * Block until a Promise is redeemed.
   */
  def await[T](future: Future[T])(implicit timeout: Timeout): T = Await.result(future, timeout.duration)

  /**
   * Block until a Promise is redeemed with the specified timeout.
   */
  def await[T](future: Future[T], timeout: Long, unit: TimeUnit): T =
    Await.result(future, Duration(timeout, unit))

}

trait EssentialActionCaller {
  self: Writeables =>

  /**
   * Execute an [[play.api.mvc.EssentialAction]].
   *
   * The body is serialised using the implicit writable, so that the action body parser can deserialize it.
   */
  def call[T](action: EssentialAction, req: Request[T])(implicit w: Writeable[T], mat: Materializer): Future[Result] =
    call(action, req, req.body)

  /**
   * Execute an [[play.api.mvc.EssentialAction]].
   *
   * The body is serialised using the implicit writable, so that the action body parser can deserialize it.
   */
  def call[T](action: EssentialAction, rh: RequestHeader, body: T)(implicit w: Writeable[T], mat: Materializer): Future[Result] = {
    import play.api.http.HeaderNames._
    val bytes = w.transform(body)

    val contentType = rh.headers.get(CONTENT_TYPE).orElse(w.contentType).map(CONTENT_TYPE -> _)
    val contentLength = rh.headers.get(CONTENT_LENGTH).orElse(Some(bytes.length.toString)).map(CONTENT_LENGTH -> _)
    val newHeaders = rh.headers.replace(contentLength.toSeq ++ contentType.toSeq: _*)

    action(rh.withHeaders(newHeaders)).run(Source.single(bytes))
  }
}

trait RouteInvokers extends EssentialActionCaller {
  self: Writeables =>

  // Java compatibility
  def jRoute[T](app: Application, r: RequestHeader, body: RequestBody): Option[Future[Result]] = {
    route(app, r, body.asBytes())
  }

  /**
   * Use the HttpRequestHandler to determine the Action to call for this request and execute it.
   *
   * The body is serialised using the implicit writable, so that the action body parser can deserialize it.
   */
  def route[T](app: Application, rh: RequestHeader, body: T)(implicit w: Writeable[T]): Option[Future[Result]] = {
    val (taggedRh, handler) = app.requestHandler.handlerForRequest(rh)
    import app.materializer
    handler match {
      case a: EssentialAction =>
        Some(call(a, taggedRh, body))
      case _ => None
    }
  }

  /**
   * Use the HttpRequestHandler to determine the Action to call for this request and execute it.
   *
   * The body is serialised using the implicit writable, so that the action body parser can deserialize it.
   */
  def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = route(app, req, req.body)

}

trait ResultExtractors {
  self: HeaderNames with Status =>

  /**
   * Extracts the Content-Type of this Content value.
   */
  def contentType(of: Content)(implicit timeout: Timeout): String = of.contentType

  /**
   * Extracts the content as String.
   */
  def contentAsString(of: Content)(implicit timeout: Timeout): String = of.body

  /**
   * Extracts the content as bytes.
   */
  def contentAsBytes(of: Content)(implicit timeout: Timeout): Array[Byte] = of.body.getBytes

  /**
   * Extracts the content as Json.
   */
  def contentAsJson(of: Content)(implicit timeout: Timeout): JsValue = Json.parse(of.body)

  /**
   * Extracts the Content-Type of this Result value.
   */
  def contentType(of: Future[Result])(implicit timeout: Timeout): Option[String] = {
    Await.result(of, timeout.duration).body.contentType.map(_.split(";").take(1).mkString.trim)
  }

  /**
   * Extracts the Content-Type of this Result value.
   */
  def contentType(of: Accumulator[ByteString, Result])(implicit timeout: Timeout, mat: Materializer): Option[String] = {
    contentType(of.run())
  }

  /**
   * Extracts the Charset of this Result value.
   */
  def charset(of: Future[Result])(implicit timeout: Timeout): Option[String] = {
    Await.result(of, timeout.duration).body.contentType match {
      case Some(s) if s.contains("charset=") => Some(s.split("; *charset=").drop(1).mkString.trim)
      case _ => None
    }
  }

  /**
   * Extracts the Charset of this Result value.
   */
  def charset(of: Accumulator[ByteString, Result])(implicit timeout: Timeout, mat: Materializer): Option[String] = {
    charset(of.run())
  }

  /**
   * Extracts the content as String.
   */
  def contentAsString(of: Future[Result])(implicit timeout: Timeout, mat: Materializer = NoMaterializer): String =
    contentAsBytes(of).decodeString(charset(of).getOrElse("utf-8"))

  /**
   * Extracts the content as String.
   */
  def contentAsString(of: Accumulator[ByteString, Result])(implicit timeout: Timeout, mat: Materializer): String = contentAsString(of.run())

  /**
   * Extracts the content as bytes.
   */
  def contentAsBytes(of: Future[Result])(implicit timeout: Timeout, mat: Materializer = NoMaterializer): ByteString = {
    val result = Await.result(of, timeout.duration)
    Await.result(result.body.consumeData, timeout.duration)
  }

  /**
   * Extracts the content as bytes.
   */
  def contentAsBytes(of: Accumulator[ByteString, Result])(implicit timeout: Timeout, mat: Materializer): ByteString = contentAsBytes(of.run())

  /**
   * Extracts the content as Json.
   */
  def contentAsJson(of: Future[Result])(implicit timeout: Timeout, mat: Materializer = NoMaterializer): JsValue = Json.parse(contentAsString(of))

  /**
   * Extracts the content as Json.
   */
  def contentAsJson(of: Accumulator[ByteString, Result])(implicit timeout: Timeout, mat: Materializer): JsValue = contentAsJson(of.run())

  /**
   * Extracts the Status code of this Result value.
   */
  def status(of: Future[Result])(implicit timeout: Timeout): Int = Await.result(of, timeout.duration).header.status

  /**
   * Extracts the Status code of this Result value.
   */
  def status(of: Accumulator[ByteString, Result])(implicit timeout: Timeout, mat: Materializer): Int = status(of.run())

  /**
   * Gets the Cookies associated with this Result value. Note that this only extracts the "new" cookies added to
   * this result (e.g. through withCookies), not including the Session or Flash. The final set of cookies may be
   * different because the Play server automatically adds those cookies and merges the headers.
   */
  def cookies(of: Future[Result])(implicit timeout: Timeout): Cookies = {
    Await.result(of.map { result =>
      val cookies = result.newCookies
      new Cookies {
        lazy val cookiesByName: Map[String, Cookie] = cookies.groupBy(_.name).mapValues(_.head)
        override def get(name: String): Option[Cookie] = cookiesByName.get(name)
        override def foreach[U](f: Cookie => U): Unit = cookies.foreach(f)
      }
    }(play.core.Execution.trampoline), timeout.duration)
  }

  /**
   * Extracts the Cookies set by this Result value.
   */
  def cookies(of: Accumulator[ByteString, Result])(implicit timeout: Timeout, mat: Materializer): Cookies = cookies(of.run())

  /**
   * Extracts the Flash values set by this Result value.
   */
  def flash(of: Future[Result])(implicit timeout: Timeout): Flash = {
    Await.result(of.map(_.newFlash.getOrElse(new Flash()))(play.core.Execution.trampoline), timeout.duration)
  }

  /**
   * Extracts the Flash values set by this Result value.
   */
  def flash(of: Accumulator[ByteString, Result])(implicit timeout: Timeout, mat: Materializer): Flash = flash(of.run())

  /**
   * Extracts the Session values set by this Result value.
   */
  def session(of: Future[Result])(implicit timeout: Timeout): Session = {
    Await.result(of.map(_.newSession.getOrElse(new Session()))(play.core.Execution.trampoline), timeout.duration)
  }

  /**
   * Extracts the Session set by this Result value.
   */
  def session(of: Accumulator[ByteString, Result])(implicit timeout: Timeout, mat: Materializer): Session = session(of.run())

  /**
   * Extracts the Location header of this Result value if this Result is a Redirect.
   */
  def redirectLocation(of: Future[Result])(implicit timeout: Timeout): Option[String] = Await.result(of, timeout.duration).header match {
    case ResponseHeader(FOUND, headers, _) => headers.get(LOCATION)
    case ResponseHeader(SEE_OTHER, headers, _) => headers.get(LOCATION)
    case ResponseHeader(TEMPORARY_REDIRECT, headers, _) => headers.get(LOCATION)
    case ResponseHeader(MOVED_PERMANENTLY, headers, _) => headers.get(LOCATION)
    case ResponseHeader(_, _, _) => None
  }

  /**
   * Extracts the Location header of this Result value if this Result is a Redirect.
   */
  def redirectLocation(of: Accumulator[ByteString, Result])(implicit timeout: Timeout, mat: Materializer): Option[String] =
    redirectLocation(of.run())

  /**
   * Extracts an Header value of this Result value.
   */
  def header(header: String, of: Future[Result])(implicit timeout: Timeout): Option[String] = headers(of).get(header)

  /**
   * Extracts an Header value of this Result value.
   */
  def header(header: String, of: Accumulator[ByteString, Result])(implicit timeout: Timeout, mat: Materializer): Option[String] =
    this.header(header, of.run())

  /**
   * Extracts all Headers of this Result value.
   */
  def headers(of: Future[Result])(implicit timeout: Timeout): Map[String, String] = Await.result(of, timeout.duration).header.headers

  /**
   * Extracts all Headers of this Result value.
   */
  def headers(of: Accumulator[ByteString, Result])(implicit timeout: Timeout, mat: Materializer): Map[String, String] =
    headers(of.run())

}

trait StubPlayBodyParsersFactory {

  /**
   * Stub method for unit testing, using NoTemporaryFileCreator.
   *
   * @param mat the input materializer.
   * @return a minimal PlayBodyParsers for unit testing.
   */
  def stubPlayBodyParsers(implicit mat: Materializer): PlayBodyParsers = {
    val errorHandler = new DefaultHttpErrorHandler(HttpErrorConfig(showDevErrors = false, None), None, None)
    PlayBodyParsers(NoTemporaryFileCreator, errorHandler)
  }

}

trait StubMessagesFactory {

  /**
   * @return a stub Langs
   * @param availables default as Seq(Lang.defaultLang).
   */
  def stubLangs(availables: Seq[Lang] = Seq(Lang.defaultLang)): Langs = {
    new DefaultLangs(availables)
  }

  /**
   * Returns a stub DefaultMessagesApi with default values and an empty map.
   *
   * @param messages map of languages to map of messages, empty by default.
   * @param langs stubLangs() by default
   * @param langCookieName "PLAY_LANG" by default
   * @param langCookieSecure false by default
   * @param langCookieHttpOnly false by default
   * @param httpConfiguration configuration, HttpConfiguration() by default.
   * @return the messagesApi with minimal configuration.
   */
  def stubMessagesApi(
    messages: Map[String, Map[String, String]] = Map.empty,
    langs: Langs = stubLangs(),
    langCookieName: String = "PLAY_LANG",
    langCookieSecure: Boolean = false,
    langCookieHttpOnly: Boolean = false,
    httpConfiguration: HttpConfiguration = HttpConfiguration()): MessagesApi = {
    new DefaultMessagesApi(messages, langs, langCookieName, langCookieSecure, langCookieHttpOnly, httpConfiguration)
  }

  /**
   * Stub method that returns a [[play.api.i18n.Messages]] instance.
   *
   * @param messagesApi the messagesApi to use, uses stubMessagesApi by default.
   * @param requestHeader the request to use, FakeRequest by default.
   * @return the Messages instance
   */
  def stubMessages(
    messagesApi: MessagesApi = stubMessagesApi(),
    requestHeader: RequestHeader = FakeRequest()): Messages = {
    messagesApi.preferred(requestHeader)
  }

  /**
   * Stub method that returns a [[play.api.mvc.MessagesRequest]] instance.
   *
   * @param messagesApi the messagesApi to use, uses stubMessagesApi by default.
   * @param request the request to use, FakeRequest by default.
   * @return the Messages instance
   */
  def stubMessagesRequest(
    messagesApi: MessagesApi = stubMessagesApi(),
    request: Request[AnyContentAsEmpty.type] = FakeRequest()): MessagesRequest[AnyContentAsEmpty.type] = {
    new MessagesRequest[AnyContentAsEmpty.type](request, messagesApi)
  }

}

trait StubBodyParserFactory {
  /**
   * Stub method that returns the content immediately.  Useful for unit testing.
   *
   * {{{
   * val stubParser = bodyParser(AnyContent("hello"))
   * }}}
   *
   * @param content the content to return, AnyContentAsEmpty by default
   * @return a BodyParser for type T that returns Accumulator.done(Right(content))
   */
  def stubBodyParser[T](content: T = AnyContentAsEmpty): BodyParser[T] = {
    BodyParser(_ => Accumulator.done(Right(content)))
  }
}

trait StubControllerComponentsFactory extends StubPlayBodyParsersFactory with StubBodyParserFactory with StubMessagesFactory {

  /**
   * Create a minimal controller components, useful for unit testing.
   *
   * In most cases, you'll want the standard defaults:
   *
   * {{{
   *   val controller = new MyController(stubControllerComponents())
   * }}}
   *
   * A custom body parser can be used with bodyParser() to provide a request body to the controller:
   *
   * {{{
   * val cc = stubControllerComponents(bodyParser(AnyContent("request body text")))
   * }}}
   *
   * @param bodyParser the body parser used to parse any content, stubBodyParser(AnyContentAsEmpty) by default.
   * @param playBodyParsers the playbodyparsers, defaults to stubPlayBodyParsers(NoMaterializer)
   * @param messagesApi: the messages api, new DefaultMessagesApi() by default.
   * @param langs the langs instance for messaging, new DefaultLangs() by default.
   * @param fileMimeTypes the mime type associated with file extensions, new DefaultFileMimeTypes(FileMimeTypesConfiguration() by default.
   * @param executionContext an execution context, defaults to ExecutionContext.global
   * @return a fully configured ControllerComponents instance.
   */
  def stubControllerComponents(
    bodyParser: BodyParser[AnyContent] = stubBodyParser(AnyContentAsEmpty),
    playBodyParsers: PlayBodyParsers = stubPlayBodyParsers(NoMaterializer),
    messagesApi: MessagesApi = stubMessagesApi(),
    langs: Langs = stubLangs(),
    fileMimeTypes: FileMimeTypes = new DefaultFileMimeTypes(FileMimeTypesConfiguration()),
    executionContext: ExecutionContext = ExecutionContext.global): ControllerComponents = {
    DefaultControllerComponents(
      DefaultActionBuilder(bodyParser)(executionContext),
      playBodyParsers,
      messagesApi,
      langs,
      fileMimeTypes,
      executionContext)
  }
}

object Helpers extends PlayRunners
  with HeaderNames
  with Status
  with MimeTypes
  with HttpProtocol
  with DefaultAwaitTimeout
  with ResultExtractors
  with Writeables
  with EssentialActionCaller
  with RouteInvokers
  with FutureAwaits
  with StubControllerComponentsFactory

/**
 * A trait declared on a class that contains an `def app: Application`, and can provide
 * instances of a class.  Useful in integration tests.
 */
trait Injecting {
  self: HasApp =>

  /**
   * Given an application, provides an instance from the application.
   *
   * @tparam T the type to return, using `app.injector.instanceOf`
   * @return an instance of type T.
   */
  def inject[T: ClassTag]: T = {
    self.app.injector.instanceOf
  }
}

/**
 * A temporary file creator with no implementation.
 */
object NoTemporaryFileCreator extends Files.TemporaryFileCreator {
  override def create(prefix: String, suffix: String): Files.TemporaryFile = {
    throw new UnsupportedOperationException("Cannot create temporary file")
  }
  override def create(path: Path): Files.TemporaryFile = {
    throw new UnsupportedOperationException(s"Cannot create temporary file at $path")
  }
  override def delete(file: Files.TemporaryFile): Try[Boolean] = {
    throw new UnsupportedOperationException(s"Cannot delete temporary file at $file")
  }
}

/**
 * In 99% of cases, when running tests against the result body, you don't actually need a materializer since it's a
 * strict body. So, rather than always requiring an implicit materializer, we use one if provided, otherwise we have
 * a default one that simply throws an exception if used.
 */
object NoMaterializer extends Materializer {
  override def withNamePrefix(name: String): Materializer =
    throw new UnsupportedOperationException("NoMaterializer cannot be named")
  override def materialize[Mat](runnable: Graph[ClosedShape, Mat]): Mat =
    throw new UnsupportedOperationException("NoMaterializer cannot materialize")
  override def materialize[Mat](runnable: Graph[ClosedShape, Mat], initialAttributes: Attributes): Mat =
    throw new UnsupportedOperationException("NoMaterializer cannot materialize")

  override def executionContext: ExecutionContextExecutor =
    throw new UnsupportedOperationException("NoMaterializer does not provide an ExecutionContext")

  def scheduleOnce(delay: FiniteDuration, task: Runnable): Cancellable =
    throw new UnsupportedOperationException("NoMaterializer cannot schedule a single event")

  def schedulePeriodically(initialDelay: FiniteDuration, interval: FiniteDuration, task: Runnable): Cancellable =
    throw new UnsupportedOperationException("NoMaterializer cannot schedule a repeated event")
}
