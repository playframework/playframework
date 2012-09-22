package play.test;

import org.junit.After;
import org.junit.Before;

/**
 * Provides a server and browser to JUnit tests
 */
public class WithBrowser extends WithServer {
    protected TestBrowser browser;

    /**
     * Override this if you want to use a different browser
     *
     * @return a new test browser
     */
    protected TestBrowser provideBrowser() {
        return Helpers.testBrowser();
    }

    @Before
    public void startBrowser() {
        browser = provideBrowser();
    }

    @After
    public void quitBrowser() {
        browser.quit();
        browser = null;
    }
}
