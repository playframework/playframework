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
    protected TestBrowser provideBrowser(int port) {
        return Helpers.testBrowser(port);
    }

    @Override
    protected void start(FakeApplication fakeApplication, int port) {
        super.start(fakeApplication, port);
        browser = provideBrowser(port);
    }

    @After
    public void quitBrowser() {
        if (browser != null) {
            browser.quit();
            browser = null;
        }
    }
}
