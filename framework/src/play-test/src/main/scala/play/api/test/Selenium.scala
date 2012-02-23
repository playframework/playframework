package play.api.test

import java.io._

import play.api._

import org.openqa.selenium._
import org.openqa.selenium.firefox._
import org.openqa.selenium.htmlunit._

import fr.javafreelance.fluentlenium.core._

import collection.JavaConverters._

/**
 * A test browser (Using Selenium WebDriver) with the FluentLenium API (https://github.com/Fluentlenium/FluentLenium).
 *
 * @param webDriver The WebDriver instance to use.
 */
case class TestBrowser(webDriver: WebDriver) extends Fluent(webDriver) {

  /**
   * Open an URL.
   */
  def goTo(url: String) {
    getDriver.get(url)
  }

  /**
   * The current page URL.
   */
  override def url = super.url

  /**
   * The current page HTML source.
   */
  override def pageSource = super.pageSource

  /**
   * Retrieves all cookies.
   */
  def getCookies(): Set[Cookie] = getDriver().manage().getCookies().asScala.toSet

  /**
   * Retrieves a cookie.
   */
  def getCookieNamed(name: String): Cookie = getDriver().manage().getCookieNamed(name)

  /**
   * Quits the browser
   */
  def quit() {
    getDriver.quit()
  }

}

/**
 * Helper utilities to build TestBrowsers
 */
object TestBrowser {

  /**
   * Creates an in-memory WebBrowser (using HtmlUnit)
   */
  def default() = TestBrowser(SeleniumDrivers.htmlunit())

  /**
   * Creates a firefox WebBrowser.
   */
  def firefox() = TestBrowser(SeleniumDrivers.firefox())

}

/**
 * Helper utilities to create usual Selenium WebDriver
 */
object SeleniumDrivers {
  
  /**
   * Creates an HtmlUnitDriver (enables JavaScript support)
   */
  def htmlunit() = {
    val htmlunit = new HtmlUnitDriver
    htmlunit.setJavascriptEnabled(true)
    htmlunit
  }
  
  /**
   * Creates a FirefoxDriver
   */
  def firefox() = new FirefoxDriver
}

/**
 * A test Netty web server.
 *
 * @param port HTTP port to bind on.
 * @param application The FakeApplication to load in this server.
 */
case class TestServer(port: Int, application: FakeApplication = FakeApplication()) {

  private var server: play.core.server.NettyServer = _

  /**
   * Starts this server.
   */
  def start() {
    if (server != null) {
      sys.error("Server already started!")
    }
    server = new play.core.server.NettyServer(new play.core.TestApplication(application), port, mode = Mode.Test)
  }

  /**
   * Stops this server.
   */
  def stop() {
    if (server == null) {
      sys.error("Server is not started!");
    }
    server.stop()
    server = null
  }

}