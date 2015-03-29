/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.test

import org.openqa.selenium.WebDriver
import org.specs2.execute.{ AsResult, Result }
import org.specs2.mutable.Around
import org.specs2.specification.Scope
import play.api.inject.guice.GuiceApplicationLoader
import play.api.{ Application, ApplicationLoader, Environment, Mode }
import play.core.server.{ NettyServer, ServerProvider }

// NOTE: Do *not* put any initialisation code in the below classes, otherwise delayedInit() gets invoked twice
// which means around() gets invoked twice and everything is not happy.  Only lazy vals and defs are allowed, no vals
// or any other code blocks.

/**
 * Used to run specs within the context of a running application loaded by the given `ApplicationLoader`.
 *
 * @param applicationLoader The application loader to use
 * @param context The context supplied to the application loader
 */
abstract class WithApplicationLoader(applicationLoader: ApplicationLoader = new GuiceApplicationLoader(), context: ApplicationLoader.Context = ApplicationLoader.createContext(new Environment(new java.io.File("."), ApplicationLoader.getClass.getClassLoader, Mode.Test))) extends Around with Scope {
  implicit lazy val app = applicationLoader.load(context)
  def around[T: AsResult](t: => T): Result = {
    Helpers.running(app)(AsResult.effectively(t))
  }
}

/**
 * Used to run specs within the context of a running application.
 *
 * @param app The fake application
 */
abstract class WithApplication(val app: Application = FakeApplication()) extends Around with Scope {
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
 * @param serverProvider *Experimental API; subject to change* The type of
 * server to use. Defaults to providing a Netty server.
 */
abstract class WithServer(
    val app: Application = FakeApplication(),
    val port: Int = Helpers.testServerPort,
    val serverProvider: Option[ServerProvider] = None) extends Around with Scope {
  implicit def implicitApp = app
  implicit def implicitPort: Port = port

  override def around[T: AsResult](t: => T): Result =
    Helpers.running(TestServer(
      port = port,
      application = app,
      serverProvider = serverProvider))(AsResult.effectively(t))
}

/**
 * Used to run specs within the context of a running server, and using a web browser
 *
 * @param webDriver The driver for the web browser to use
 * @param app The fake application
 * @param port The port to run the server on
 */
abstract class WithBrowser[WEBDRIVER <: WebDriver](
    val webDriver: WebDriver = WebDriverFactory(Helpers.HTMLUNIT),
    val app: Application = FakeApplication(),
    val port: Int = Helpers.testServerPort) extends Around with Scope {

  def this(
    webDriver: Class[WEBDRIVER],
    app: Application,
    port: Int) = this(WebDriverFactory(webDriver), app, port)

  implicit def implicitApp: Application = app
  implicit def implicitPort: Port = port

  lazy val browser: TestBrowser = TestBrowser(webDriver, Some("http://localhost:" + port))

  override def around[T: AsResult](t: => T): Result = {
    try {
      Helpers.running(TestServer(port, app))(AsResult.effectively(t))
    } finally {
      browser.quit()
    }
  }
}

