/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import java.util.concurrent.TimeUnit

import org.fluentlenium.adapter.FluentAdapter
import org.fluentlenium.core.domain.{ FluentList, FluentWebElement }
import org.openqa.selenium._
import org.openqa.selenium.firefox._
import org.openqa.selenium.htmlunit._
import org.openqa.selenium.support.ui.FluentWait

import scala.compat.java8.FunctionConverters._

/**
 * A test browser (Using Selenium WebDriver) with the FluentLenium API (https://github.com/Fluentlenium/FluentLenium).
 *
 * @param webDriver The WebDriver instance to use.
 */
case class TestBrowser(webDriver: WebDriver, baseUrl: Option[String]) extends FluentAdapter() {
  super.initFluent(webDriver)
  baseUrl.foreach(baseUrl => super.getConfiguration.setBaseUrl(baseUrl))

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
  def submit(selector: String, fields: (String, String)*): FluentList[FluentWebElement] = {
    fields.foreach {
      case (fieldName, fieldValue) =>
        $(s"$selector *[name=$fieldName]").fill.`with`(fieldValue)
    }
    $(selector).submit()
  }

  /**
   * Repeatedly applies this instance's input value to the given block until one of the following occurs:
   * the function returns neither null nor false,
   * the function throws an unignored exception,
   * the timeout expires
   *
   * @param timeout
   * @param timeUnit duration
   * @param block code to be executed
   */
  def waitUntil[T](timeout: Int, timeUnit: TimeUnit)(block: => T): T = {
    val wait = new FluentWait[WebDriver](webDriver).withTimeout(timeout, timeUnit)
    val f = (driver: WebDriver) => block
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
  def manage: WebDriver.Options = super.getDriver.manage

  def quit(): Unit = {
    Option(super.getDriver).foreach(_.quit())
    releaseFluent()
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
  def of[WEBDRIVER <: WebDriver](webDriver: Class[WEBDRIVER], baseUrl: Option[String] = None) = TestBrowser(WebDriverFactory(webDriver), baseUrl)

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
      case _ =>
    }
    driver
  }
}
