package play.api.test

import org.specs2.mutable.Around
import org.specs2.specification.Scope
import org.openqa.selenium.WebDriver
import org.specs2.execute.{ AsResult, Result }

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
  override def around[T: AsResult](t: => T): Result = {
    Helpers.running(app)(AsResult.effectively(t))
  }
}

/**
 * Used to run specs within the context of a running server.
 *
 * @param app The fake application
 * @param port The port to run the server on
 */
abstract class WithServer(val app: FakeApplication = FakeApplication(),
    val port: Int = Helpers.testServerPort) extends Around with Scope {
  implicit def implicitApp = app
  implicit def implicitPort: Port = port

  override def around[T: AsResult](t: => T): Result = Helpers.running(TestServer(port, app))(AsResult.effectively(t))
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
    val app: FakeApplication = FakeApplication(),
    val port: Int = Helpers.testServerPort) extends Around with Scope {

  implicit def implicitApp = app
  implicit def implicitPort: Port = port

  lazy val browser: TestBrowser = TestBrowser.of(webDriver, Some("http://localhost:" + port))

  override def around[T: AsResult](t: => T): Result = {
    try {
      Helpers.running(TestServer(port, app))(AsResult.effectively(t))
    } finally {
      browser.quit()
    }
  }
}

