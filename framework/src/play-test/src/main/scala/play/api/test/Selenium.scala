package play.api.test

import java.io._

import play.api._

import org.openqa.selenium._
import org.openqa.selenium.firefox._
import org.openqa.selenium.htmlunit._

import org.fluentlenium.core._

import collection.JavaConverters._

/**
 * A test browser (Using Selenium WebDriver) with the FluentLenium API (https://github.com/Fluentlenium/FluentLenium).
 *
 * @param webDriver The WebDriver instance to use.
 */
case class TestBrowser(webDriver: WebDriver) extends FluentAdapter(webDriver) {


  /**
   * The current page URL.
   */
  override def url = super.url

  /**
   * The title of the current page.
   */
  override def title = super.title

  /**
   * The current page HTML source.
   */
  override def pageSource = super.pageSource

  /**
   * Retrieves all cookies.
   */
  override def getCookies(): java.util.Set[Cookie] = super.getCookies

  /**
   * Retrieves a cookie.
   */
  override def getCookie(name: String): Cookie = super.getCookie(name)



}

/**
 * Helper utilities to build TestBrowsers
 */
object TestBrowser {

  /**
   * Creates an in-memory WebBrowser (using HtmlUnit)
   */
  def default() = of(classOf[HtmlUnitDriver])

  /**
   * Creates a firefox WebBrowser.
   */
  def firefox() = of(classOf[FirefoxDriver])

  /**
   * Creates a WebBrowser of the specified class name.
   */
  def of[WEBDRIVER <: WebDriver](webDriver: Class[WEBDRIVER]) = TestBrowser(WebDriverFactory(webDriver))

}

object WebDriverFactory {
  /**
   * Creates a Selenium Web Driver and configures it
   * @param clazz Type of driver to create
   * @return The driver instance
   */
  def apply[D <: WebDriver](clazz: Class[D]): WebDriver = {
    val driver = clazz.newInstance
    // Driver-specific configuration
    driver match {
      case htmlunit: HtmlUnitDriver => htmlunit.setJavascriptEnabled(true)
      case _ =>
    }
    driver
  }
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
    play.core.Invoker.uninit()
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