/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import java.util.concurrent.TimeUnit

import scala.jdk.FunctionConverters._

import com.codeborne.selenide.Config
import com.codeborne.selenide.SelenideConfig
import org.openqa.selenium._
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.firefox._
import org.openqa.selenium.htmlunit._
import org.openqa.selenium.support.ui.FluentWait
import play.test.AbstractTestBrowser
import play.test.BrowserElements

/**
 * A test browser (Using Selenium WebDriver) backed by Selenide (https://selenide.org).
 *
 * @param webDriver The WebDriver instance to use.
 * @param baseUrl The base url to use for relative requests.
 * @see `play.test.AbstractTestBrowser`
 */
case class TestBrowser(webDriver: WebDriver, baseUrl: Option[String])
    extends AbstractTestBrowser(webDriver, baseUrl.orNull) {

  /**
   * Submits a form with the given field values
   *
   * @example {{{
   *   submit("#login", fields =
   *     "email" -> email,
   *     "password" -> password
   *   )
   * }}}
   */
  def submit(selector: String, fields: (String, String)*): BrowserElements = {
    fields.foreach {
      case (fieldName, fieldValue) =>
        $(s"$selector *[name=${TestBrowser.cssString(fieldName)}]").fill().`with`(fieldValue)
    }
    $(selector).submit()
  }

  /**
   * Repeatedly applies this instance's input value to the given block until one of the following occurs:
   * the function returns neither null nor false,
   * the function throws an unignored exception,
   * the timeout expires
   *
   * @param timeout the timeout amount
   * @param timeUnit timeout unit
   * @param block code to be executed
   */
  def waitUntil[T](timeout: Int, timeUnit: TimeUnit)(block: => T): T = {
    val wait = new FluentWait[WebDriver](webDriver).withTimeout(java.time.Duration.ofMillis(timeUnit.toMillis(timeout)))
    val f    = (driver: WebDriver) => block
    wait.until(f.asJava)
  }

  /**
   * Repeatedly applies this instance's input value to the given block until one of the following occurs:
   * the function returns neither null nor false,
   * the function throws an unignored exception,
   * the timeout expires
   *
   * @param timeout duration of how long should wait
   * @param block code to be executed
   */
  def waitUntil[T](timeout: java.time.Duration)(block: => T): T = {
    val wait = new FluentWait[WebDriver](webDriver).withTimeout(timeout)
    val f    = (driver: WebDriver) => block
    wait.until(f.asJava)
  }

  /**
   * Repeatedly applies this instance's input value to the given block until one of the following occurs:
   * the function returns neither null nor false,
   * the function throws an unignored exception,
   * the default timeout expires
   *
   * @param block code to be executed
   */
  def waitUntil[T](block: => T): T = waitUntil(3000, TimeUnit.MILLISECONDS)(block)

  /**
   * retrieves the underlying option interface that can be used
   * to set cookies, manage timeouts among other things
   */
  def manage: WebDriver.Options = webDriver.manage

  def quit(): Unit = quitBrowser()
}

/**
 * Helper utilities to build TestBrowsers
 */
object TestBrowser {

  // Quotes a value for use in a CSS attribute selector, like [name="value"]
  private def cssString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

  /**
   * Creates an in-memory WebBrowser (using HtmlUnit)
   *
   * @param baseUrl The default base URL that will be used for relative URLs
   */
  def default(baseUrl: Option[String] = None) = of(classOf[HtmlUnitDriver], baseUrl)

  /**
   * Creates a firefox WebBrowser.
   *
   * @param baseUrl The default base URL that will be used for relative URLs
   */
  def firefox(baseUrl: Option[String] = None) = of(classOf[FirefoxDriver], baseUrl)

  /**
   * Creates a WebBrowser of the specified class name.
   *
   * @param baseUrl The default base URL that will be used for relative URLs
   */
  def of[WEBDRIVER <: WebDriver](webDriver: Class[WEBDRIVER], baseUrl: Option[String] = None) =
    TestBrowser(WebDriverFactory(webDriver), baseUrl)
}

object WebDriverFactory {

  /**
   * Creates a Selenium Web Driver and configures it.
   *
   * Chrome, Edge and Firefox are started with WebDriver BiDi enabled, so Selenide can collect the browser's console
   * logs. They also use Selenide's `headless` and `browserBinary` settings, e.g. from the system properties
   * `selenide.headless=true` and `selenide.browserBinary=/path/to/browser`.
   *
   * @param clazz Type of driver to create
   * @return The driver instance
   */
  def apply[D <: WebDriver](clazz: Class[D]): WebDriver = {
    require(clazz != null, "The web driver class is null")
    // Reads the selenide.* system properties and a selenide.properties file on the classpath
    val config            = new SelenideConfig()
    val driver: WebDriver =
      if (clazz == classOf[ChromeDriver]) new ChromeDriver(chromeOptions(config))
      else if (clazz == classOf[EdgeDriver]) new EdgeDriver(edgeOptions(config))
      else if (clazz == classOf[FirefoxDriver]) new FirefoxDriver(firefoxOptions(config))
      else clazz.getDeclaredConstructor().newInstance()
    // Driver-specific configuration
    driver match {
      case htmlunit: HtmlUnitDriver => htmlunit.setJavascriptEnabled(true)
      case _                        =>
    }
    driver
  }

  private[test] def chromeOptions(config: Config): ChromeOptions = {
    val options = new ChromeOptions().enableBiDi()
    Option(config.browserBinary()).foreach(binary => options.setBinary(binary))
    if (config.headless()) options.addArguments("--headless=new") else options
  }

  private[test] def edgeOptions(config: Config): EdgeOptions = {
    val options = new EdgeOptions().enableBiDi()
    Option(config.browserBinary()).foreach(binary => options.setBinary(binary))
    if (config.headless()) options.addArguments("--headless=new") else options
  }

  private[test] def firefoxOptions(config: Config): FirefoxOptions = {
    val options = new FirefoxOptions().enableBiDi()
    Option(config.browserBinary()).foreach(binary => options.setBinary(binary))
    if (config.headless()) options.addArguments("-headless") else options
  }
}
