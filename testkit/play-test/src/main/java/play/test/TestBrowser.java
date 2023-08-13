/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import io.fluentlenium.adapter.FluentAdapter;
import java.time.Duration;
import java.util.function.Function;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;

/**
 * A test browser (Using Selenium WebDriver) with the FluentLenium API
 * (https://github.com/Fluentlenium/FluentLenium).
 */
public class TestBrowser extends FluentAdapter {

  /**
   * A test browser (Using Selenium WebDriver) with the FluentLenium API
   * (https://github.com/Fluentlenium/FluentLenium).
   *
   * @param webDriver The WebDriver instance to use.
   * @param baseUrl The base url to use for relative requests.
   * @throws Exception if the webdriver cannot be created.
   */
  public TestBrowser(Class<? extends WebDriver> webDriver, String baseUrl) throws Exception {
    this(play.api.test.WebDriverFactory.apply(webDriver), baseUrl);
  }

  /**
   * A test browser (Using Selenium WebDriver) with the FluentLenium API
   * (https://github.com/Fluentlenium/FluentLenium).
   *
   * @param webDriver The WebDriver instance to use.
   * @param baseUrl The base url to use for relative requests.
   */
  public TestBrowser(WebDriver webDriver, String baseUrl) {
    super.initFluent(webDriver);
    super.getConfiguration().setBaseUrl(baseUrl);
  }

  /**
   * Creates a generic {@code FluentWait<WebDriver>} instance using the underlying web driver.
   *
   * @return the webdriver contained in a fluent wait.
   */
  public FluentWait<WebDriver> fluentWait() {
    return new FluentWait<>(super.getDriver());
  }

  /**
   * Repeatedly applies this instance's input value to the given function until one of the following
   * occurs: the function returns neither null nor false, the function throws an unignored
   * exception, the timeout expires
   *
   * <p>Useful in situations where FluentAdapter#await is too specific (for example to check against
   * page source)
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
   * useful in situations where FluentAdapter#await is too specific (for example to check against
   * page source or title)
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
    return super.getDriver().manage();
  }

  /** Quits and releases the {@link WebDriver} */
  public void quit() {
    if (getDriver() != null) {
      getDriver().quit();
    }
    releaseFluent();
  }
}
