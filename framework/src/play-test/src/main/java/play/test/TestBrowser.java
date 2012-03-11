package play.test;

import play.*;

import java.util.*;

import org.openqa.selenium.*;

import org.fluentlenium.core.*;

/**
 * A test browser (Using Selenium WebDriver) with the FluentLenium API (https://github.com/Fluentlenium/FluentLenium).
 */
public class TestBrowser extends FluentAdapter {
    
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

    /**
     * The current page URL.
     */
    public String url() {
        return super.url();
    }
    /**
    * The current page URL.
    */
    public String title() {
       return super.title();
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
        return super.getCookies();
    }

    /**
     * Retrieves a cookie.
     */
    public Cookie getCookie(String name) {
        return super.getCookie(name);
    }



}