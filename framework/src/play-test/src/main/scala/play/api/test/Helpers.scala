/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.test

import scala.language.reflectiveCalls

import play.api._
import play.api.mvc._
import play.api.http._

import play.api.libs.iteratee._
import play.api.libs.json.{ Json, JsValue }

import play.twirl.api.Content

import org.openqa.selenium._
import org.openqa.selenium.firefox._
import org.openqa.selenium.htmlunit._

import scala.concurrent.Await
import scala.concurrent.duration._

import scala.concurrent.Future
import akka.util.Timeout

/**
 * Helper functions to run tests.
 */
trait PlayRunners extends HttpVerbs {

  val HTMLUNIT = classOf[HtmlUnitDriver]
  val FIREFOX = classOf[FirefoxDriver]

  /**
   * Executes a block of code in a running application.
   */
  def running[T](app: Application)(block: => T): T = {
    PlayRunners.mutex.synchronized {
      try {
        Play.start(app)
        block
      } finally {
        Play.stop(app)
      }
    }
  }

  /**
   * Executes a block of code in a running server.
   */
  def running[T](testServer: TestServer)(block: => T): T = {
    PlayRunners.mutex.synchronized {
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
    PlayRunners.mutex.synchronized {
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
  lazy val testServerPort = Option(System.getProperty("testserver.port")).map(_.toInt).getOrElse(19001)

  /**
   * Constructs a in-memory (h2) database configuration to add to a FakeApplication.
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
  implicit def writeableOf_AnyContentAsJson(implicit codec: Codec): Writeable[AnyContentAsJson] =
    Writeable.writeableOf_JsValue.map(c => c.json)

  implicit def writeableOf_AnyContentAsXml(implicit codec: Codec): Writeable[AnyContentAsXml] =
    Writeable.writeableOf_NodeSeq.map(c => c.xml)

  implicit def writeableOf_AnyContentAsFormUrlEncoded(implicit code: Codec): Writeable[AnyContentAsFormUrlEncoded] =
    Writeable.writeableOf_urlEncodedForm.map(c => c.data)

  implicit def writeableOf_AnyContentAsRaw: Writeable[AnyContentAsRaw] =
    Writeable.wBytes.map(c => c.raw.initialData)

  implicit def writeableOf_AnyContentAsText(implicit code: Codec): Writeable[AnyContentAsText] =
    Writeable.wString.map(c => c.txt)

  implicit def writeableOf_AnyContentAsEmpty(implicit code: Codec): Writeable[AnyContentAsEmpty.type] =
    Writeable(_ => Array.empty[Byte], None)
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
   * The body is serialised using the implicit writable, so that the action body parser can deserialise it.
   */
  def call[T](action: EssentialAction, req: Request[T])(implicit w: Writeable[T]): Future[Result] =
    call(action, req, req.body)

  /**
   * Execute an [[play.api.mvc.EssentialAction]].
   *
   * The body is serialised using the implicit writable, so that the action body parser can deserialise it.
   */
  def call[T](action: EssentialAction, rh: RequestHeader, body: T)(implicit w: Writeable[T]): Future[Result] = {
    import play.api.http.HeaderNames._
    val newContentType = rh.headers.get(CONTENT_TYPE).fold(w.contentType)(_ => None)
    val rhWithCt = newContentType.map { ct =>
      rh.copy(headers = rh.headers.replace(CONTENT_TYPE -> ct))
    }.getOrElse(rh)

    val requestBody = Enumerator(body) &> w.toEnumeratee
    requestBody |>>> action(rhWithCt)
  }
}

trait RouteInvokers extends EssentialActionCaller {
  self: Writeables =>

  // Java compatibility
  def jRoute[T](app: Application, r: RequestHeader, body: T): Option[Future[Result]] = {
    (body: @unchecked) match {
      case body: AnyContentAsFormUrlEncoded => route(app, r, body)
      case body: AnyContentAsJson => route(app, r, body)
      case body: AnyContentAsXml => route(app, r, body)
      case body: AnyContentAsText => route(app, r, body)
      case body: AnyContentAsRaw => route(app, r, body)
      case body: AnyContentAsEmpty.type => route(app, r, body)
      //case _ => MatchError is thrown
    }
  }

  /**
   * Use the HttpRequestHandler to determine the Action to call for this request and execute it.
   *
   * The body is serialised using the implicit writable, so that the action body parser can deserialise it.
   */
  def route[T](app: Application, rh: RequestHeader, body: T)(implicit w: Writeable[T]): Option[Future[Result]] = {
    val (taggedRh, handler) = app.requestHandler.handlerForRequest(rh)
    handler match {
      case a: EssentialAction =>
        Some(call(a, taggedRh, body))
      case _ => None
    }
  }

  /**
   * Use the HttpRequestHandler to determine the Action to call for this request and execute it.
   *
   * The body is serialised using the implicit writable, so that the action body parser can deserialise it.
   */
  def route[T](rh: RequestHeader, body: T)(implicit w: Writeable[T]): Option[Future[Result]] = route(Play.current, rh, body)

  /**
   * Use the HttpRequestHandler to determine the Action to call for this request and execute it.
   *
   * The body is serialised using the implicit writable, so that the action body parser can deserialise it.
   */
  def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = route(app, req, req.body)

  /**
   * Use the HttpRequestHandler to determine the Action to call for this request and execute it.
   *
   * The body is serialised using the implicit writable, so that the action body parser can deserialise it.
   */
  def route[T](req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = route(Play.current, req)
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
  def contentType(of: Future[Result])(implicit timeout: Timeout): Option[String] = header(CONTENT_TYPE, of).map(_.split(";").take(1).mkString.trim)

  /**
   * Extracts the Charset of this Result value.
   */
  def charset(of: Future[Result])(implicit timeout: Timeout): Option[String] = header(CONTENT_TYPE, of) match {
    case Some(s) if s.contains("charset=") => Some(s.split("; charset=").drop(1).mkString.trim)
    case _ => None
  }

  /**
   * Extracts the content as String.
   */
  def contentAsString(of: Future[Result])(implicit timeout: Timeout): String = new String(contentAsBytes(of), charset(of).getOrElse("utf-8"))

  /**
   * Extracts the content as bytes.
   */
  def contentAsBytes(of: Future[Result])(implicit timeout: Timeout): Array[Byte] = {
    val result = Await.result(of, timeout.duration)
    val eBytes = result.header.headers.get(TRANSFER_ENCODING) match {
      case Some("chunked") => result.body &> Results.dechunk
      case _ => result.body
    }
    Await.result(eBytes |>>> Iteratee.consume[Array[Byte]](), timeout.duration)
  }

  /**
   * Extracts the content as Json.
   */
  def contentAsJson(of: Future[Result])(implicit timeout: Timeout): JsValue = Json.parse(contentAsString(of))

  /**
   * Extracts the Status code of this Result value.
   */
  def status(of: Future[Result])(implicit timeout: Timeout): Int = Await.result(of, timeout.duration).header.status

  /**
   * Extracts the Cookies of this Result value.
   */
  def cookies(of: Future[Result])(implicit timeout: Timeout): Cookies = Cookies(header(SET_COOKIE, of))

  /**
   * Extracts the Flash values of this Result value.
   */
  def flash(of: Future[Result])(implicit timeout: Timeout): Flash = Flash.decodeFromCookie(cookies(of).get(Flash.COOKIE_NAME))

  /**
   * Extracts the Session of this Result value.
   * Extracts the Session from this Result value.
   */
  def session(of: Future[Result])(implicit timeout: Timeout): Session = Session.decodeFromCookie(cookies(of).get(Session.COOKIE_NAME))

  /**
   * Extracts the Location header of this Result value if this Result is a Redirect.
   */
  def redirectLocation(of: Future[Result])(implicit timeout: Timeout): Option[String] = Await.result(of, timeout.duration).header match {
    case ResponseHeader(FOUND, headers) => headers.get(LOCATION)
    case ResponseHeader(SEE_OTHER, headers) => headers.get(LOCATION)
    case ResponseHeader(TEMPORARY_REDIRECT, headers) => headers.get(LOCATION)
    case ResponseHeader(MOVED_PERMANENTLY, headers) => headers.get(LOCATION)
    case ResponseHeader(_, _) => None
  }

  /**
   * Extracts an Header value of this Result value.
   */
  def header(header: String, of: Future[Result])(implicit timeout: Timeout): Option[String] = headers(of).get(header)

  /**
   * Extracts all Headers of this Result value.
   */
  def headers(of: Future[Result])(implicit timeout: Timeout): Map[String, String] = Await.result(of, timeout.duration).header.headers
}

object Helpers extends PlayRunners
  with HeaderNames
  with Status
  with HttpProtocol
  with DefaultAwaitTimeout
  with ResultExtractors
  with Writeables
  with EssentialActionCaller
  with RouteInvokers
  with FutureAwaits
