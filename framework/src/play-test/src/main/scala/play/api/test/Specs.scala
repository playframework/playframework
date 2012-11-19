package play.api.test

import org.specs2.mutable.Around
import org.specs2.specification.Scope
import org.openqa.selenium.WebDriver
import org.specs2.execute.Result

// NOTE: Do *not* put any initialisation code in the below classes, otherwise delayedInit() gets invoked twice
// which means around() gets invoked twice and everything is not happy.  Only lazy vals and defs are allowed, no vals
// or any other code blocks.

/**
 * Used to run specs within the context of a running application.
 *
 * @param app The fake application
 */
abstract class WithApplication(val app: FakeApplication = FakeApplication()) extends Around with Scope {
  implicit def implicitApp = app
  def around[T](t: => T)(implicit evidence: (T) => Result) = {
    Helpers.running(app)(t)
  }
}

/**
 * Used to run specs within the context of a running server.
 *
 * @param app The fake application
 * @param port The port to run the server on
 */
abstract class WithServer(val app: FakeApplication = FakeApplication(),
                          val port: Int = Helpers.testServerPort,
                          val sslPort: Option[Int] = Helpers.testServerSSLPort) extends Around with Scope {
  implicit lazy val implicitApp = app
  def around[T](t: => T)(implicit evidence: (T) => Result) = Helpers.running(TestServer(port, app,sslPort))(t)
}

/**
 * Used to run specs within the context of a running server, and using a web browser
 *
 * @param webDriver The driver for the web browser to use
 * @param app The fake application
 * @param port The port to run the server on
 */
abstract class WithBrowser[WEBDRIVER <: WebDriver](
        val webDriver: Class[WEBDRIVER] = Helpers.HTMLUNIT,
        implicit val app: FakeApplication = FakeApplication(),
        val port: Int = Helpers.testServerPort,
        val sslPort: Option[Int] = Helpers.testServerSSLPort ) extends Around with Scope {
  implicit lazy val implicitApp = app

  lazy val browser: TestBrowser = TestBrowser.of(webDriver, Some("http://localhost:" + port))
  lazy val browserSecure: Option[TestBrowser] =
    sslPort.map(p=>TestBrowser.of(webDriver, Some("https://localhost:" + p)))

  def around[T](t: => T)(implicit evidence: (T) => Result) = {
    try {
      Helpers.running(TestServer(port, app,sslPort))(t)
    } finally {
      browser.quit()
    }
  }
}

