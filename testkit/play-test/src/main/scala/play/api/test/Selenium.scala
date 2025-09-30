/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import java.util.concurrent.TimeUnit

import scala.jdk.FunctionConverters._

import com.codeborne.selenide.SelenideConfig
import com.codeborne.selenide.SelenideDriver
import com.codeborne.selenide.SelenideElement
import org.openqa.selenium._
import org.openqa.selenium.firefox._
import org.openqa.selenium.htmlunit._
import org.openqa.selenium.support.ui.FluentWait

/**
 * A test browser (Using Selenium WebDriver) with the Selenide API (https://github.com/selenide/selenide).
 *
 * @param webDriver The WebDriver instance to use.
 */
case class TestBrowser(webDriver: WebDriver, baseUrl: Option[String]) {
  private val config = new SelenideConfig()
  baseUrl.foreach(baseUrl => config.baseUrl(baseUrl))
  private val driver = new SelenideDriver(config, webDriver, null)

  def open(relativeOrAbsoluteUrl: String): Unit = {
    driver.open(relativeOrAbsoluteUrl)
  }

  def goTo(relativeOrAbsoluteUrl: String): Unit = {
    open(relativeOrAbsoluteUrl)
  }

  def source: String = driver.source

  def pageSource: String = source

  def el(cssSelector: String): SelenideElement = driver.find(cssSelector)

  def $(cssSelector: String): SelenideElement = el(cssSelector)

  def url: String = {
    // return the relative url
    driver.url().substring(driver.config().baseUrl().length + 1)
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
  def manage: WebDriver.Options = driver.getWebDriver().manage

  def quit(): Unit = {
    Option(driver.getWebDriver()).foreach(_.quit())
    // releaseFluent()
  }
}

/**
 * Helper utilities to build TestBrowsers
 */
object TestBrowser {

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
   * Creates a Selenium Web Driver and configures it
   * @param clazz Type of driver to create
   * @return The driver instance
   */
  def apply[D <: WebDriver](clazz: Class[D]): WebDriver = {
    val driver = clazz.getDeclaredConstructor().newInstance()
    // Driver-specific configuration
    driver match {
      case htmlunit: HtmlUnitDriver => htmlunit.setJavascriptEnabled(true)
      case _                        =>
    }
    driver
  }
}
