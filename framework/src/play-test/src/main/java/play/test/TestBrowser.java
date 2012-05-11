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
        super(webDriver);
    }
    

}