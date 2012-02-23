package play.test;

import play.*;

import java.util.*;

import org.openqa.selenium.*;

import fr.javafreelance.fluentlenium.core.*;

/**
 * A test browser (Using Selenium WebDriver) with the FluentLenium API (https://github.com/Fluentlenium/FluentLenium).
 */
public class TestBrowser extends Fluent {
    
    /**
     * A test browser (Using Selenium WebDriver) with the FluentLenium API (https://github.com/Fluentlenium/FluentLenium).
     *
     * @param webDriver The WebDriver instance to use.
     */
    public TestBrowser(Class<? extends WebDriver> webDriver) throws Exception {
        this(play.api.test.WebDriverFactory.apply(webDriver));
    }
    
    /**
     * A test browser (Using Selenium WebDriver) with the FluentLenium API (https://github.com/Fluentlenium/FluentLenium).
     *
     * @param webDriver The WebDriver instance to use.
     */
    public TestBrowser(WebDriver webDriver) {
        setDriver(webDriver);
    }
    
    /**
     * Open an URL.
     */
    public void goTo(String url) {
        if (url == null) {
            throw new IllegalArgumentException("Url is mandatory");
        }
        getDriver().get(url);
    }
    
    /**
     * The current page URL.
     */
    public String url() {
        return super.url();
    }
    
    /**
     * The current page HTML source.
     */
    public String pageSource() {
        return super.pageSource();
    }
    
    /**
     * Retrieves all cookies.
     */
    public Set<Cookie> getCookies() {
        return getDriver().manage().getCookies();
    }
    
    /**
     * Retrieves a cookie.
     */
    public Cookie getCookieNamed(String name) {
        return getDriver().manage().getCookieNamed(name);
    }
    
    /**
     * Quits the browser
     */
    public void quit() {
        getDriver().quit();
    }
    
}