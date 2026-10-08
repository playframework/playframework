/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import com.codeborne.selenide.SelenideConfig;
import com.codeborne.selenide.SelenideDriver;
import com.codeborne.selenide.SelenideElement;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

/**
 * Base class of Play's test browsers ({@link play.test.TestBrowser} and {@code
 * play.api.test.TestBrowser}).
 *
 * <p>Wraps a Selenium {@link WebDriver} in a {@link SelenideDriver} instance and provides the
 * browser methods Play's test browser offered when it was based on FluentLenium. Use {@link
 * #selenide()} to access the complete Selenide API.
 *
 * <p>The Selenide instance is created without Selenide's static API, so neither the browser nor its
 * configuration is kept in {@link ThreadLocal}s, and the browser's own screenshot laboratory
 * doesn't use them either.
 */
public abstract class AbstractTestBrowser {

  // Selenide's defaults, which are relative to the working directory and Gradle-style
  private static final String SELENIDE_DEFAULT_REPORTS_FOLDER = "build/reports/tests";
  private static final String SELENIDE_DEFAULT_DOWNLOADS_FOLDER = "build/downloads";

  private final WebDriver webDriver;
  private final SelenideConfig config;
  private final SelenideDriver selenide;

  /**
   * @param webDriver The WebDriver instance to use.
   * @param baseUrl The base url to use for relative requests, may be {@code null}.
   */
  protected AbstractTestBrowser(WebDriver webDriver, String baseUrl) {
    this.webDriver = Objects.requireNonNull(webDriver, "webDriver must not be null");
    // The Selenide configuration holds the base url, also for Play's own methods. Without a base
    // url,
    // it is empty, so relative urls don't silently resolve against Selenide's default base url.
    this.config =
        new SelenideConfig()
            .browser(browserName(webDriver))
            .baseUrl(baseUrl == null ? "" : baseUrl);
    // Keep reports and downloads in sbt's target folder, unless configured via selenide.*
    // system properties or a selenide.properties file on the classpath
    if (SELENIDE_DEFAULT_REPORTS_FOLDER.equals(config.reportsFolder())) {
      config.reportsFolder("target/selenide/reports");
    }
    if (SELENIDE_DEFAULT_DOWNLOADS_FOLDER.equals(config.downloadsFolder())) {
      config.downloadsFolder("target/selenide/downloads");
    }
    this.selenide =
        new SelenideDriver(config, webDriver, null, new NoThreadLocalScreenShotLaboratory());
  }

  private static String browserName(WebDriver webDriver) {
    if (webDriver instanceof HasCapabilities hasCapabilities) {
      String name = hasCapabilities.getCapabilities().getBrowserName();
      if (name != null && !name.isEmpty()) {
        return name;
      }
    }
    return webDriver.getClass().getSimpleName();
  }

  /**
   * The Selenide driver of this browser, to use the complete Selenide API.
   *
   * @return the Selenide driver.
   */
  public SelenideDriver selenide() {
    return selenide;
  }

  /**
   * The Selenide configuration of this browser. Changes, like timeouts or the base url, apply to
   * this browser only. Settings used to start a browser, like {@code headless} or {@code
   * browserBinary}, have no effect, because the browser is already running.
   *
   * @return the Selenide configuration.
   */
  public SelenideConfig selenideConfig() {
    return config;
  }

  /**
   * The underlying Selenium web driver.
   *
   * @return the web driver.
   */
  public WebDriver getDriver() {
    return webDriver;
  }

  /**
   * The base url used for relative requests.
   *
   * @return the base url, or {@code null} if none is set.
   */
  public String getBaseUrl() {
    String baseUrl = config.baseUrl();
    return baseUrl == null || baseUrl.isEmpty() ? null : baseUrl;
  }

  /* ---------------------------- Navigation ---------------------------- */

  /**
   * Opens the given url. A relative url is resolved against the base url.
   *
   * @param url the url, relative or absolute.
   * @throws IllegalArgumentException if the url is relative and there is no base url.
   */
  public void goTo(String url) {
    Objects.requireNonNull(url, "It is required to specify a URL to navigate to.");
    String absoluteUrl = buildUrl(url);
    if (!URI.create(absoluteUrl).isAbsolute()) {
      throw new IllegalArgumentException(
          "Can not open relative url '" + url + "' because this browser has no base url");
    }
    selenide.open(absoluteUrl);
  }

  /**
   * The current url. If it starts with the base url, the url relative to the base url is returned
   * (e.g. {@code "login"} for {@code "http://localhost:19001/login"}).
   *
   * @return the current url.
   */
  public String url() {
    String currentUrl = webDriver.getCurrentUrl();
    String base = buildUrl(null);
    if (currentUrl != null && base != null && currentUrl.startsWith(base)) {
      return currentUrl.substring(base.length());
    }
    return currentUrl;
  }

  /**
   * The source of the current page.
   *
   * @return the page source.
   */
  public String pageSource() {
    return webDriver.getPageSource();
  }

  /* ---------------------------- Elements ---------------------------- */

  /**
   * The first element matching the given CSS selector. Like all Selenide elements, the element is
   * looked up lazily and actions on it wait until the element is ready (see {@link
   * SelenideConfig#timeout()}).
   *
   * @param cssSelector the CSS selector.
   * @return the element.
   */
  public SelenideElement el(String cssSelector) {
    return selenide.$(cssSelector);
  }

  /**
   * The first element matching the given locator.
   *
   * @param locator the locator.
   * @return the element.
   * @see #el(String)
   */
  public SelenideElement el(By locator) {
    return selenide.$(locator);
  }

