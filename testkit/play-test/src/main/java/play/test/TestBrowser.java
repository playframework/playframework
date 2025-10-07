/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import com.codeborne.selenide.SelenideConfig;
import com.codeborne.selenide.SelenideDriver;
import com.codeborne.selenide.SelenideElement;
import java.time.Duration;
import java.util.function.Function;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;

/**
 * A test browser (Using Selenium WebDriver) with the Selenide API
 * (https://github.com/selenide/selenide).
 */
public class TestBrowser {

  private SelenideDriver driver;

  /**
   * A test browser (Using Selenium WebDriver) with the Selenide API
   * (https://github.com/selenide/selenide).
   *
   * @param webDriver The WebDriver instance to use.
   * @param baseUrl The base url to use for relative requests.
   * @throws Exception if the webdriver cannot be created.
   */
  public TestBrowser(Class<? extends WebDriver> webDriver, String baseUrl) throws Exception {
    this(play.api.test.WebDriverFactory.apply(webDriver), baseUrl);
  }

  /**
   * A test browser (Using Selenium WebDriver) with the Selenide API
   * (https://github.com/selenide/selenide).
   *
   * @param webDriver The WebDriver instance to use.
   * @param baseUrl The base url to use for relative requests.
   */
  public TestBrowser(WebDriver webDriver, String baseUrl) {
    SelenideConfig config = new SelenideConfig();
    config.baseUrl(baseUrl);
    driver = new SelenideDriver(config, webDriver, null);
  }

  public void open(String relativeOrAbsoluteUrl) {
    driver.open(relativeOrAbsoluteUrl);
  }

  public void goTo(String relativeOrAbsoluteUrl) {
    open(relativeOrAbsoluteUrl);
  }

  public String source() {
    return driver.source();
  }

  public String pageSource() {
    return source();
  }

  public SelenideElement el(String cssSelector) {
    return driver.find(cssSelector);
  }

  public SelenideElement $(String cssSelector) {
    return el(cssSelector);
  }

  public String url() {
    // return the relative url
    return driver.url().substring(driver.config().baseUrl().length() + 1);
  }

  /**
   * Creates a generic {@code FluentWait<WebDriver>} instance using the underlying web driver.
   *
   * @return the webdriver contained in a fluent wait.
   */
  public FluentWait<WebDriver> fluentWait() {
    return new FluentWait<>(driver.getWebDriver());
  }

  /**
   * Repeatedly applies this instance's input value to the given function until one of the following
   * occurs: the function returns neither null nor false, the function throws an unignored
   * exception, the timeout expires
   *
   * @param <T> the return type
   * @param wait generic {@code FluentWait<WebDriver>} instance
   * @param f function to execute
   * @return the return value
   */
  public <T> T waitUntil(FluentWait<WebDriver> wait, Function<WebDriver, T> f) {
    return wait.until(f);
  }

  /**
   * Repeatedly applies this instance's input value to the given function until one of the following
   * occurs:
   *
   * <ul>
   *   <li>the function returns neither null nor false,
   *   <li>the function throws an unignored exception,
   *   <li>the default timeout expires
   * </ul>
   *
   * @param f function to execute
   * @param <T> the return type
   * @return the return value.
   */
  public <T> T waitUntil(Function<WebDriver, T> f) {
    FluentWait<WebDriver> wait = fluentWait().withTimeout(Duration.ofMillis(3000));
    return waitUntil(wait, f);
  }

  /**
   * Retrieves the underlying option interface that can be used to set cookies, manage timeouts
   * among other things.
   *
   * @return the web driver options.
   */
  public WebDriver.Options manage() {
    return driver.getWebDriver().manage();
  }

  /** Quits and releases the {@link WebDriver} */
  void quit() {
    // TODO siehe dprecation comment in WebDriverRunner.closeWebDriver
    final WebDriver webDriver = driver.getWebDriver();
    if (webDriver != null) {
      webDriver.quit();
    }
    // releaseFluent();
  }
}
