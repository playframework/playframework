package play.api.test

import play.api._
import play.api.mvc._
import play.api.http._

import play.api.libs.iteratee._
import play.api.libs.concurrent._

import org.openqa.selenium._
import org.openqa.selenium.firefox._
import org.openqa.selenium.htmlunit._

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
    try {
      Play.start(fakeApp)
      block
    } finally {
      Play.stop()
    }
  }

  /**
   * Executes a block of code in a running server.
   */
  def running[T](testServer: TestServer)(block: => T): T = {
    try {
      testServer.start()
      block
    } finally {
      testServer.stop()
    }
  }

  /**
   * Executes a block of code in a running server, with a test browser.
   */
  def running[T, WEBDRIVER <: WebDriver](testServer: TestServer, webDriver: Class[WEBDRIVER])(block: TestBrowser => T): T = {
    var browser: TestBrowser = null
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

  /**
   * Apply pending evolutions for the given DB.
   */
  def evolutionFor(dbName: String, path: java.io.File = new java.io.File(".")): Unit = play.api.db.evolutions.OfflineEvolutions.applyScript(path, this.getClass.getClassLoader, dbName)

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
    case r => sys.error("Cannot extract the body content from a result of type " + r.getClass.getName)
  }

  /**
   * Extracts the Status code of this Result value.
   */
  def status(of: Result): Int = of match {
    case Result(status, _) => status
    case AsyncResult(p) => status(p.await.get)
    case r => sys.error("Cannot extract the status from a result of type " + r.getClass.getName)
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
    case Result(FOUND, headers) => headers.get(LOCATION)
    case Result(SEE_OTHER, headers) => headers.get(LOCATION)
    case Result(TEMPORARY_REDIRECT, headers) => headers.get(LOCATION)
    case Result(MOVED_PERMANENTLY, headers) => headers.get(LOCATION)
    case Result(_, _) => None
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
    case Result(_, headers) => headers
    case AsyncResult(p) => headers(p.await.get)
    case r => sys.error("Cannot extract the headers from a result of type " + r.getClass.getName)
  }

  /**
   * Use the Router to determine the Action to call for this request and executes it.
   */
  def routeAndCall[T](request: FakeRequest[T]): Option[Result] = {
    routeAndCall(this.getClass.getClassLoader.loadClass("Routes").asInstanceOf[Class[play.core.Router.Routes]], request)
  }

  /**
   * Use the Router to determine the Action to call for this request and executes it.
   */
  def routeAndCall[T, ROUTER <: play.core.Router.Routes](router: Class[ROUTER], request: FakeRequest[T]): Option[Result] = {
    val routes = router.getClassLoader.loadClass(router.getName + "$").getDeclaredField("MODULE$").get(null).asInstanceOf[play.core.Router.Routes]
    routes.routes.lift(request).map {
      case a: Action[_] => 
        val action = a.asInstanceOf[Action[T]]
        val parsedBody: Option[Either[play.api.mvc.Result,T]] = action.parser(request).fold1(
          (a,in) => Promise.pure(Some(a)),
          k => Promise.pure(None),
          (msg,in) => Promise.pure(None)).await.get
        parsedBody.map{resultOrT =>
          resultOrT.right.toOption.map{innerBody => 
            action(FakeRequest(request.method, request.uri, request.headers, innerBody))
          }.getOrElse(resultOrT.left.get)
        }.getOrElse(action(request))
        
    }
  }

  /**
   * Block until a Promise is redeemed.
   */
  def await[T](p: play.api.libs.concurrent.Promise[T]): T = await(p, 5000)

  /**
   * Block until a Promise is redeemed with the specified timeout.
   */
  def await[T](p: play.api.libs.concurrent.Promise[T], timeout: Long, unit: java.util.concurrent.TimeUnit = java.util.concurrent.TimeUnit.MILLISECONDS): T = p.await(timeout, unit).get

  /**
   * Constructs a in-memory (h2) database configuration to add to a FakeApplication.
   */
  def inMemoryDatabase(name: String = "default"): Map[String, String] = {
    Map(
      ("db." + name + ".driver") -> "org.h2.Driver",
      ("db." + name + ".url") -> ("jdbc:h2:mem:play-test-" + scala.util.Random.nextInt)
    )
  }

}
