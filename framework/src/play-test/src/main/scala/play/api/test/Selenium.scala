package play.api.test

import java.io._

import play.api._

import org.openqa.selenium._
import org.openqa.selenium.firefox._
import org.openqa.selenium.htmlunit._

import org.fluentlenium.core._

import collection.JavaConverters._
import java.util.concurrent.TimeUnit 
import com.google.common.base.Function
import org.openqa.selenium.support.ui.FluentWait
/**
 * A test browser (Using Selenium WebDriver) with the FluentLenium API (https://github.com/Fluentlenium/FluentLenium).
 *
 * @param webDriver The WebDriver instance to use.
 */
case class TestBrowser(webDriver: WebDriver) extends FluentAdapter(webDriver) {

   /**
   * Repeatedly applies this instance's input value to the given block until one of the following occurs: 
   * the function returns neither null nor false, 
   * the function throws an unignored exception, 
   * the timeout expires  
   *
   * @param timeout
   * @param timeunit duration
   * @param block code to be executed
   */
  def waitUntil[T](timeout: Int, timeUnit: TimeUnit)(block: => T): T = {
    val wait = new FluentWait[WebDriver](webDriver).withTimeout(timeout,timeUnit)
    val f = new Function[WebDriver, T]() {
     def apply(driver: WebDriver): T = {
       block
     }}
    wait.until(f)
  }

  /**
   * Repeatedly applies this instance's input value to the given block until one of the following occurs: 
   * the function returns neither null nor false, 
   * the function throws an unignored exception, 
   * the default timeout expires  
   *
   * @param block code to be executed
   */
  def waitUntil[T](block: => T): T =  waitUntil(3000,TimeUnit.MILLISECONDS)(block)
  
  /**
   * retrieves the underlying option interface that can be used
   * to set cookies, manage timeouts among other things
   */
   def manage: WebDriver.Options = super.getDriver.manage

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
    try {
      server = new play.core.server.NettyServer(new play.core.TestApplication(application), port, mode = Mode.Test)
     } catch {
        case t: Throwable => 
          t.printStackTrace
          throw new RuntimeException(t)
     } 
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
    play.api.libs.concurrent.Promise.resetSystem()
    play.api.libs.ws.WS.resetClient()
  }

}