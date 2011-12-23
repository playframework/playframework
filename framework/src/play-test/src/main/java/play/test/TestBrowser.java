package play.test;

import play.*;

import java.util.*;

import org.openqa.selenium.*;

import fr.javafreelance.fluentlenium.core.*;

public class TestBrowser extends Fluent {
    
    public TestBrowser(Class<? extends WebDriver> webDriver) throws Exception {
        this(webDriver.newInstance());
    }
    
    public TestBrowser(WebDriver webDriver) {
        setDriver(webDriver);
    }
    
    public void goTo(String url) {
        if (url == null) {
            throw new IllegalArgumentException("Url is mandatory");
        }
        getDriver().get(url);
    }
    
    public String url() {
        return super.url();
    }
    
    public String pageSource() {
        return super.pageSource();
    }
    
    public Set<Cookie> getCookies() {
        return getDriver().manage().getCookies();
    }
    
    public Cookie getCookieNamed(String name) {
        return getDriver().manage().getCookieNamed(name);
    }
    
    public void quit() {
        getDriver().quit();
    }
    
}