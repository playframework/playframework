package play.api.test

import play.api._
import play.api.mvc._
import play.api.http._

import play.api.libs.iteratee._

import org.openqa.selenium._
import org.openqa.selenium.firefox._
import org.openqa.selenium.htmlunit._

object Helpers extends Status with HeaderNames {

  val GET = "GET"
  val POST = "POST"
  val PUT = "PUT"
  val DELETE = "DELETE"
  val HEAD = "HEAD"

  val HTMLUNIT = classOf[HtmlUnitDriver]
  val FIREFOX = classOf[FirefoxDriver]

  def running[T](fakeApp: FakeApplication)(block: => T): T = {
    try {
      Play.start(fakeApp)
      block
    } finally {
      Play.stop()
    }
  }

  def running[T](testServer: TestServer)(block: => T): T = {
    try {
      testServer.start()
      block
    } finally {
      testServer.stop()
    }
  }

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

  def contentType(of: Content): String = of.contentType

  def contentAsString(of: Content): String = of.body

  def contentAsBytes(of: Content): Array[Byte] = of.body.getBytes

  def contentType(of: Result): Option[String] = header(CONTENT_TYPE, of).map(_.split(";").take(1).mkString.trim)

  def charset(of: Result): Option[String] = header(CONTENT_TYPE, of).map(_.split("; charset=").drop(1).mkString.trim)

  def contentAsString(of: Result): String = new String(contentAsBytes(of), charset(of).getOrElse("utf-8"))

  def contentAsBytes(of: Result): Array[Byte] = of match {
    case r @ SimpleResult(_, bodyEnumerator) => {
      var readAsBytes = Enumeratee.map[r.BODY_CONTENT](r.writeable.transform(_)).transform(Iteratee.consume[Array[Byte]]())
      bodyEnumerator(readAsBytes).flatMap(_.run).value.get
    }
    case r => sys.error("Cannot extract the body content from a result of type " + r.getClass.getName)
  }

  def status(of: Result): Int = of match {
    case Result(status, _) => status
    case r => sys.error("Cannot extract the status from a result of type " + r.getClass.getName)
  }

  def cookies(of: Result): Cookies = Cookies(header(SET_COOKIE, of))

  def flash(of: Result): Flash = Flash.decodeFromCookie(cookies(of).get(Flash.COOKIE_NAME))

  def redirectLocation(of: Result): Option[String] = of match {
    case Result(FOUND, headers) => headers.get(LOCATION)
    case Result(_, _) => None
    case r => sys.error("Cannot extract the headers from a result of type " + r.getClass.getName)
  }

  def header(header: String, of: Result): Option[String] = headers(of).get(header)

  def headers(of: Result): Map[String, String] = of match {
    case Result(_, headers) => headers
    case r => sys.error("Cannot extract the headers from a result of type " + r.getClass.getName)
  }

  def routeAndCall[T](request: FakeRequest[T]): Option[Result] = {
    routeAndCall(this.getClass.getClassLoader.loadClass("Routes").asInstanceOf[Class[play.core.Router.Routes]], request)
  }

  def routeAndCall[T, ROUTER <: play.core.Router.Routes](router: Class[ROUTER], request: FakeRequest[T]): Option[Result] = {
    val routes = router.getClassLoader.loadClass(router.getName + "$").getDeclaredField("MODULE$").get(null).asInstanceOf[play.core.Router.Routes]
    routes.routes.lift(request).map {
      case action: Action[_] => action.asInstanceOf[Action[T]](request)
    }
  }

  def await[T](p: play.api.libs.concurrent.Promise[T]): T = await(p, 5000)

  def await[T](p: play.api.libs.concurrent.Promise[T], timeout: Long, unit: java.util.concurrent.TimeUnit = java.util.concurrent.TimeUnit.MILLISECONDS): T = p.await(timeout, unit).get

  def inMemoryDatabase(name: String = "default") = {
    Map(
      ("db." + name + ".driver") -> "org.h2.Driver",
      ("db." + name + ".url") -> ("jdbc:h2:mem:play-test-" + scala.util.Random.nextInt)
    )
  }

}