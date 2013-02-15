package play.api.test

import scala.language.reflectiveCalls
import scala.xml.NodeSeq

import play.api._
import libs.ws.WS
import play.api.mvc._
import play.api.http._

import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.libs.json.JsValue

import org.openqa.selenium._
import org.openqa.selenium.firefox._
import org.openqa.selenium.htmlunit._

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Helper functions to run tests.
 */
object Helpers extends Status with HeaderNames {

  val GET = "GET"
  val POST = "POST"
  val PUT = "PUT"
  val DELETE = "DELETE"
  val HEAD = "HEAD"

  val HTMLUNIT = classOf[HtmlUnitDriver]
  val FIREFOX = classOf[FirefoxDriver]

  /**
   * Executes a block of code in a running application.
   */
  def running[T](fakeApp: FakeApplication)(block: => T): T = {
    synchronized {
      try {
        Play.start(fakeApp)
        block
      } finally {
        Play.stop()
        play.api.libs.ws.WS.resetClient()
      }
    }
  }

  /**
   * Executes a block of code in a running server.
   */
  def running[T](testServer: TestServer)(block: => T): T = {
    synchronized {
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
    var browser: TestBrowser = null
    synchronized {
      try {
        testServer.start()
        browser = TestBrowser.of(webDriver)
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
   * Extracts the Content-Type of this Content value.
   */
  def contentType(of: Content): String = of.contentType

  /**
   * Extracts the content as String.
   */
  def contentAsString(of: Content): String = of.body

  /**
   * Extracts the content as bytes.
   */
  def contentAsBytes(of: Content): Array[Byte] = of.body.getBytes

  /**
   * Extracts the Content-Type of this Result value.
   */
  def contentType(of: Result): Option[String] = header(CONTENT_TYPE, of).map(_.split(";").take(1).mkString.trim)

  /**
   * Extracts the Charset of this Result value.
   */
  def charset(of: Result): Option[String] = header(CONTENT_TYPE, of) match {
    case Some(s) if s.contains("charset=") => Some(s.split("; charset=").drop(1).mkString.trim)
    case _ => None
  }

  /**
   * Extracts the content as String.
   */
  def contentAsString(of: Result): String = new String(contentAsBytes(of), charset(of).getOrElse("utf-8"))

  /**
   * Extracts the content as bytes.
   */
  def contentAsBytes(of: Result): Array[Byte] = of match {
    case r @ SimpleResult(_, bodyEnumerator) => {
      var readAsBytes = Enumeratee.map[r.BODY_CONTENT](r.writeable.transform(_)).transform(Iteratee.consume[Array[Byte]]())
      bodyEnumerator(readAsBytes).flatMap(_.run).value1.get
    }
    case AsyncResult(p) => contentAsBytes(p.await.get)
  }

  /**
   * Extracts the Status code of this Result value.
   */
  def status(of: Result): Int = of match {
    case PlainResult(status, _) => status
    case AsyncResult(p) => status(p.await.get)
  }

  /**
   * Extracts the Cookies of this Result value.
   */
  def cookies(of: Result): Cookies = Cookies(header(SET_COOKIE, of))

  /**
   * Extracts the Flash values of this Result value.
   */
  def flash(of: Result): Flash = Flash.decodeFromCookie(cookies(of).get(Flash.COOKIE_NAME))

  /**
   * Extracts the Session of this Result value.
   * Extracts the Session from this Result value.
   */
  def session(of: Result): Session = Session.decodeFromCookie(cookies(of).get(Session.COOKIE_NAME))

  /**
   * Extracts the Location header of this Result value if this Result is a Redirect.
   */
  def redirectLocation(of: Result): Option[String] = of match {
    case PlainResult(FOUND, headers) => headers.get(LOCATION)
    case PlainResult(SEE_OTHER, headers) => headers.get(LOCATION)
    case PlainResult(TEMPORARY_REDIRECT, headers) => headers.get(LOCATION)
    case PlainResult(MOVED_PERMANENTLY, headers) => headers.get(LOCATION)
    case PlainResult(_, _) => None
    case AsyncResult(p) => redirectLocation(p.await.get)
    case r => sys.error("Cannot extract the headers from a result of type " + r.getClass.getName)
  }

  /**
   * Extracts an Header value of this Result value.
   */
  def header(header: String, of: Result): Option[String] = headers(of).get(header)

  /**
   * Extracts all Headers of this Result value.
   */
  def headers(of: Result): Map[String, String] = of match {
    case PlainResult(_, headers) => headers
    case AsyncResult(p) => headers(p.await.get)
  }

  /**
   * Use the Router to determine the Action to call for this request and executes it.
   */
  @deprecated("Use `route` instead.", "2.1.0")
  def routeAndCall[T](request: FakeRequest[T]): Option[Result] = {
    routeAndCall(this.getClass.getClassLoader.loadClass("Routes").asInstanceOf[Class[play.core.Router.Routes]], request)
  }

  /**
   * Use the Router to determine the Action to call for this request and executes it.
   */
  @deprecated("Use `route` instead.", "2.1.0")
  def routeAndCall[T, ROUTER <: play.core.Router.Routes](router: Class[ROUTER], request: FakeRequest[T]): Option[Result] = {
    val routes = router.getClassLoader.loadClass(router.getName + "$").getDeclaredField("MODULE$").get(null).asInstanceOf[play.core.Router.Routes]
    routes.routes.lift(request).map {
      case a: Action[_] =>
        val action = a.asInstanceOf[Action[T]]
        val parsedBody: Option[Either[play.api.mvc.Result, T]] = action.parser(request).fold1(
          (a, in) => Promise.pure(Some(a)),
          k => Promise.pure(None),
          (msg, in) => Promise.pure(None)).await.get
        parsedBody.map { resultOrT =>
          resultOrT.right.toOption.map { innerBody =>
            action(FakeRequest(request.method, request.uri, request.headers, innerBody))
          }.getOrElse(resultOrT.left.get)
        }.getOrElse(action(request))

    }
  }

  // Java compatibility
  def jRoute(app: Application, rh: RequestHeader): Option[Result] = route(app, rh, AnyContentAsEmpty)
  def jRoute(app: Application, rh: RequestHeader, body: Array[Byte]): Option[Result] = route(app, rh, body)(Writeable.wBytes)
  def jRoute(rh: RequestHeader, body: Array[Byte]): Option[Result] = jRoute(Play.current, rh, body)
  def jRoute[T](app: Application, r: FakeRequest[T]): Option[Result] = {
    (r.body: @unchecked) match {
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
   * Use the Router to determine the Action to call for this request and execute it.
   *
   * The body is serialised using the implicit writable, so that the action body parser can deserialise it.
   */
  def route[T](app: Application, rh: RequestHeader, body: T)(implicit w: Writeable[T]): Option[Result] = {
    val rhWithCt = w.contentType.map(ct => rh.copy(
      headers = FakeHeaders((rh.headers.toMap + ("Content-Type" -> Seq(ct))).toSeq)
    )).getOrElse(rh)
    val handler = app.global.onRouteRequest(rhWithCt)
    val taggedRh = handler.map({
      case h: RequestTaggingHandler => h.tagRequest(rhWithCt)
      case _ => rh
    }).getOrElse(rhWithCt)
    handler.flatMap {
      case a: EssentialAction => {
        Some(AsyncResult(app.global.doFilter(a)(taggedRh).feed(Input.El(w.transform(body))).flatMap(_.run)))
      }
      case _ => None
    }
  }

  /**
   * Use the Router to determine the Action to call for this request and execute it.
   *
   * The body is serialised using the implicit writable, so that the action body parser can deserialise it.
   */
  def route[T](rh: RequestHeader, body: T)(implicit w: Writeable[T]): Option[Result] = route(Play.current, rh, body)

  /**
   * Use the Router to determine the Action to call for this request and execute it.
   *
   * The body is serialised using the implicit writable, so that the action body parser can deserialise it.
   */
  def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Result] = route(app, req, req.body)

  /**
   * Use the Router to determine the Action to call for this request and execute it.
   *
   * The body is serialised using the implicit writable, so that the action body parser can deserialise it.
   */
  def route[T](req: Request[T])(implicit w: Writeable[T]): Option[Result] = route(Play.current, req)

  /**
   * Block until a Promise is redeemed.
   */
  def await[T](p: scala.concurrent.Future[T]): T = await(p, 5000)

  /**
   * Block until a Promise is redeemed with the specified timeout.
   */
  def await[T](p: scala.concurrent.Future[T], timeout: Long, unit: java.util.concurrent.TimeUnit = java.util.concurrent.TimeUnit.MILLISECONDS): T = p.await(timeout, unit).get

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

  /**
   * Construct a WS request for the given reverse route.
   *
   * For example:
   * {{{
   *   wsCall(controllers.routes.Application.index()).get()
   * }}}
   */
  def wsCall(call: Call)(implicit port: Port): WS.WSRequestHolder = wsUrl(call.url)

  /**
   * Construct a WS request for the given relative URL.
   */
  def wsUrl(url: String)(implicit port: Port): WS.WSRequestHolder = WS.url("http://localhost:" + port + url)

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