  /**
   * All elements matching the given CSS selector. Unlike Selenide's {@code $}, this returns a
   * collection, like Play's test browser always did. Use {@link #el(String)} for a single element.
   * Besides the Selenide collection API, the result supports clicking, filling and submitting all
   * of its elements, see {@link BrowserElements}.
   *
   * @param cssSelector the CSS selector.
   * @return the elements.
   */
  public BrowserElements $(String cssSelector) {
    return new BrowserElements(selenide.driver(), cssSelector);
  }

  /**
   * All elements matching the given locator.
   *
   * @param locator the locator.
   * @return the elements.
   * @see #$(String)
   */
  public BrowserElements $(By locator) {
    return new BrowserElements(selenide.driver(), locator);
  }

  /**
   * All elements matching the given CSS selector. Alias for {@link #$(String)}.
   *
   * @param cssSelector the CSS selector.
   * @return the elements.
   */
  public BrowserElements find(String cssSelector) {
    return $(cssSelector);
  }

  /**
   * All elements matching the given locator. Alias for {@link #$(By)}.
   *
   * @param locator the locator.
   * @return the elements.
   */
  public BrowserElements find(By locator) {
    return $(locator);
  }

  /* ---------------------------- Scripts ---------------------------- */

  /**
   * Executes JavaScript in the current page.
   *
   * @param script the script.
   * @param args the script arguments.
   * @return the result of the script, see {@link JavascriptExecutor#executeScript(String,
   *     Object...)}.
   */
  public Object executeScript(String script, Object... args) {
    return ((JavascriptExecutor) webDriver).executeScript(script, args);
  }

  /* ---------------------------- Cookies ---------------------------- */

  /**
   * The cookies of the current domain.
   *
   * @return the cookies.
   */
  public Set<Cookie> getCookies() {
    return webDriver.manage().getCookies();
  }

  /**
   * The cookie with the given name.
   *
   * @param name the cookie name.
   * @return the cookie, or {@code null} if there is no such cookie.
   */
  public Cookie getCookie(String name) {
    return webDriver.manage().getCookieNamed(name);
  }

  /* ---------------------------- Screenshots ---------------------------- */

  /**
   * Takes a screenshot of the current page and saves it in the working directory, named after the
   * current timestamp.
   *
   * @return the screenshot file.
   */
  public File takeScreenshot() {
    return takeScreenshot(System.currentTimeMillis() + ".png");
  }

  /**
   * Takes a screenshot of the current page and saves it to the given file.
   *
   * @param fileName the file name, relative to the working directory or absolute.
   * @return the screenshot file.
   * @throws WebDriverException if the browser can not take screenshots (like HtmlUnit).
   */
  public File takeScreenshot(String fileName) {
    if (!(webDriver instanceof TakesScreenshot takesScreenshot)) {
      throw new WebDriverException("Current browser doesn't allow taking screenshot.");
    }
    return writeFile(fileName, takesScreenshot.getScreenshotAs(OutputType.BYTES));
  }

  /**
   * Saves the source of the current page in the working directory, named after the current
   * timestamp.
   *
   * @return the HTML file.
   */
  public File takeHtmlDump() {
    return takeHtmlDump(System.currentTimeMillis() + ".html");
  }

  /**
   * Saves the source of the current page to the given file.
   *
   * @param fileName the file name, relative to the working directory or absolute.
   * @return the HTML file.
   */
  public File takeHtmlDump(String fileName) {
    String source = pageSource();
    return writeFile(fileName, (source == null ? "" : source).getBytes(StandardCharsets.UTF_8));
  }

  /* ---------------------------- Lifecycle ---------------------------- */

  /** Quits the web driver. */
  protected void quitBrowser() {
    selenide.close();
  }

  /* ---------------------------- Helpers ---------------------------- */

  private static File writeFile(String fileName, byte[] content) {
    File destination = new File(fileName);
    try {
      File parent = destination.getAbsoluteFile().getParentFile();
      if (parent != null) {
        Files.createDirectories(parent.toPath());
      }
      Files.write(destination.toPath(), content);
    } catch (IOException e) {
      throw new UncheckedIOException("Error when writing " + destination, e);
    }
    return destination;
  }

  // Resolves urls like FluentLenium did: the base url always ends with a "/" and a leading "/" of
  // a relative url is removed, so both "login" and "/login" resolve to "<baseUrl>/login".
  private String buildUrl(String url) {
    URI base = sanitizedBaseUrl();
    if (base != null && url != null && url.startsWith("/")) {
      url = url.substring(1);
    }
    URI uri = url == null ? null : URI.create(url);
    if (base != null) {
      return uri == null ? base.toString() : base.resolve(uri).toString();
    }
    return uri == null ? null : uri.toString();
  }

  // Adds a missing scheme to the base url and uses the scheme of the current url if it points to
  // the same host (e.g. after a redirect from http to https).
  private URI sanitizedBaseUrl() {
    String baseUrl = getBaseUrl();
    if (baseUrl == null) {
      return null;
    }
    String spec = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    URI base = URI.create(spec);
    if (base.getScheme() == null) {
      base = URI.create("http://" + spec.replaceFirst("^/+", ""));
    }
    String currentUrl = webDriver.getCurrentUrl();
    if (currentUrl != null) {
      try {
        URI current = URI.create(currentUrl);
        String scheme = current.getScheme();
        if (Objects.equals(base.getAuthority(), current.getAuthority())
            && ("http".equals(scheme) || "https".equals(scheme))
            && !scheme.equals(base.getScheme())) {
          base = URI.create(scheme + base.toString().substring(base.getScheme().length()));
        }
      } catch (IllegalArgumentException ignored) {
        // current url is not a valid URI (e.g. "about:blank" is valid, but a data url may not be)
      }
    }
    return base;
  }
}
